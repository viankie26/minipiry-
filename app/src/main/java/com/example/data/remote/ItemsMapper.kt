package com.example.data.remote

import com.example.domain.logic.ValidatedItemDraft
import com.example.domain.model.Category
import com.example.domain.model.Item
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

/**
 * Maps between Firestore `items` document fields (`expiry_date`, `used_up`, `created_at`, `ownerId`)
 * and pure Kotlin domain `Item` (`expiryDate`, `usedUp`, `createdAt`), preserving exact parity
 * with original `items-api.ts` and `firestore.rules`.
 */
object ItemsMapper {

  fun fromDocument(snapshot: DocumentSnapshot): Item? {
    if (!snapshot.exists()) return null
    val id = snapshot.id.takeIf { it.isNotBlank() } ?: return null
    val name = snapshot.getString("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val expiryDate = snapshot.getString("expiry_date")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val category = Category.fromWire(snapshot.getString("category"))
    val note = snapshot.getString("note")?.trim()?.takeIf { it.isNotEmpty() }
    val usedUp = snapshot.getBoolean("used_up") ?: false

    val serverTimestamp = snapshot.getTimestamp(
      "created_at",
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    )
    val createdAtMillis = serverTimestamp?.toDate()?.time
      ?: ((snapshot.data?.get("created_at") as? Number)?.toLong() ?: 0L)

    return Item(
      id = id,
      name = name,
      category = category,
      expiryDate = expiryDate,
      createdAt = createdAtMillis,
      note = note,
      usedUp = usedUp,
    )
  }

  @Suppress("UNCHECKED_CAST")
  fun toCreatePayload(ownerId: String, draft: ValidatedItemDraft): Map<String, Any> {
    return mapOf<String, Any?>(
      "ownerId" to ownerId,
      "name" to draft.name,
      "category" to draft.category.wireValue,
      "expiry_date" to draft.expiryDate,
      "note" to draft.note,
      "used_up" to false,
      "created_at" to FieldValue.serverTimestamp(),
      "updated_at" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
  }

  fun toUpdatePayload(draft: ValidatedItemDraft, usedUp: Boolean): Map<String, Any> {
    return mapOf(
      "name" to draft.name,
      "category" to draft.category.wireValue,
      "expiry_date" to draft.expiryDate,
      "note" to (draft.note ?: FieldValue.delete()),
      "used_up" to usedUp,
      "updated_at" to FieldValue.serverTimestamp(),
    )
  }

  fun toToggleUsedUpPayload(usedUp: Boolean): Map<String, Any> {
    return mapOf(
      "used_up" to usedUp,
      "updated_at" to FieldValue.serverTimestamp(),
    )
  }
}
