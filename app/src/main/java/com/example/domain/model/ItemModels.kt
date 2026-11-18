package com.example.domain.model

enum class Category(val wireValue: String, val emoji: String) {
  FOOD("food", "🍎"),
  MEDICINE("medicine", "💊"),
  DAILY("daily", "🧴"),
  OTHER("other", "📦");

  companion object {
    fun fromWire(raw: String?): Category {
      if (raw.isNullOrBlank()) return OTHER
      val normalized = raw.trim().lowercase()
      return entries.firstOrNull {
        it.wireValue == normalized || it.name.lowercase() == normalized
      } ?: OTHER
    }
  }
}

enum class CategoryFilter(val category: Category?) {
  ALL(null),
  FOOD(Category.FOOD),
  MEDICINE(Category.MEDICINE),
  DAILY(Category.DAILY),
  OTHER(Category.OTHER),
}

enum class HomeTabFilter {
  ALL,
  SOON,
  EXPIRED,
}

enum class Urgency {
  EXPIRED,
  SOON,
  NORMAL,
}

enum class AppLanguage(val code: String) {
  ZH("zh"),
  EN("en");

  companion object {
    fun fromCode(raw: String?): AppLanguage {
      return if (raw?.trim()?.lowercase() == "en") EN else ZH
    }
  }
}

data class Item(
  val id: String,
  val name: String,
  val category: Category,
  val expiryDate: String, // YYYY-MM-DD，本地日期，不做 UTC 转换
  val createdAt: Long,    // 毫秒时间戳
  val note: String? = null,
  val usedUp: Boolean = false,
)

object PresetItems {
  private val zhPresets: Map<Category, List<String>> = mapOf(
    Category.FOOD to listOf("牛奶", "鸡蛋", "酸奶", "面包", "豆浆", "水果", "蔬菜", "剩菜"),
    Category.MEDICINE to listOf("感冒药", "退烧药", "肠胃药", "维生素", "创可贴", "眼药水"),
    Category.DAILY to listOf("洗发水", "沐浴露", "牙膏", "洗面奶", "防晒霜", "洗衣液"),
    Category.OTHER to listOf("电池", "纸巾", "垃圾袋", "调味料", "干货"),
  )

  private val enPresets: Map<Category, List<String>> = mapOf(
    Category.FOOD to listOf("Milk", "Eggs", "Yogurt", "Bread", "Soy milk", "Fruit", "Vegetables", "Leftovers"),
    Category.MEDICINE to listOf("Cold medicine", "Fever reducer", "Stomach medicine", "Vitamins", "Band-aids", "Eye drops"),
    Category.DAILY to listOf("Shampoo", "Body wash", "Toothpaste", "Face wash", "Sunscreen", "Laundry detergent"),
    Category.OTHER to listOf("Batteries", "Tissues", "Trash bags", "Seasoning", "Dried goods"),
  )

  fun forCategory(category: Category, language: AppLanguage = AppLanguage.ZH): List<String> {
    return when (language) {
      AppLanguage.ZH -> zhPresets[category].orEmpty()
      AppLanguage.EN -> enPresets[category].orEmpty()
    }
  }
}
