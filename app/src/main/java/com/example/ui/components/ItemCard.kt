package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.DateLogic
import com.example.domain.model.AppLanguage
import com.example.domain.model.Item
import com.example.domain.model.Urgency
import com.example.ui.theme.EditorialCountdownStyle
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette
import kotlin.math.abs

/**
 * Exact Compose port of `src/components/ItemCard.tsx` from `viankie26/baozhiqi`.
 * 2-column Swiss editorial ledger row:
 * - Left (`weight(1f)`): `[EXPIRED | SOON | OK]` solid rectangular stamp + category kicker,
 *   `20sp` Archivo Black item name, and formatted date (`+ " — note"`).
 * - Right (`88.dp`): Giant `38sp` Archivo Black tabular countdown numeral + unit kicker.
 */
@Composable
fun ItemCard(
  item: Item,
  language: AppLanguage,
  nowMillis: Long,
  onCardClick: () -> Unit,
  modifier: Modifier = Modifier,
  muted: Boolean = false,
) {
  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current

  val days = DateLogic.daysUntil(item.expiryDate, nowMillis)
  val urgency = DateLogic.urgencyFromDays(days)
  val formattedDate = DateLogic.formatDate(item.expiryDate, language)

  val (badgeBg, badgeFg, statusText, numeralColor) = when (urgency) {
    Urgency.EXPIRED -> Quad(
      first = palette.expired,
      second = palette.statusBadgeText,
      third = "EXPIRED",
      fourth = palette.expired,
    )
    Urgency.SOON -> Quad(
      first = palette.soon,
      second = palette.statusBadgeText,
      third = "SOON",
      fourth = palette.soon,
    )
    Urgency.NORMAL -> Quad(
      first = palette.foreground,
      second = palette.primaryForeground,
      third = "OK",
      fourth = palette.foreground,
    )
  }

  val countdownUnit = when {
    days < 0 -> dict.cardDaysOver
    days == 0 -> dict.cardToday
    else -> dict.cardDaysLeft
  }

  val dateAndNoteLine = buildString {
    append(formattedDate)
    if (!item.note.isNullOrBlank()) {
      append(" — ")
      append(item.note)
    }
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    modifier = modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .alpha(if (muted) 0.55f else 1f)
      .clickable(
        role = Role.Button,
        onClick = onCardClick,
      )
      .padding(vertical = 16.dp)
      .testTag("item_card_${item.id}"),
  ) {
    Column(
      modifier = Modifier.weight(1f),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .background(badgeBg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
          Text(
            text = statusText,
            style = EditorialKickerStyle,
            color = badgeFg,
          )
        }

        Text(
          text = dict.categoryName(item.category).uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.name,
        style = MaterialTheme.typography.headlineSmall,
        color = palette.foreground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = dateAndNoteLine,
        style = MaterialTheme.typography.bodySmall,
        color = palette.mutedForeground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(88.dp),
    ) {
      Text(
        text = abs(days).toString(),
        style = EditorialCountdownStyle,
        color = numeralColor,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = countdownUnit.uppercase(),
        style = EditorialKickerStyle,
        color = palette.mutedForeground,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    }
  }
}

private data class Quad<A, B, C, D>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
)
