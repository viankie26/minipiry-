package com.example

import com.example.domain.logic.CalendarIcsGenerator
import com.example.domain.logic.DateLogic
import com.example.domain.logic.ExpiryInputLogic
import com.example.domain.logic.ExpiryPreset
import com.example.domain.logic.FormErrorCode
import com.example.domain.logic.FormField
import com.example.domain.logic.ItemFormValidator
import com.example.domain.logic.ItemSelectors
import com.example.domain.logic.ReminderScheduleLogic
import com.example.domain.logic.VoiceTranscriptParser
import com.example.domain.model.AppLanguage
import com.example.domain.model.Category
import com.example.domain.model.CategoryFilter
import com.example.domain.model.HomeTabFilter
import com.example.domain.model.Item
import com.example.domain.model.PresetItems
import com.example.domain.model.Urgency
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinipiryDomainLogicTest {

  private val fixedTz: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

  private fun localMillis(year: Int, month: Int, day: Int, hour: Int = 18, minute: Int = 30): Long {
    return Calendar.getInstance(fixedTz).apply {
      clear()
      set(year, month - 1, day, hour, minute, 0)
    }.timeInMillis
  }

  @Test
  fun `daysUntil returns 0 for today, 1 for tomorrow, -1 for yesterday`() {
    val now = localMillis(2026, 11, 17, hour = 21, minute = 45)
    assertEquals(0, DateLogic.daysUntil("2026-11-17", now, fixedTz))
    assertEquals(1, DateLogic.daysUntil("2026-11-18", now, fixedTz))
    assertEquals(-1, DateLogic.daysUntil("2026-11-16", now, fixedTz))
    assertEquals(7, DateLogic.daysUntil("2026-11-24", now, fixedTz))
    assertEquals(8, DateLogic.daysUntil("2026-11-25", now, fixedTz))
  }

  @Test
  fun `urgencyOf classifies -1 expired, 0 to 7 soon, 8+ normal`() {
    val now = localMillis(2026, 11, 17)
    val makeItem = { id: String, date: String ->
      Item(id = id, name = id, category = Category.FOOD, expiryDate = date, createdAt = now)
    }

    assertEquals(Urgency.EXPIRED, DateLogic.urgencyOf(makeItem("a", "2026-11-16"), now, fixedTz))
    assertEquals(Urgency.SOON, DateLogic.urgencyOf(makeItem("b", "2026-11-17"), now, fixedTz))
    assertEquals(Urgency.SOON, DateLogic.urgencyOf(makeItem("c", "2026-11-24"), now, fixedTz))
    assertEquals(Urgency.NORMAL, DateLogic.urgencyOf(makeItem("d", "2026-11-25"), now, fixedTz))
  }

  @Test
  fun `remainingLabel formats Chinese and English labels accurately`() {
    assertEquals("已过期 3 天", DateLogic.remainingLabel(-3, AppLanguage.ZH))
    assertEquals("今天过期", DateLogic.remainingLabel(0, AppLanguage.ZH))
    assertEquals("明天过期", DateLogic.remainingLabel(1, AppLanguage.ZH))
    assertEquals("剩 5 天", DateLogic.remainingLabel(5, AppLanguage.ZH))

    assertEquals("Expired 3d ago", DateLogic.remainingLabel(-3, AppLanguage.EN))
    assertEquals("Expires today", DateLogic.remainingLabel(0, AppLanguage.EN))
    assertEquals("Expires tomorrow", DateLogic.remainingLabel(1, AppLanguage.EN))
    assertEquals("5 days left", DateLogic.remainingLabel(5, AppLanguage.EN))
  }

  @Test
  fun `progressOf returns 1_0 at 0 or negative days, 0_08 at 30+ days, and 1 - days div 30 in between`() {
    assertEquals(1.0, DateLogic.progressOf(-5), 1e-6)
    assertEquals(1.0, DateLogic.progressOf(0), 1e-6)
    assertEquals(0.5, DateLogic.progressOf(15), 1e-6)
    assertEquals(0.08, DateLogic.progressOf(30), 1e-6)
    assertEquals(0.08, DateLogic.progressOf(90), 1e-6)
  }

  @Test
  fun `formatDate formats Chinese and English dates with localized weekday`() {
    assertEquals("2026年11月14日 周六", DateLogic.formatDate("2026-11-14", AppLanguage.ZH, fixedTz))
    assertEquals("Sat, Nov 14, 2026", DateLogic.formatDate("2026-11-14", AppLanguage.EN, fixedTz))
    assertEquals("2026年11月15日 周日", DateLogic.formatDate("2026-11-15", AppLanguage.ZH, fixedTz))
  }

  @Test
  fun `parseLooseDate handles ISO dates, embedded month-day phrases, and year rollover`() {
    val nowNov17 = localMillis(2026, 11, 17)
    assertEquals("2026-11-15", DateLogic.parseLooseDate("2026-11-15", nowNov17, fixedTz))
    assertEquals("2026-11-17", DateLogic.parseLooseDate("今天", nowNov17, fixedTz))
    assertEquals("2026-11-18", DateLogic.parseLooseDate("牛奶 明天到期", nowNov17, fixedTz))
    assertEquals("2026-11-19", DateLogic.parseLooseDate("后天", nowNov17, fixedTz))
    // Month 9 < current month 11 -> next year (2027)
    assertEquals("2027-09-15", DateLogic.parseLooseDate("牛奶 9月15号过期", nowNov17, fixedTz))
    assertEquals("2027-09-15", DateLogic.parseLooseDate("9/15", nowNov17, fixedTz))
    // Month 12 >= current month 11 -> current year (2026)
    assertEquals("2026-12-15", DateLogic.parseLooseDate("12月15日", nowNov17, fixedTz))
    assertEquals("2026-12-15", DateLogic.parseLooseDate("12-15", nowNov17, fixedTz))
  }

  @Test
  fun `composeExpiryDate validates real calendar dates, 2-digit years, 14d half-month, and clamped month presets`() {
    assertEquals("2026-11-15", ExpiryInputLogic.composeExpiryDate("26", "11", "15"))
    assertEquals("2009-03-05", ExpiryInputLogic.composeExpiryDate("9", "3", "5"))
    assertNull(ExpiryInputLogic.composeExpiryDate("2026", "2", "30"))
    assertNull(ExpiryInputLogic.composeExpiryDate("2026", "2", "29"))
    assertEquals("2024-02-29", ExpiryInputLogic.composeExpiryDate("2024", "2", "29"))
    assertNull(ExpiryInputLogic.composeExpiryDate("1999", "12", "31"))

    // 2w preset adds 14 days
    assertEquals(
      "2026-11-15",
      ExpiryInputLogic.applyExpiryPresetFromYmd(ExpiryPreset.HALF_MONTH, 2026, 11, 1, fixedTz)
    )
    // Jan 31 + 1 month clamps to Feb 28 (2026) and Feb 29 (2024 leap year)
    assertEquals(
      "2026-02-28",
      ExpiryInputLogic.applyExpiryPresetFromYmd(ExpiryPreset.MONTH_1, 2026, 1, 31, fixedTz)
    )
    assertEquals(
      "2024-02-29",
      ExpiryInputLogic.applyExpiryPresetFromYmd(ExpiryPreset.MONTH_1, 2024, 1, 31, fixedTz)
    )
    assertEquals(
      "2025-02-28",
      ExpiryInputLogic.applyExpiryPresetFromYmd(ExpiryPreset.YEAR_1, 2024, 2, 29, fixedTz)
    )
  }

  @Test
  fun `validateItemForm reports empty name and empty date once each and omits blank note`() {
    val bothEmpty = ItemFormValidator.validateItemForm("   ", Category.FOOD, "", "   ")
    assertFalse(bothEmpty.isValid)
    assertEquals(2, bothEmpty.errors.size)
    assertEquals(FormErrorCode.NAME_REQUIRED, bothEmpty.errors[FormField.NAME])
    assertEquals(FormErrorCode.DATE_REQUIRED, bothEmpty.errors[FormField.DATE])

    val validNoNote = ItemFormValidator.validateItemForm("  牛奶  ", Category.FOOD, "2026-11-20", "   ")
    assertTrue(validNoNote.isValid)
    assertEquals("牛奶", validNoNote.draft?.name)
    assertNull(validNoNote.draft?.note)
  }

  @Test
  fun `selectHomeItems excludes usedUp, preserves original order, filters, and computes show flags`() {
    val now = localMillis(2026, 11, 17)
    val items = listOf(
      Item("exp_1", "牛奶A", Category.FOOD, "2026-11-10", 500L, note = "冷藏"),
      Item("soon_1", "感冒药", Category.MEDICINE, "2026-11-20", 400L),
      Item("used_1", "旧酸奶", Category.FOOD, "2026-11-01", 350L, usedUp = true),
      Item("exp_2", "面包B", Category.FOOD, "2026-11-16", 300L),
      Item("norm_1", "面霜", Category.DAILY, "2027-03-01", 200L),
    )

    val allSelection = ItemSelectors.selectHomeItems(
      items = items,
      query = "",
      category = CategoryFilter.ALL,
      filter = HomeTabFilter.ALL,
      nowMillis = now,
      timeZone = fixedTz,
    )
    assertEquals(4, allSelection.allActive.size)
    assertEquals(4, allSelection.active.size)
    assertEquals(2, allSelection.expired.size)
    assertEquals(1, allSelection.soon.size)
    assertEquals(1, allSelection.normal.size)
    assertEquals(3, allSelection.alertCount)
    assertTrue(allSelection.showExpired)
    assertTrue(allSelection.showSoon)
    assertTrue(allSelection.showNormal)
    // Original order preserved within expired group: exp_1 before exp_2
    assertEquals(listOf("exp_1", "exp_2"), allSelection.expired.map { it.id })
    assertEquals(listOf("soon_1"), allSelection.soon.map { it.id })
    assertEquals(listOf("norm_1"), allSelection.normal.map { it.id })

    // Filter == EXPIRED sets showExpired=true, showSoon=false, showNormal=false while keeping counts
    val expiredOnly = ItemSelectors.selectHomeItems(
      items = items,
      filter = HomeTabFilter.EXPIRED,
      nowMillis = now,
      timeZone = fixedTz,
    )
    assertTrue(expiredOnly.showExpired)
    assertFalse(expiredOnly.showSoon)
    assertFalse(expiredOnly.showNormal)
    assertEquals(2, expiredOnly.expired.size)

    // Archived selector returns only usedUp == true
    val archived = ItemSelectors.selectArchivedItems(items)
    assertEquals(listOf("used_1"), archived.map { it.id })
  }

  @Test
  fun `CalendarIcsGenerator produces bilingual summary, expiry-ledger UID, and dual alarms`() {
    val item = Item(
      id = "item_99",
      name = "酸奶",
      category = Category.FOOD,
      expiryDate = "2026-11-20",
      createdAt = 1000L,
      note = "顶层",
    )
    val zhIcs = CalendarIcsGenerator.generateItemIcs(item, AppLanguage.ZH, 1700000000000L)
    assertTrue(zhIcs.contains("UID:item_99@expiry-ledger"))
    assertTrue(zhIcs.contains("SUMMARY:[到期提醒] 酸奶"))
    assertTrue(zhIcs.contains("DTSTART;VALUE=DATE:20261120"))
    assertEquals(2, Regex("BEGIN:VALARM").findAll(zhIcs).count())

    val enIcs = CalendarIcsGenerator.generateItemIcs(item, AppLanguage.EN, 1700000000000L)
    assertTrue(enIcs.contains("SUMMARY:[Expiry] 酸奶"))
    assertEquals(8, PresetItems.forCategory(Category.FOOD, AppLanguage.ZH).size)
  }

  @Test
  fun `ReminderScheduleLogic formats daysLabel and selects only un-notified active items within window`() {
    assertEquals("当天", ReminderScheduleLogic.daysLabel(0, AppLanguage.ZH))
    assertEquals("提前 3 天", ReminderScheduleLogic.daysLabel(3, AppLanguage.ZH))
    assertEquals("Same day", ReminderScheduleLogic.daysLabel(0, AppLanguage.EN))
    assertEquals("3d before", ReminderScheduleLogic.daysLabel(3, AppLanguage.EN))
    assertEquals(listOf(0, 1, 2, 3, 5, 7, 14), ReminderScheduleLogic.DAYS_BEFORE_OPTIONS)

    val now = localMillis(2026, 11, 17, hour = 8, minute = 0)
    val items = listOf(
      Item("expired_1", "牛奶A", Category.FOOD, "2026-11-16", now),
      Item("today_1", "酸奶", Category.FOOD, "2026-11-17", now),
      Item("day3_1", "感冒药", Category.MEDICINE, "2026-11-20", now),
      Item("day4_1", "面包", Category.FOOD, "2026-11-21", now),
      Item("used_1", "鸡蛋", Category.FOOD, "2026-11-18", now, usedUp = true),
      Item("notified_1", "豆浆", Category.FOOD, "2026-11-18", now),
    )

    val due = ReminderScheduleLogic.itemsDueForNotification(
      items = items,
      daysBefore = 3,
      alreadyNotifiedIds = setOf("notified_1"),
      nowMillis = now,
      timeZone = fixedTz,
    )
    assertEquals(listOf("today_1", "day3_1"), due.map { it.id })

    // 08:00 -> 09:00 same day = 1 hour (3,600,000 ms)
    val delay = ReminderScheduleLogic.computeInitialDelayMillis(
      targetHour = 9,
      targetMinute = 0,
      nowMillis = now,
      timeZone = fixedTz,
    )
    assertEquals(3_600_000L, delay)
  }

  @Test
  fun `VoiceTranscriptParser extracts Chinese numerals, relative offsets, and inferred categories`() {
    val nowNov17 = localMillis(2026, 11, 17)

    val milk = VoiceTranscriptParser.parse("牛奶 九月十五号过期", nowNov17, fixedTz)
    assertEquals("牛奶", milk.name)
    assertEquals(Category.FOOD, milk.category)
    assertEquals("2027-09-15", milk.expiryDate)

    val medicine = VoiceTranscriptParser.parse("感冒药 三个月后过期", nowNov17, fixedTz)
    assertEquals("感冒药", medicine.name)
    assertEquals(Category.MEDICINE, medicine.category)
    assertEquals("2027-02-17", medicine.expiryDate)

    val sunscreen = VoiceTranscriptParser.parse("防晒霜 半年后到期", nowNov17, fixedTz)
    assertEquals("防晒霜", sunscreen.name)
    assertEquals(Category.DAILY, sunscreen.category)
    assertEquals("2027-05-17", sunscreen.expiryDate)
  }
}
