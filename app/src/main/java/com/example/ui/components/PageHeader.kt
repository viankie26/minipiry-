package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette

/**
 * Exact Compose port of `src/components/PageHeader.tsx` from `viankie26/baozhiqi`.
 * Sticky warm-paper header with uppercase kicker, Archivo Black display title,
 * right-aligned square utility controls, and a crisp `2.dp` solid ink rule.
 */
@Composable
fun PageHeader(
  title: String,
  kicker: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  backAriaLabel: String? = null,
  onBack: (() -> Unit)? = null,
  right: (@Composable RowScope.() -> Unit)? = null,
) {
  val palette = LocalPaperInkPalette.current

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(palette.warmBg)
      .windowInsetsPadding(WindowInsets.statusBars),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (onBack != null) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .clickable(
                    role = Role.Button,
                    onClick = onBack,
                  )
                  .testTag("header_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = backAriaLabel ?: "Back",
                  tint = palette.foreground,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Text(
              text = kicker.uppercase(),
              style = EditorialKickerStyle,
              color = palette.mutedForeground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (right != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = right,
          )
        }
      }

      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = palette.mutedForeground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 2dp solid ink horizontal rule inset by 20dp (`border-t-2 border-foreground`)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .padding(horizontal = 20.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .background(palette.rule),
      )
    }
  }
}

/**
 * Exact port of `src/components/LanguageSwitch.tsx` (`中文` | `English` sharp 0dp buttons).
 */
@Composable
fun LanguageSwitch(
  language: AppLanguage,
  onSelectLanguage: (AppLanguage) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalPaperInkPalette.current

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AppLanguage.entries.forEach { option ->
      val selected = option == language
      val label = if (option == AppLanguage.ZH) "中文" else "English"
      Surface(
        shape = RectangleShape,
        color = if (selected) palette.foreground else palette.surface,
        border = BorderStroke(
          width = 1.dp,
          color = if (selected) palette.foreground else palette.border,
        ),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clickable(
            role = Role.Button,
            onClick = { onSelectLanguage(option) },
          )
          .testTag("lang_switch_${option.code}"),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .height(36.dp)
            .padding(horizontal = 14.dp),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) palette.primaryForeground else palette.foreground,
          )
        }
      }
    }
  }
}

/**
 * Compact header language toggle (`EN` / `中`) matching `SignOutButton`'s `h-9 border border-border` frame.
 */
@Composable
fun CompactLanguageToggle(
  language: AppLanguage,
  contentDescription: String,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalPaperInkPalette.current
  val nextLabel = if (language == AppLanguage.ZH) "EN" else "中"

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clickable(
        role = Role.Button,
        onClickLabel = contentDescription,
        onClick = onToggle,
      )
      .testTag("lang_toggle_button"),
  ) {
    Surface(
      shape = RectangleShape,
      color = palette.warmBg,
      border = BorderStroke(1.dp, palette.border),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .height(36.dp)
          .width(36.dp),
      ) {
        Text(
          text = nextLabel,
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )
      }
    }
  }
}
