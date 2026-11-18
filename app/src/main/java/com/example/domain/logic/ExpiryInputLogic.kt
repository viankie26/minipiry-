package com.example.domain.logic

import java.util.Calendar
import java.util.TimeZone

data class ExpiryDateParts(
  val year: String,
  val month: String,
  val day: String,
)

enum class ExpiryPreset {
  DAYS_3,
  WEEK_1,
  HALF_MONTH,
  MONTH_1,
  MONTH_3,
  MONTH_6,
  YEAR_1,
}

object ExpiryInputLogic {

  fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
  }

  fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
      1, 3, 5, 7, 8, 10, 12 -> 31
      4, 6, 9, 11 -> 30
      2 -> if (isLeapYear(year)) 29 else 28
      else -> 0
    }
  }

  fun isValidCalendarDate(year: Int, month: Int, day: Int): Boolean {
    if (year !in 2000..2099) return false
    if (month !in 1..12) return false
    val maxDay = daysInMonth(year, month)
    return day in 1..maxDay
  }

  fun expandYear(yearInput: String): Int? {
    val s = yearInput.trim()
    if (!YEAR_DIGITS_REGEX.matches(s)) return null
    val raw = s.toIntOrNull() ?: return null
    val full = if (s.length <= 2) 2000 + raw else raw
    return if (full in 2000..2099) full else null
  }

  fun composeExpiryDate(
    yearInput: String,
    monthInput: String,
    dayInput: String,
  ): String? {
    val year = expandYear(yearInput) ?: return null
    val rawMonth = monthInput.trim()
    val rawDay = dayInput.trim()
    if (rawMonth.isEmpty() || rawDay.isEmpty()) return null
    val month = rawMonth.toIntOrNull() ?: return null
    val day = rawDay.toIntOrNull() ?: return null
    if (!isValidCalendarDate(year, month, day)) return null
    return formatYmd(year, month, day)
  }

  fun splitExpiryDate(expiryDate: String): ExpiryDateParts {
    val parsed = parseStrictYmd(expiryDate)
      ?: return ExpiryDateParts(year = "", month = "", day = "")
    return ExpiryDateParts(
      year = parsed.first.toString(),
      month = parsed.second.toString(),
      day = parsed.third.toString(),
    )
  }

  fun parseStrictYmd(expiryDate: String): Triple<Int, Int, Int>? {
    val trimmed = expiryDate.trim()
    val match = STRICT_YMD_REGEX.matchEntire(trimmed) ?: return null
    val year = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val day = match.groupValues[3].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..daysInMonth(year, month)) return null
    return Triple(year, month, day)
  }

  fun applyExpiryPreset(
    preset: ExpiryPreset,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val cal = Calendar.getInstance(timeZone).apply {
      timeInMillis = nowMillis
    }
    val baseYear = cal.get(Calendar.YEAR)
    val baseMonth = cal.get(Calendar.MONTH) + 1
    val baseDay = cal.get(Calendar.DAY_OF_MONTH)
    return applyExpiryPresetFromYmd(preset, baseYear, baseMonth, baseDay, timeZone)
  }

  fun applyExpiryPresetFromYmd(
    preset: ExpiryPreset,
    baseYear: Int,
    baseMonth: Int,
    baseDay: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    return when (preset) {
      ExpiryPreset.DAYS_3 -> addDaysYmd(baseYear, baseMonth, baseDay, 3, timeZone)
      ExpiryPreset.WEEK_1 -> addDaysYmd(baseYear, baseMonth, baseDay, 7, timeZone)
      // Matches src/lib/expiry-input.ts: id === "2w" ? 14
      ExpiryPreset.HALF_MONTH -> addDaysYmd(baseYear, baseMonth, baseDay, 14, timeZone)
      ExpiryPreset.MONTH_1 -> addMonthsClampedYmd(baseYear, baseMonth, baseDay, 1)
      ExpiryPreset.MONTH_3 -> addMonthsClampedYmd(baseYear, baseMonth, baseDay, 3)
      ExpiryPreset.MONTH_6 -> addMonthsClampedYmd(baseYear, baseMonth, baseDay, 6)
      ExpiryPreset.YEAR_1 -> addMonthsClampedYmd(baseYear, baseMonth, baseDay, 12)
    }
  }

  fun addDaysYmd(
    year: Int,
    month: Int,
    day: Int,
    deltaDays: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val cal = Calendar.getInstance(timeZone).apply {
      clear()
      set(year, month - 1, day, 12, 0, 0)
      add(Calendar.DAY_OF_MONTH, deltaDays)
    }
    return formatYmd(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun addMonthsClampedYmd(
    year: Int,
    month: Int,
    day: Int,
    deltaMonths: Int,
  ): String {
    val totalZeroBasedMonths = (year * 12 + (month - 1)) + deltaMonths
    val targetYear = Math.floorDiv(totalZeroBasedMonths, 12)
    val targetMonth = Math.floorMod(totalZeroBasedMonths, 12) + 1
    val clampedDay = day.coerceAtMost(daysInMonth(targetYear, targetMonth))
    return formatYmd(targetYear, targetMonth, clampedDay)
  }

  fun formatYmd(year: Int, month: Int, day: Int): String {
    return "%04d-%02d-%02d".format(year, month, day)
  }

  private val YEAR_DIGITS_REGEX = Regex("^\\d{1,4}$")
  private val STRICT_YMD_REGEX = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")
}
