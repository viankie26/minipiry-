package com.example.domain.logic

import com.example.domain.model.AppLanguage
import com.example.domain.model.Item
import com.example.domain.model.Urgency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

object DateLogic {
  private const val MILLIS_PER_DAY = 86_400_000.0
  private val ZH_WEEKDAYS = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

  private val ISO_DATE_REGEX = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$")
  private val LOOSE_MONTH_DAY_REGEX = Regex("(\\d{1,2})\\s*[月/\\-]\\s*(\\d{1,2})\\s*[日号]?")

  fun daysUntil(
    expiryDate: String,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Int {
    val parsed = ExpiryInputLogic.parseStrictYmd(expiryDate) ?: return 0
    val nowMidnight = Calendar.getInstance(timeZone).apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val expMidnight = Calendar.getInstance(timeZone).apply {
      clear()
      set(Calendar.YEAR, parsed.first)
      set(Calendar.MONTH, parsed.second - 1)
      set(Calendar.DAY_OF_MONTH, parsed.third)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val diffMs = expMidnight.timeInMillis - nowMidnight.timeInMillis
    return (diffMs / MILLIS_PER_DAY).roundToInt()
  }

  fun urgencyFromDays(days: Int): Urgency {
    return when {
      days < 0 -> Urgency.EXPIRED
      days <= 7 -> Urgency.SOON
      else -> Urgency.NORMAL
    }
  }

  fun urgencyOf(
    item: Item,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Urgency {
    return urgencyFromDays(daysUntil(item.expiryDate, nowMillis, timeZone))
  }

  fun remainingLabel(
    days: Int,
    language: AppLanguage = AppLanguage.ZH,
  ): String {
    return when (language) {
      AppLanguage.ZH -> when {
        days < 0 -> "已过期 ${abs(days)} 天"
        days == 0 -> "今天过期"
        days == 1 -> "明天过期"
        else -> "剩 $days 天"
      }
      AppLanguage.EN -> when {
        days < 0 -> "Expired ${abs(days)}d ago"
        days == 0 -> "Expires today"
        days == 1 -> "Expires tomorrow"
        else -> "$days days left"
      }
    }
  }

  fun progressOf(days: Int): Double {
    return when {
      days <= 0 -> 1.0
      days >= 30 -> 0.08
      else -> 1.0 - (days.toDouble() / 30.0)
    }
  }

  fun formatDate(
    expiryDate: String,
    language: AppLanguage = AppLanguage.ZH,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val parsed = ExpiryInputLogic.parseStrictYmd(expiryDate) ?: return expiryDate
    val cal = Calendar.getInstance(timeZone).apply {
      clear()
      set(parsed.first, parsed.second - 1, parsed.third, 12, 0, 0)
    }
    return when (language) {
      AppLanguage.ZH -> {
        val dayOfWeekIdx = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
        "${parsed.first}年${parsed.second}月${parsed.third}日 ${ZH_WEEKDAYS[dayOfWeekIdx]}"
      }
      AppLanguage.EN -> {
        val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).apply {
          this.timeZone = timeZone
        }
        formatter.format(cal.time)
      }
    }
  }

  fun datelineString(
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
    return "%04d.%02d.%02d".format(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  /**
   * Matches `src/lib/date.ts` `parseLooseDate` including embedded phrases
   * like `"牛奶 9月15号过期"`, `"今天"`, `"明天"`, `"后天"`.
   */
  fun parseLooseDate(
    rawInput: String,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String? {
    val s = rawInput.trim()
    if (s.isEmpty()) return null

    val nowCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = nowMillis
    }
    val curYear = nowCal.get(Calendar.YEAR)
    val curMonth = nowCal.get(Calendar.MONTH) + 1
    val curDay = nowCal.get(Calendar.DAY_OF_MONTH)

    ISO_DATE_REGEX.matchEntire(s)?.let { iso ->
      val y = iso.groupValues[1].toIntOrNull() ?: return null
      val m = iso.groupValues[2].toIntOrNull() ?: return null
      val d = iso.groupValues[3].toIntOrNull() ?: return null
      if (!ExpiryInputLogic.isValidCalendarDate(y, m, d)) return null
      return ExpiryInputLogic.formatYmd(y, m, d)
    }

    LOOSE_MONTH_DAY_REGEX.find(s)?.let { m ->
      val month = m.groupValues[1].toIntOrNull() ?: return null
      val day = m.groupValues[2].toIntOrNull() ?: return null
      if (month !in 1..12) return null
      val year = if (month < curMonth) curYear + 1 else curYear
      if (!ExpiryInputLogic.isValidCalendarDate(year, month, day)) return null
      return ExpiryInputLogic.formatYmd(year, month, day)
    }

    val lower = s.lowercase()
    if (s.contains("后天") || lower == "day after tomorrow") {
      return ExpiryInputLogic.addDaysYmd(curYear, curMonth, curDay, 2, timeZone)
    }
    if (s.contains("明天") || lower == "tomorrow") {
      return ExpiryInputLogic.addDaysYmd(curYear, curMonth, curDay, 1, timeZone)
    }
    if (s.contains("今天") || lower == "today") {
      return ExpiryInputLogic.formatYmd(curYear, curMonth, curDay)
    }

    return null
  }
}
