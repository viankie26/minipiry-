package com.example.domain.logic

import com.example.domain.model.Category

enum class FormField {
  NAME,
  DATE,
}

enum class FormErrorCode {
  NAME_REQUIRED,
  NAME_TOO_LONG,
  DATE_REQUIRED,
  DATE_INVALID,
  NOTE_TOO_LONG,
}

data class ValidatedItemDraft(
  val name: String,
  val category: Category,
  val expiryDate: String,
  val note: String?,
)

data class ItemFormValidationResult(
  val draft: ValidatedItemDraft?,
  val errors: Map<FormField, FormErrorCode>,
) {
  val isValid: Boolean
    get() = draft != null && errors.isEmpty()
}

object ItemFormValidator {
  private const val MAX_NAME_LENGTH = 120
  private const val MAX_NOTE_LENGTH = 500

  /**
   * Validates item form fields matching `item-form.ts` + Firestore rule bounds:
   * - Trimmed `name` empty -> error on `FormField.NAME`
   * - `expiryDate` empty or invalid -> error on `FormField.DATE`
   * - `note` trimmed empty -> omitted (`null`), otherwise preserved (trimmed to fit rule bounds)
   */
  fun validateItemForm(
    name: String,
    category: Category,
    expiryDate: String,
    note: String? = null,
  ): ItemFormValidationResult {
    val errors = linkedMapOf<FormField, FormErrorCode>()

    val trimmedName = name.trim()
    if (trimmedName.isEmpty()) {
      errors[FormField.NAME] = FormErrorCode.NAME_REQUIRED
    } else if (trimmedName.length > MAX_NAME_LENGTH) {
      errors[FormField.NAME] = FormErrorCode.NAME_TOO_LONG
    }

    val trimmedDate = expiryDate.trim()
    if (trimmedDate.isEmpty()) {
      errors[FormField.DATE] = FormErrorCode.DATE_REQUIRED
    } else {
      val parsed = ExpiryInputLogic.parseStrictYmd(trimmedDate)
      if (parsed == null || !ExpiryInputLogic.isValidCalendarDate(parsed.first, parsed.second, parsed.third)) {
        errors[FormField.DATE] = FormErrorCode.DATE_INVALID
      }
    }

    val normalizedNote = if (note == null || note.trim().isEmpty()) {
      null
    } else {
      note.trim().take(MAX_NOTE_LENGTH)
    }

    if (errors.isNotEmpty()) {
      return ItemFormValidationResult(draft = null, errors = errors)
    }

    return ItemFormValidationResult(
      draft = ValidatedItemDraft(
        name = trimmedName,
        category = category,
        expiryDate = trimmedDate,
        note = normalizedNote,
      ),
      errors = emptyMap(),
    )
  }
}
