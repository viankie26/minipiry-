package com.example.domain.logic

import com.example.domain.model.AppLanguage
import com.example.domain.model.Item
import java.util.Calendar
import java.util.TimeZone

data class ReminderSettings(
  val enabled: Boolean = true,
  val daysBefore: Int = 3,
  val remindHour: Int = 9,
  val remindMinute: Int = 0,
)

/**
 * Exact Kotlin port of `src/lib/reminder-schedule.ts` from `viankie26/baozhiqi`
 * plus local WorkManager delay & candidate-item calculation.
 */
object ReminderScheduleLogic {
  val DAYS_BEFORE_OPTIONS: List<Int> = listOf(0, 1, 2, 3, 5, 7, 14)
  val HOURS: List<Int> = (0..23).toList()

  fun daysLabel(days: Int, language: AppLanguage = AppLanguage.ZH): String {
    return when (language) {
      AppLanguage.EN -> if (days == 0) "Same day" else "${days}d before"
      AppLanguage.ZH -> if (days == 0) "当天" else "提前 $days 天"
    }
  }

  /**
   * Finds active (`!usedUp`) items expiring within `0..daysBefore` days from `nowMillis`
   * that have not yet been notified (`id !in alreadyNotifiedIds`).
   */
  fun itemsDueForNotification(
    items: List<Item>,
    daysBefore: Int,
    alreadyNotifiedIds: Set<String>,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): List<Item> {
    return items.filter { item ->
      if (item.usedUp || item.id in alreadyNotifiedIds) return@filter false
      val days = DateLogic.daysUntil(item.expiryDate, nowMillis, timeZone)
      days in 0..daysBefore
    }
  }

  /**
   * Computes milliseconds from `nowMillis` until the next occurrence of `hour:minute` local time.
   */
  fun computeInitialDelayMillis(
    targetHour: Int,
    targetMinute: Int = 0,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Long {
    val targetCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
      set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    if (targetCal.timeInMillis <= nowMillis) {
      targetCal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return (targetCal.timeInMillis - nowMillis).coerceAtLeast(60_000L)
  }
}
