package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.domain.i18n.I18nDict
import com.example.domain.model.AppLanguage
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette

enum class BottomNavTab {
  IN_STOCK,
  ARCHIVE,
}

/**
 * Exact Compose port of `src/components/BottomNav.tsx` from `viankie26/baozhiqi`.
 * Fixed warm-paper bar with `2.dp` solid ink top border and `[1fr _ 64.dp _ 1fr]` grid:
 * `[在库 / STOCK]` | solid ink `+` square | `[归档 / ARCHIVE]`.
 */
@Composable
fun MinipiryBottomNavBar(
  activeTab: BottomNavTab,
  language: AppLanguage,
  onSelectInStock: () -> Unit,
  onClickAdd: () -> Unit,
  onSelectArchive: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(palette.warmBg),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // 2dp solid ink top rule (`border-t-2 border-foreground`)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(palette.rule),
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .height(IntrinsicSize.Min)
        .heightIn(min = 52.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BottomTabLink(
        label = dict.navStock,
        active = activeTab == BottomNavTab.IN_STOCK,
        onClick = onSelectInStock,
        testTag = "bottom_nav_in_stock",
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
      )

      // 1dp vertical hairline (`border-l border-border`)
      Box(
        modifier = Modifier
          .width(1.dp)
          .fillMaxHeight()
          .background(palette.border),
      )

      // Center solid ink `+` button (`w-16 bg-foreground text-background`)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .width(64.dp)
          .fillMaxHeight()
          .minimumInteractiveComponentSize()
          .background(palette.foreground)
          .clickable(
            role = Role.Button,
            onClickLabel = dict.navAddAria,
            onClick = onClickAdd,
          )
          .testTag("bottom_nav_add"),
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = dict.navAddAria,
          tint = palette.primaryForeground,
          modifier = Modifier.size(26.dp),
        )
      }

      // 1dp vertical hairline (`border-r border-border`)
      Box(
        modifier = Modifier
          .width(1.dp)
          .fillMaxHeight()
          .background(palette.border),
      )

      BottomTabLink(
        label = dict.navArchive,
        active = activeTab == BottomNavTab.ARCHIVE,
        onClick = onSelectArchive,
        testTag = "bottom_nav_archive",
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
      )
    }
  }
}

@Composable
private fun BottomTabLink(
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalPaperInkPalette.current

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clickable(
        role = Role.Tab,
        onClick = onClick,
      )
      .padding(horizontal = 8.dp, vertical = 14.dp)
      .testTag(testTag),
  ) {
    Text(
      text = label.uppercase(),
      style = EditorialKickerStyle.copy(
        textDecoration = if (active) TextDecoration.Underline else TextDecoration.None,
      ),
      color = if (active) palette.foreground else palette.mutedForeground,
      textAlign = TextAlign.Center,
    )
  }
}
