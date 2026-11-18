package com.example.domain.logic

import com.example.domain.model.Category
import java.util.Calendar
import java.util.TimeZone

data class ParsedVoiceEntry(
  val transcript: String,
  val name: String?,
  val category: Category?,
  val expiryDate: String?,
)

/**
 * Pure Kotlin parser for spoken expiry sentences on Android (`AddItemScreen`),
 * e.g. `"牛奶 九月十五号过期"`, `"酸奶 明天到期"`, `"感冒药 三个月后过期"`, `"milk expires in 1 week"`.
 */
object VoiceTranscriptParser {

  private val FULL_YMD_CN = Regex("(\\d{2,4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[日号]?")
  private val DAYS_OFFSET_ZH = Regex("(\\d+)\\s*[天日]后?")
  private val WEEKS_OFFSET_ZH = Regex("(\\d+)\\s*个?(?:星期|礼拜|周)后?")
  private val MONTHS_OFFSET_ZH = Regex("(\\d+)\\s*个?月后?")
  private val YEARS_OFFSET_ZH = Regex("(\\d+)\\s*年后?")
  private val DAYS_OFFSET_EN = Regex("\\bin\\s+(\\d+)\\s+days?\\b", RegexOption.IGNORE_CASE)
  private val WEEKS_OFFSET_EN = Regex("\\bin\\s+(\\d+)\\s+weeks?\\b", RegexOption.IGNORE_CASE)
  private val MONTHS_OFFSET_EN = Regex("\\bin\\s+(\\d+)\\s+months?\\b", RegexOption.IGNORE_CASE)

  private val MEDICINE_KEYWORDS = listOf("药", "胶囊", "维生素", "创可贴", "眼药水", "布洛芬", "medicine", "vitamin", "pill", "drops")
  private val DAILY_KEYWORDS = listOf("洗发", "沐浴", "牙膏", "洗面", "防晒", "护肤", "面霜", "洗衣", "面膜", "shampoo", "sunscreen", "soap", "toothpaste", "lotion", "wash")
  private val OTHER_KEYWORDS = listOf("电池", "纸巾", "垃圾袋", "胶水", "滤芯", "batter", "tissue", "filter")
  private val FOOD_KEYWORDS = listOf("奶", "蛋", "面包", "豆浆", "水果", "蔬菜", "肉", "鱼", "菜", "果汁", "茶", "咖啡", "零食", "饼干", "酱", "milk", "egg", "yogurt", "bread", "cheese", "juice", "meat", "fruit")

  fun parse(
    rawTranscript: String,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): ParsedVoiceEntry {
    val trimmed = rawTranscript.trim()
    if (trimmed.isEmpty()) {
      return ParsedVoiceEntry("", null, null, null)
    }

    val normalized = normalizeChineseNumerals(trimmed)
    val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
    val curY = nowCal.get(Calendar.YEAR)
    val curM = nowCal.get(Calendar.MONTH) + 1
    val curD = nowCal.get(Calendar.DAY_OF_MONTH)

    var expiryDate: String? = null
    var matchedDateSpan: String? = null

    FULL_YMD_CN.find(normalized)?.let { m ->
      val composed = ExpiryInputLogic.composeExpiryDate(m.groupValues[1], m.groupValues[2], m.groupValues[3])
      if (composed != null) {
        expiryDate = composed
        matchedDateSpan = m.value
      }
    }

    if (expiryDate == null) {
      val loose = DateLogic.parseLooseDate(normalized, nowMillis, timeZone)
      if (loose != null) {
        expiryDate = loose
        matchedDateSpan = Regex("(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}\\s*[月/\\-]\\s*\\d{1,2}\\s*[日号]?|大后天|后天|明天|今天|day after tomorrow|tomorrow|today)", RegexOption.IGNORE_CASE)
          .find(normalized)?.value
      }
    }

    if (expiryDate == null) {
      when {
        normalized.contains("半个月") -> {
          expiryDate = ExpiryInputLogic.applyExpiryPresetFromYmd(ExpiryPreset.HALF_MONTH, curY, curM, curD, timeZone)
          matchedDateSpan = "半个月"
        }
        normalized.contains("半年") -> {
          expiryDate = ExpiryInputLogic.applyExpiryPresetFromYmd(ExpiryPreset.MONTH_6, curY, curM, curD, timeZone)
          matchedDateSpan = "半年"
        }
        WEEKS_OFFSET_ZH.find(normalized) != null -> {
          val m = WEEKS_OFFSET_ZH.find(normalized)!!
          val weeks = m.groupValues[1].toIntOrNull() ?: 1
          expiryDate = ExpiryInputLogic.addDaysYmd(curY, curM, curD, weeks * 7, timeZone)
          matchedDateSpan = m.value
        }
        MONTHS_OFFSET_ZH.find(normalized) != null -> {
          val m = MONTHS_OFFSET_ZH.find(normalized)!!
          val months = m.groupValues[1].toIntOrNull() ?: 1
          expiryDate = addClampedMonths(curY, curM, curD, months, timeZone)
          matchedDateSpan = m.value
        }
        YEARS_OFFSET_ZH.find(normalized) != null -> {
          val m = YEARS_OFFSET_ZH.find(normalized)!!
          val years = m.groupValues[1].toIntOrNull() ?: 1
          expiryDate = addClampedMonths(curY, curM, curD, years * 12, timeZone)
          matchedDateSpan = m.value
        }
        DAYS_OFFSET_ZH.find(normalized) != null -> {
          val m = DAYS_OFFSET_ZH.find(normalized)!!
          val days = m.groupValues[1].toIntOrNull() ?: 1
          expiryDate = ExpiryInputLogic.addDaysYmd(curY, curM, curD, days, timeZone)
          matchedDateSpan = m.value
        }
        DAYS_OFFSET_EN.find(normalized) != null -> {
          val m = DAYS_OFFSET_EN.find(normalized)!!
          val days = m.groupValues[1].toIntOrNull() ?: 1
          expiryDate = ExpiryInputLogic.addDaysYmd(curY, curM, curD, days, timeZone)
          matchedDateSpan = m.value
        }
        WEEKS_OFFSET_EN.find(normalized) != null -> {
          val m = WEEKS_OFFSET_EN.find(normalized)!!
          val weeks = m.groupValues[1].toIntOrNull() ?: 1
          expiryDate = ExpiryInputLogic.addDaysYmd(curY, curM, curD, weeks * 7, timeZone)
          matchedDateSpan = m.value
        }
        MONTHS_OFFSET_EN.find(normalized) != null -> {
          val m = MONTHS_OFFSET_EN.find(normalized)!!
          val months = m.groupValues[1].toIntOrNull() ?: 1
          expiryDate = addClampedMonths(curY, curM, curD, months, timeZone)
          matchedDateSpan = m.value
        }
      }
    }

    var candidateName = normalized
    matchedDateSpan?.let { span ->
      candidateName = candidateName.replaceFirst(span, " ")
    }
    candidateName = candidateName
      .replace(Regex("(保质期到?|有效期到?|过期|到期|后过期|后到期|expires?\\s*(on)?)", RegexOption.IGNORE_CASE), " ")
      .replace(Regex("[，。！？,.!?]+"), " ")
      .trim()
      .split(Regex("\\s+"))
      .firstOrNull { it.isNotBlank() }
      .orEmpty()

    val inferredCategory = inferCategory(candidateName.ifEmpty { trimmed })

    return ParsedVoiceEntry(
      transcript = trimmed,
      name = candidateName.takeIf { it.isNotEmpty() },
      category = inferredCategory,
      expiryDate = expiryDate,
    )
  }

  private fun inferCategory(text: String): Category? {
    val lower = text.lowercase()
    return when {
      MEDICINE_KEYWORDS.any { lower.contains(it) } -> Category.MEDICINE
      DAILY_KEYWORDS.any { lower.contains(it) } -> Category.DAILY
      OTHER_KEYWORDS.any { lower.contains(it) } -> Category.OTHER
      FOOD_KEYWORDS.any { lower.contains(it) } -> Category.FOOD
      else -> null
    }
  }

  private fun addClampedMonths(year: Int, month: Int, day: Int, monthsToAdd: Int, timeZone: TimeZone): String {
    val totalMonthZeroBased = (year * 12 + (month - 1)) + monthsToAdd
    val targetYear = totalMonthZeroBased / 12
    val targetMonth = (totalMonthZeroBased % 12) + 1
    val cal = Calendar.getInstance(timeZone).apply {
      clear()
      set(Calendar.YEAR, targetYear)
      set(Calendar.MONTH, targetMonth - 1)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return ExpiryInputLogic.formatYmd(targetYear, targetMonth, minOf(day, maxDay))
  }

  private fun normalizeChineseNumerals(input: String): String {
    val replacements = listOf(
      "三十一" to "31", "三十" to "30",
      "二十九" to "29", "二十八" to "28", "二十七" to "27", "二十六" to "26", "二十五" to "25",
      "二十四" to "24", "二十三" to "23", "二十二" to "22", "二十一" to "21", "二十" to "20",
      "十九" to "19", "十八" to "18", "十七" to "17", "十六" to "16", "十五" to "15",
      "十四" to "14", "十三" to "13", "十二" to "12", "十一" to "11", "十" to "10",
      "九" to "9", "八" to "8", "七" to "7", "六" to "6", "五" to "5",
      "四" to "4", "三" to "3", "两" to "2", "二" to "2", "一" to "1",
    )
    var res = input
    for ((zh, num) in replacements) {
      res = res.replace(zh, num)
    }
    return res
  }
}
