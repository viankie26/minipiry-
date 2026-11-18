package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.domain.logic.ExpiryInputLogic
import com.example.domain.logic.ItemFormValidator
import com.example.domain.model.AppLanguage
import com.example.domain.model.Category
import com.example.domain.model.Item
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreItemsRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireVerifiedUserId(expectedUserId: String): String {
    val currentUid = auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    check(currentUid == expectedUserId) {
      "Authenticated session changed; refusing cross-user Firestore operation."
    }
    return currentUid
  }

  /**
   * Real-time stream of all items owned by `userId`, ordered by `created_at` descending.
   * Preserves that exact descending order for `ItemSelectors.selectHomeItems`.
   */
  fun observeItems(userId: String): Flow<List<Item>> = flow {
    require(userId.isNotBlank()) { "userId must not be blank" }
    val path = COLLECTION_ITEMS
    emitAll(
      db.collection(COLLECTION_ITEMS)
        .whereEqualTo("ownerId", userId)
        .snapshots()
        .map { querySnapshot ->
          querySnapshot.documents
            .mapNotNull { ItemsMapper.fromDocument(it) }
            .sortedByDescending { it.createdAt }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun refreshItems(userId: String): Result<List<Item>> {
    val path = COLLECTION_ITEMS
    return try {
      val uid = requireVerifiedUserId(userId)
      val snapshot = db.collection(COLLECTION_ITEMS)
        .whereEqualTo("ownerId", uid)
        .get()
        .await()
      val items = snapshot.documents
        .mapNotNull { ItemsMapper.fromDocument(it) }
        .sortedByDescending { it.createdAt }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun addItem(
    userId: String,
    name: String,
    category: Category,
    expiryDate: String,
    note: String?,
  ): Result<String> {
    var docPath = COLLECTION_ITEMS
    return try {
      val uid = requireVerifiedUserId(userId)
      val validation = ItemFormValidator.validateItemForm(name, category, expiryDate, note)
      val draft = validation.draft
        ?: return Result.failure(IllegalArgumentException("Invalid item form payload"))

      val docRef = db.collection(COLLECTION_ITEMS).document()
      docPath = docRef.path
      val payload = ItemsMapper.toCreatePayload(uid, draft)
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun setUsedUp(
    userId: String,
    itemId: String,
    usedUp: Boolean,
  ): Result<Unit> {
    var docPath = "$COLLECTION_ITEMS/$itemId"
    return try {
      requireVerifiedUserId(userId)
      require(itemId.isNotBlank()) { "itemId must not be blank" }
      val docRef = db.collection(COLLECTION_ITEMS).document(itemId)
      docPath = docRef.path
      docRef.update(ItemsMapper.toToggleUsedUpPayload(usedUp)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateItem(
    userId: String,
    itemId: String,
    name: String,
    category: Category,
    expiryDate: String,
    note: String?,
    usedUp: Boolean,
  ): Result<Unit> {
    var docPath = "$COLLECTION_ITEMS/$itemId"
    return try {
      requireVerifiedUserId(userId)
      require(itemId.isNotBlank()) { "itemId must not be blank" }
      val validation = ItemFormValidator.validateItemForm(name, category, expiryDate, note)
      val draft = validation.draft
        ?: return Result.failure(IllegalArgumentException("Invalid item form payload"))

      val docRef = db.collection(COLLECTION_ITEMS).document(itemId)
      docPath = docRef.path
      docRef.update(ItemsMapper.toUpdatePayload(draft, usedUp)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteItem(
    userId: String,
    itemId: String,
  ): Result<Unit> {
    var docPath = "$COLLECTION_ITEMS/$itemId"
    return try {
      requireVerifiedUserId(userId)
      require(itemId.isNotBlank()) { "itemId must not be blank" }
      val docRef = db.collection(COLLECTION_ITEMS).document(itemId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteAllItemsForUser(userId: String): Result<Unit> {
    val path = COLLECTION_ITEMS
    return try {
      val uid = requireVerifiedUserId(userId)
      val snapshot = db.collection(COLLECTION_ITEMS)
        .whereEqualTo("ownerId", uid)
        .get()
        .await()
      snapshot.documents.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { doc -> batch.delete(doc.reference) }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  /**
   * Inserts 4 realistic items (expired / expiring today / expiring in 4 days / fresh)
   * into Cloud Firestore so the user can immediately verify all Home screen urgency states,
   * statistics, tabs, filters, and persistence.
   */
  suspend fun seedPreviewItems(
    userId: String,
    language: AppLanguage,
  ): Result<Unit> {
    var docPath = COLLECTION_ITEMS
    return try {
      val uid = requireVerifiedUserId(userId)
      val cal = Calendar.getInstance()
      val y = cal.get(Calendar.YEAR)
      val m = cal.get(Calendar.MONTH) + 1
      val d = cal.get(Calendar.DAY_OF_MONTH)

      val offsets = listOf(-1, 2, 20, 90, 5, 365)
      val categories = listOf(
        Category.FOOD,
        Category.FOOD,
        Category.MEDICINE,
        Category.DAILY,
        Category.FOOD,
        Category.OTHER,
      )
      val names = com.example.domain.i18n.I18nDict.of(language).sampleNames

      val batch = db.batch()
      names.forEachIndexed { index, sampleName ->
        val isoDate = ExpiryInputLogic.addDaysYmd(y, m, d, offsets.getOrElse(index) { 30 })
        val cat = categories.getOrElse(index) { Category.OTHER }
        val validation = ItemFormValidator.validateItemForm(
          name = sampleName,
          category = cat,
          expiryDate = isoDate,
          note = null,
        )
        val draft = checkNotNull(validation.draft)
        val docRef = db.collection(COLLECTION_ITEMS).document()
        docPath = docRef.path
        batch.set(docRef, ItemsMapper.toCreatePayload(uid, draft))
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  companion object {
    const val COLLECTION_ITEMS = "items"
  }
}
