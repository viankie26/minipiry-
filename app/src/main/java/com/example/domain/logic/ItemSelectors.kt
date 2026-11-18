package com.example.domain.logic

import com.example.domain.model.CategoryFilter
import com.example.domain.model.HomeTabFilter
import com.example.domain.model.Item
import com.example.domain.model.Urgency
import java.util.TimeZone

data class HomeSelectionResult(
  val allActive: List<Item>,
  val active: List<Item>,
  val expired: List<Item>,
  val soon: List<Item>,
  val normal: List<Item>,
  val alertCount: Int,
  val showExpired: Boolean,
  val showSoon: Boolean,
  val showNormal: Boolean,
)

object ItemSelectors {

  /**
   * Exact port of `src/lib/item-selectors.ts` `selectHomeItems`:
   * - Excludes `usedUp == true` while preserving incoming record order.
   * - Filters `allActive` into `active` by category and lowercase query on `name` / `note`.
   * - Partitions `active` into `expired`, `soon`, `normal` by `urgencyOf`.
   * - Computes `alertCount = expired.size + soon.size` and `showExpired / showSoon / showNormal`.
   */
  fun selectHomeItems(
    items: List<Item>,
    query: String = "",
    category: CategoryFilter = CategoryFilter.ALL,
    filter: HomeTabFilter = HomeTabFilter.ALL,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): HomeSelectionResult {
    val allActive = items.filter { !it.usedUp }
    val q = query.trim().lowercase()

    val active = allActive.filter { item ->
      (category.category == null || item.category == category.category) &&
        (q.isEmpty() ||
          item.name.lowercase().contains(q) ||
          (item.note ?: "").lowercase().contains(q))
    }

    val expired = mutableListOf<Item>()
    val soon = mutableListOf<Item>()
    val normal = mutableListOf<Item>()

    for (item in active) {
      when (DateLogic.urgencyOf(item, nowMillis, timeZone)) {
        Urgency.EXPIRED -> expired.add(item)
        Urgency.SOON -> soon.add(item)
        Urgency.NORMAL -> normal.add(item)
      }
    }

    return HomeSelectionResult(
      allActive = allActive,
      active = active,
      expired = expired,
      soon = soon,
      normal = normal,
      alertCount = expired.size + soon.size,
      showExpired = filter == HomeTabFilter.ALL || filter == HomeTabFilter.EXPIRED,
      showSoon = filter == HomeTabFilter.ALL || filter == HomeTabFilter.SOON,
      showNormal = filter == HomeTabFilter.ALL,
    )
  }

  fun selectArchivedItems(items: List<Item>): List<Item> {
    return items.filter { it.usedUp }
  }
}
