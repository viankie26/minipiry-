package com.example.ui.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.domain.i18n.I18nDict
import com.example.domain.model.AppLanguage
import com.example.domain.model.Item
import com.example.ui.components.BottomNavTab
import com.example.ui.components.ItemCard
import com.example.ui.components.MinipiryBottomNavBar
import com.example.ui.components.PageHeader
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette

/**
 * Exact Compose port of `src/routes/_authenticated/archive.tsx` from `viankie26/baozhiqi`.
 */
@Composable
fun ArchiveScreen(
  archivedItems: List<Item>,
  language: AppLanguage,
  nowMillis: Long,
  snackbarHostState: SnackbarHostState,
  onOpenDetail: (String) -> Unit,
  onRestoreItem: (Item) -> Unit,
  onDeleteItem: (Item) -> Unit,
  onNavigateHome: () -> Unit,
  onNavigateAdd: () -> Unit,
) {
  BackHandler(onBack = onNavigateHome)

  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current

  val subtitle = if (archivedItems.isNotEmpty()) {
    dict.archiveSubtitleCount(archivedItems.size)
  } else {
    dict.archiveSubtitleEmpty
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = palette.warmBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PageHeader(
        title = dict.archiveTitle,
        kicker = dict.archiveKicker,
        subtitle = subtitle,
      )
    },
    bottomBar = {
      MinipiryBottomNavBar(
        activeTab = BottomNavTab.ARCHIVE,
        language = language,
        onSelectInStock = onNavigateHome,
        onClickAdd = onNavigateAdd,
        onSelectArchive = { /* already on Archive */ },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(palette.warmBg)
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (archivedItems.isEmpty()) {
        Column(
          horizontalAlignment = Alignment.Start,
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .padding(horizontal = 20.dp, vertical = 72.dp)
            .testTag("archive_empty_state"),
        ) {
          Text(
            text = dict.archiveEmptyTitle,
            style = MaterialTheme.typography.displaySmall,
            color = palette.foreground,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = dict.archiveEmptyBody,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.mutedForeground,
            modifier = Modifier.widthIn(max = 288.dp),
          )
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 36.dp,
          ),
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
            .testTag("archive_items_list"),
        ) {
          items(
            items = archivedItems,
            key = { "arch_${it.id}" },
          ) { item ->
            Column(modifier = Modifier.fillMaxWidth()) {
              ItemCard(
                item = item,
                language = language,
                nowMillis = nowMillis,
                muted = true,
                onCardClick = { onOpenDetail(item.id) },
              )

              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 14.dp),
              ) {
                // Restore (`flex-1 border-2 border-foreground bg-foreground text-background`)
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(2.dp, palette.foreground)
                    .background(palette.foreground)
                    .clickable(
                      role = Role.Button,
                      onClick = { onRestoreItem(item) },
                    )
                    .testTag("archive_restore_${item.id}"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = null,
                    tint = palette.primaryForeground,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = dict.archiveUndo.uppercase(),
                    style = EditorialKickerStyle,
                    color = palette.primaryForeground,
                  )
                }

                // Delete (`border-2 border-foreground px-4`)
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .height(48.dp)
                    .border(2.dp, palette.foreground)
                    .background(palette.warmBg)
                    .clickable(
                      role = Role.Button,
                      onClick = { onDeleteItem(item) },
                    )
                    .padding(horizontal = 16.dp)
                    .testTag("archive_delete_${item.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = palette.foreground,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = dict.archiveDel.uppercase(),
                    style = EditorialKickerStyle,
                    color = palette.foreground,
                  )
                }
              }

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(1.dp)
                  .background(palette.border),
              )
            }
          }
        }
      }
    }
  }
}
