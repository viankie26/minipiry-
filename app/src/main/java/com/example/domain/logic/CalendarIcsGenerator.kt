package com.example.domain.logic

import com.example.domain.i18n.I18nDict
import com.example.domain.model.AppLanguage
import com.example.domain.model.Item
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CalendarIcsGenerator {

  fun generateItemIcs(
    item: Item,
    language: AppLanguage = AppLanguage.ZH,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    return buildCalendar(listOf(item), language, nowMillis)
  }

  /**
   * Exact port of `src/lib/calendar-data.ts` `buildCalendar` with 75-byte UTF-8 folding,
   * bilingual summary (`[到期提醒] 名称` / `[Expiry] Name`), and dual `VALARM` reminders.
   */
  fun buildCalendar(
    items: List<Item>,
    language: AppLanguage = AppLanguage.ZH,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val dict = I18nDict.of(language)
    val activeItems = items.filter { !it.usedUp }

    val dtStampFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val stamp = dtStampFormat.format(Date(nowMillis))

    val lines = mutableListOf(
      "BEGIN:VCALENDAR",
      "VERSION:2.0",
      "PRODID:-//Minipiry//CN",
      "CALSCALE:GREGORIAN",
      "METHOD:PUBLISH",
    )

    for (entry in activeItems) {
      val parsed = ExpiryInputLogic.parseStrictYmd(entry.expiryDate)
        ?: throw IllegalArgumentException("Invalid expiry date: ${entry.expiryDate}")
      val startCompact = "%04d%02d%02d".format(parsed.first, parsed.second, parsed.third)
      val nextDayYmd = ExpiryInputLogic.addDaysYmd(parsed.first, parsed.second, parsed.third, 1)
      val nextParsed = ExpiryInputLogic.parseStrictYmd(nextDayYmd) ?: parsed
      val endCompact = "%04d%02d%02d".format(nextParsed.first, nextParsed.second, nextParsed.third)

      val categoryText = dict.categoryName(entry.category)
      val descriptionParts = buildList {
        add(dict.calCategoryLine(categoryText))
        if (!entry.note.isNullOrBlank()) {
          add(dict.calNoteLine(entry.note))
        }
      }.joinToString("\n")

      lines.add("BEGIN:VEVENT")
      lines.add("UID:${escapeIcs(entry.id)}@expiry-ledger")
      lines.add("DTSTAMP:$stamp")
      lines.add("DTSTART;VALUE=DATE:$startCompact")
      lines.add("DTEND;VALUE=DATE:$endCompact")
      lines.add("SUMMARY:${escapeIcs(dict.calSummary(entry.name))}")
      lines.add("DESCRIPTION:${escapeIcs(descriptionParts)}")
      lines.add("BEGIN:VALARM")
      lines.add("ACTION:DISPLAY")
      lines.add("DESCRIPTION:${escapeIcs(dict.calAlarmTomorrow(entry.name))}")
      lines.add("TRIGGER:-PT15H")
      lines.add("END:VALARM")
      lines.add("BEGIN:VALARM")
      lines.add("ACTION:DISPLAY")
      lines.add("DESCRIPTION:${escapeIcs(dict.calAlarmToday(entry.name))}")
      lines.add("TRIGGER:PT9H")
      lines.add("END:VALARM")
      lines.add("END:VEVENT")
    }

    lines.add("END:VCALENDAR")
    return lines.joinToString("\r\n") { foldLine(it) } + "\r\n"
  }

  private fun foldLine(line: String): String {
    val sb = StringBuilder()
    var bytes = 0
    var index = 0
    while (index < line.length) {
      val codePoint = line.codePointAt(index)
      val charStr = String(Character.toChars(codePoint))
      val size = charStr.toByteArray(Charsets.UTF_8).size
      if (bytes + size > 75) {
        sb.append("\r\n ")
        bytes = 1
      }
      sb.append(charStr)
      bytes += size
      index += Character.charCount(codePoint)
    }
    return sb.toString()
  }

  private fun escapeIcs(raw: String): String {
    return raw
      .replace("\\", "\\\\")
      .replace(";", "\\;")
      .replace(",", "\\,")
      .replace("\r\n", "\\n")
      .replace("\r", "\\n")
      .replace("\n", "\\n")
  }
}
