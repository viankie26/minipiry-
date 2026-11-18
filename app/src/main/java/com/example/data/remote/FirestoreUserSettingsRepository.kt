package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.domain.logic.ReminderSettings
import com.example.domain.model.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Reads, writes, and deletes `/user_settings/{uid}` in Cloud Firestore,
 * strictly conforming to `firebase-blueprint.json` and `firestore.rules`.
 */
class FirestoreUserSettingsRepository(
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
      ?: throw IllegalStateException("User must be signed in before accessing user_settings.")
    check(currentUid == expectedUserId) {
      "Authenticated session changed; refusing cross-user settings operation."
    }
    return currentUid
  }

  suspend fun loadSettings(userId: String): Result<ReminderSettings?> {
    val docPath = "$COLLECTION_USER_SETTINGS/$userId"
    return try {
      val uid = requireVerifiedUserId(userId)
      val snap = db.collection(COLLECTION_USER_SETTINGS).document(uid).get().await()
      if (!snap.exists()) {
        return Result.success(null)
      }
      val enabled = snap.getBoolean("remindersEnabled") ?: true
      val hour = (snap.getLong("reminderHour")?.toInt() ?: 9).coerceIn(0, 23)
      val minute = (snap.getLong("reminderMinute")?.toInt() ?: 0).coerceIn(0, 59)
      val daysBefore = (snap.getLong("daysBefore")?.toInt() ?: 3).coerceIn(0, 30)
      Result.success(
        ReminderSettings(
          enabled = enabled,
          daysBefore = daysBefore,
          remindHour = hour,
          remindMinute = minute,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docPath)
      Result.failure(e)
    }
  }

  suspend fun saveSettings(
    userId: String,
    settings: ReminderSettings,
    language: AppLanguage,
  ): Result<Unit> {
    val docPath = "$COLLECTION_USER_SETTINGS/$userId"
    return try {
      val uid = requireVerifiedUserId(userId)
      val docRef = db.collection(COLLECTION_USER_SETTINGS).document(uid)
      val existing = docRef.get().await()

      if (!existing.exists()) {
        val createPayload = mapOf(
          "ownerId" to uid,
          "remindersEnabled" to settings.enabled,
          "reminderHour" to settings.remindHour.coerceIn(0, 23),
          "reminderMinute" to settings.remindMinute.coerceIn(0, 59),
          "daysBefore" to settings.daysBefore.coerceIn(0, 30),
          "language" to language.code,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(createPayload).await()
      } else {
        val updatePayload = mapOf(
          "remindersEnabled" to settings.enabled,
          "reminderHour" to settings.remindHour.coerceIn(0, 23),
          "reminderMinute" to settings.remindMinute.coerceIn(0, 59),
          "daysBefore" to settings.daysBefore.coerceIn(0, 30),
          "language" to language.code,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updatePayload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteSettings(userId: String): Result<Unit> {
    val docPath = "$COLLECTION_USER_SETTINGS/$userId"
    return try {
      val uid = requireVerifiedUserId(userId)
      val docRef = db.collection(COLLECTION_USER_SETTINGS).document(uid)
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.delete().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  companion object {
    const val COLLECTION_USER_SETTINGS = "user_settings"
  }
}
