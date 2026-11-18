package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.i18n.I18nDict
import com.example.domain.i18n.StringsDict
import com.example.domain.logic.DateLogic
import com.example.domain.logic.HomeSelectionResult
import com.example.domain.model.AppLanguage
import com.example.domain.model.CategoryFilter
import com.example.domain.model.HomeTabFilter
import com.example.domain.model.Item
import com.example.ui.components.BottomNavTab
import com.example.ui.components.CompactLanguageToggle
import com.example.ui.components.ItemCard
import com.example.ui.components.MinipiryBottomNavBar
import com.example.ui.components.PageHeader
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.EditorialStatNumeralStyle
import com.example.ui.theme.LocalPaperInkPalette
import com.example.ui.util.CalendarExporter

/**
 * Exact Compose port of `src/routes/_authenticated/index.tsx` from `viankie26/baozhiqi`
 * in the "Paper & Ink" — Swiss Editorial aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  language: AppLanguage,
  snackbarHostState: SnackbarHostState,
  onToggleLanguage: () -> Unit,
  onNavigateAdd: () -> Unit,
  onNavigateArchive: () -> Unit,
  onNavigateReminders: () -> Unit,
  onOpenDetail: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current
  val context = LocalContext.current

  val dateline = DateLogic.datelineString(uiState.nowMillis)

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = palette.warmBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PageHeader(
        title = dict.homeTitle,
        kicker = "${dict.brandKicker} · $dateline",
        right = {
          CompactLanguageToggle(
            language = language,
            contentDescription = stringResource(R.string.cd_toggle_language),
            onToggle = onToggleLanguage,
          )

          // Bell square button (`h-9 w-9 border-2 border-foreground`)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clickable(
                role = Role.Button,
                onClickLabel = dict.bellAria,
                onClick = onNavigateReminders,
              )
              .testTag("reminders_nav_button"),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(36.dp)
                .border(2.dp, palette.foreground)
                .background(palette.warmBg),
            ) {
              Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = dict.bellAria,
                tint = palette.foreground,
                modifier = Modifier.size(18.dp),
              )
            }
          }

          // SignOutButton (`h-9 border border-border px-2.5 text-muted-foreground`)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clickable(
                role = Role.Button,
                onClickLabel = dict.accountSignOutAria,
                onClick = onSignOut,
              )
              .testTag("sign_out_button"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier
                .height(36.dp)
                .border(1.dp, palette.border)
                .background(palette.warmBg)
                .padding(horizontal = 10.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = dict.accountSignOutAria,
                tint = palette.mutedForeground,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = dict.accountSignOut.uppercase(),
                style = EditorialKickerStyle,
                color = palette.mutedForeground,
              )
            }
          }
        },
      )
    },
    bottomBar = {
      MinipiryBottomNavBar(
        activeTab = BottomNavTab.IN_STOCK,
        language = language,
        onSelectInStock = { viewModel.onTabFilterChange(HomeTabFilter.ALL) },
        onClickAdd = onNavigateAdd,
        onSelectArchive = onNavigateArchive,
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
      PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh(language) },
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 520.dp),
      ) {
        val selection = uiState.selection

        LazyColumn(
          contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 36.dp,
          ),
          modifier = Modifier
            .fillMaxSize()
            .testTag("home_items_list"),
        ) {
          // Error / Offline banner (`border-2 border-expired/40 px-3 py-2 text-expired`)
          if (uiState.streamState is ItemsStreamState.Error) {
            item(key = "error_banner") {
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(2.dp, palette.expired.copy(alpha = 0.45f))
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("home_error_banner"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.WifiOff,
                  contentDescription = null,
                  tint = palette.expired,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = dict.offline,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.expired,
                  modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                  onClick = { viewModel.refresh(language) },
                  shape = RectangleShape,
                  border = BorderStroke(1.dp, palette.border),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                  Text(
                    text = dict.retry,
                    style = EditorialKickerStyle,
                    color = palette.foreground,
                  )
                }
              }
            }
          }

          // 1. Numbers strip (`grid-cols-3 border-b-2 border-foreground`)
          item(key = "numbers_strip") {
            NumbersStrip(
              selection = selection,
              dict = dict,
              onSelectTab = viewModel::onTabFilterChange,
            )
          }

          // 2. Solid ink alert bar (`bg-foreground px-3 py-2.5 text-background`)
          if (selection.alertCount > 0) {
            item(key = "alert_bar") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(palette.foreground)
                  .clickable {
                    viewModel.onTabFilterChange(
                      if (selection.expired.isNotEmpty()) HomeTabFilter.EXPIRED else HomeTabFilter.SOON
                    )
                  }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("home_alert_banner"),
              ) {
                Text(
                  text = dict.alertNeed(selection.alertCount).uppercase(),
                  style = EditorialKickerStyle,
                  color = palette.primaryForeground,
                )
              }
            }
          }

          // 3. "全部添加到日历" button (`border-b border-border py-3`)
          item(key = "add_all_calendar") {
            Column(modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = {
                  val exportResult = CalendarExporter.exportItemsToCalendar(
                    context = context,
                    items = selection.active,
                    language = language,
                  )
                  exportResult.fold(
                    onSuccess = { viewModel.postMessage(dict.calReady) },
                    onFailure = { viewModel.postMessage(dict.calFail) },
                  )
                },
                enabled = selection.allActive.isNotEmpty(),
                shape = RectangleShape,
                border = BorderStroke(1.dp, palette.border),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = palette.surface,
                  contentColor = palette.foreground,
                  disabledContainerColor = palette.warmBg,
                  disabledContentColor = palette.mutedForeground.copy(alpha = 0.55f),
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp)
                  .height(48.dp)
                  .testTag("add_all_calendar_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.CalendarMonth,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val suffix = if (selection.active.isNotEmpty()) {
                  dict.countSuffix(selection.active.size)
                } else {
                  ""
                }
                Text(
                  text = "${dict.addAll}$suffix",
                  style = MaterialTheme.typography.labelLarge,
                )
              }
              HairlineDivider()
            }
          }

          // 4. Search + Category chips (`allActive.length > 0`)
          if (selection.allActive.isNotEmpty()) {
            item(key = "search_and_categories") {
              SearchAndCategoriesBlock(
                query = uiState.searchQuery,
                selectedCategory = uiState.categoryFilter,
                dict = dict,
                onQueryChange = viewModel::onSearchQueryChange,
                onCategorySelect = viewModel::onCategoryFilterChange,
              )
            }
          }

          // 5. Filter tabs (`全部` / `临期 N` / `过期 N` + right `编辑/完成` button)
          item(key = "filter_tabs") {
            FilterTabsBar(
              activeTab = uiState.tabFilter,
              soonCount = selection.soon.size,
              expiredCount = selection.expired.size,
              hasAnyActiveItems = selection.allActive.isNotEmpty(),
              isEditing = uiState.isEditing,
              dict = dict,
              onSelectTab = viewModel::onTabFilterChange,
              onToggleEditing = viewModel::toggleEditing,
            )
          }

          // 6. Content body: Loading / EmptyState / NoMatch / Grouped ExpiringRow sections
          when (uiState.streamState) {
            is ItemsStreamState.Loading -> {
              item(key = "loading_indicator") {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                ) {
                  CircularProgressIndicator(
                    color = palette.foreground,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                      .size(28.dp)
                      .testTag("home_loading_indicator"),
                  )
                }
              }
            }

            is ItemsStreamState.Error,
            is ItemsStreamState.Success -> {
              if (selection.allActive.isEmpty()) {
                item(key = "empty_state") {
                  EditorialEmptyState(
                    dict = dict,
                    isSeeding = uiState.isSeeding,
                    onSeedSample = { viewModel.seedSampleItems(language) },
                  )
                }
              } else if (selection.active.isEmpty()) {
                item(key = "no_match_query") {
                  EditorialNoMatch(text = dict.noResult)
                }
              } else {
                if (selection.showExpired && selection.expired.isNotEmpty()) {
                  item(key = "sec_overdue") {
                    EditorialSectionHeader(title = dict.secOverdue)
                  }
                  items(
                    items = selection.expired,
                    key = { "exp_${it.id}" },
                  ) { item ->
                    ExpiringRow(
                      item = item,
                      language = language,
                      nowMillis = uiState.nowMillis,
                      isEditing = uiState.isEditing,
                      deleteAria = dict.deleteAria(item.name),
                      onCardClick = { onOpenDetail(item.id) },
                      onDelete = { viewModel.deleteItem(item, language) },
                    )
                  }
                }

                if (selection.showSoon && selection.soon.isNotEmpty()) {
                  item(key = "sec_soon") {
                    EditorialSectionHeader(title = dict.secSoon)
                  }
                  items(
                    items = selection.soon,
                    key = { "soon_${it.id}" },
                  ) { item ->
                    ExpiringRow(
                      item = item,
                      language = language,
                      nowMillis = uiState.nowMillis,
                      isEditing = uiState.isEditing,
                      deleteAria = dict.deleteAria(item.name),
                      onCardClick = { onOpenDetail(item.id) },
                      onDelete = { viewModel.deleteItem(item, language) },
                    )
                  }
                }

                if (selection.showNormal && selection.normal.isNotEmpty()) {
                  item(key = "sec_ok") {
                    EditorialSectionHeader(title = dict.secOk)
                  }
                  items(
                    items = selection.normal,
                    key = { "norm_${it.id}" },
                  ) { item ->
                    ExpiringRow(
                      item = item,
                      language = language,
                      nowMillis = uiState.nowMillis,
                      isEditing = uiState.isEditing,
                      deleteAria = dict.deleteAria(item.name),
                      onCardClick = { onOpenDetail(item.id) },
                      onDelete = { viewModel.deleteItem(item, language) },
                    )
                  }
                }

                if (uiState.tabFilter == HomeTabFilter.SOON && selection.soon.isEmpty()) {
                  item(key = "no_soon") {
                    EditorialNoMatch(text = dict.noSoon)
                  }
                }

                if (uiState.tabFilter == HomeTabFilter.EXPIRED && selection.expired.isEmpty()) {
                  item(key = "no_expired") {
                    EditorialNoMatch(text = dict.noExpired)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NumbersStrip(
  selection: HomeSelectionResult,
  dict: StringsDict,
  onSelectTab: (HomeTabFilter) -> Unit,
) {
  val palette = LocalPaperInkPalette.current

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min),
    ) {
      EditorialStatCell(
        label = dict.statStock,
        value = selection.active.size,
        valueColor = palette.foreground,
        onClick = { onSelectTab(HomeTabFilter.ALL) },
        testTag = "stat_card_in_stock",
        modifier = Modifier
          .weight(1f)
          .padding(end = 12.dp),
      )

      Box(
        modifier = Modifier
          .width(1.dp)
          .fillMaxHeight()
          .background(palette.border),
      )

      EditorialStatCell(
        label = dict.statSoon,
        value = selection.soon.size,
        valueColor = palette.soon,
        onClick = { onSelectTab(HomeTabFilter.SOON) },
        testTag = "stat_card_soon",
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp, end = 8.dp),
      )

      Box(
        modifier = Modifier
          .width(1.dp)
          .fillMaxHeight()
          .background(palette.border),
      )

      EditorialStatCell(
        label = dict.statOver,
        value = selection.expired.size,
        valueColor = palette.expired,
        onClick = { onSelectTab(HomeTabFilter.EXPIRED) },
        testTag = "stat_card_expired",
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
      )
    }

    // 2dp solid ink rule (`border-b-2 border-foreground`)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(palette.rule),
    )
  }
}

@Composable
private fun EditorialStatCell(
  label: String,
  value: Int,
  valueColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalPaperInkPalette.current

  Column(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clickable(role = Role.Button, onClick = onClick)
      .padding(vertical = 16.dp)
      .testTag(testTag),
  ) {
    Text(
      text = value.toString(),
      style = EditorialStatNumeralStyle,
      color = valueColor,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = label.uppercase(),
      style = EditorialKickerStyle,
      color = palette.mutedForeground,
      maxLines = 1,
    )
  }
}

@Composable
private fun SearchAndCategoriesBlock(
  query: String,
  selectedCategory: CategoryFilter,
  dict: StringsDict,
  onQueryChange: (String) -> Unit,
  onCategorySelect: (CategoryFilter) -> Unit,
) {
  val palette = LocalPaperInkPalette.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp),
  ) {
    // Sharp 0dp rectangular search input (`rounded-none pl-9`)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .background(palette.surface)
        .border(1.dp, palette.border)
        .padding(horizontal = 12.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = stringResource(R.string.cd_search_icon),
        tint = palette.mutedForeground,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.weight(1f),
      ) {
        if (query.isEmpty()) {
          Text(
            text = dict.search,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.mutedForeground,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.foreground),
          cursorBrush = SolidColor(palette.foreground),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_search_input"),
        )
      }
      if (query.isNotEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(
              role = Role.Button,
              onClickLabel = stringResource(R.string.cd_clear_search),
              onClick = { onQueryChange("") },
            )
            .testTag("clear_search_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.cd_clear_search),
            tint = palette.mutedForeground,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Rounded-full category pills (`className="shrink-0 rounded-full"`)
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
    ) {
      items(CategoryFilter.entries) { filter ->
        val selected = filter == selectedCategory
        Surface(
          shape = CircleShape,
          color = if (selected) palette.foreground else palette.surface,
          border = BorderStroke(
            width = 1.dp,
            color = if (selected) palette.foreground else palette.border,
          ),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(
              role = Role.RadioButton,
              onClick = { onCategorySelect(filter) },
            )
            .testTag("category_chip_${filter.name.lowercase()}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .height(34.dp)
              .padding(horizontal = 14.dp),
          ) {
            Text(
              text = dict.categoryFilterLabel(filter),
              style = MaterialTheme.typography.labelMedium,
              color = if (selected) palette.primaryForeground else palette.foreground,
            )
          }
        }
      }
    }

    HairlineDivider()
  }
}

@Composable
private fun FilterTabsBar(
  activeTab: HomeTabFilter,
  soonCount: Int,
  expiredCount: Int,
  hasAnyActiveItems: Boolean,
  isEditing: Boolean,
  dict: StringsDict,
  onSelectTab: (HomeTabFilter) -> Unit,
  onToggleEditing: () -> Unit,
) {
  val palette = LocalPaperInkPalette.current

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        EditorialUnderlineTab(
          label = dict.tabAll,
          active = activeTab == HomeTabFilter.ALL,
          onClick = { onSelectTab(HomeTabFilter.ALL) },
          testTag = "home_tab_all",
        )
        EditorialUnderlineTab(
          label = "${dict.tabSoon} $soonCount",
          active = activeTab == HomeTabFilter.SOON,
          onClick = { onSelectTab(HomeTabFilter.SOON) },
          testTag = "home_tab_soon",
        )
        EditorialUnderlineTab(
          label = "${dict.tabExpired} $expiredCount",
          active = activeTab == HomeTabFilter.EXPIRED,
          onClick = { onSelectTab(HomeTabFilter.EXPIRED) },
          testTag = "home_tab_expired",
        )
      }

      if (hasAnyActiveItems) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onToggleEditing)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("home_edit_toggle_button"),
        ) {
          Icon(
            imageVector = if (isEditing) Icons.Outlined.Close else Icons.Outlined.Edit,
            contentDescription = if (isEditing) dict.done else dict.edit,
            tint = palette.foreground,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = (if (isEditing) dict.done else dict.edit).uppercase(),
            style = EditorialKickerStyle,
            color = palette.foreground,
          )
        }
      }
    }

    HairlineDivider()
  }
}

@Composable
private fun EditorialUnderlineTab(
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val palette = LocalPaperInkPalette.current

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .clickable(role = Role.Tab, onClick = onClick)
      .testTag(testTag),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 6.dp),
    ) {
      Text(
        text = label.uppercase(),
        style = EditorialKickerStyle,
        color = if (active) palette.foreground else palette.mutedForeground,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Box(
        modifier = Modifier
          .height(2.dp)
          .width(28.dp)
          .background(if (active) palette.foreground else Color.Transparent),
      )
    }
  }
}

@Composable
private fun EditorialSectionHeader(title: String) {
  val palette = LocalPaperInkPalette.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(palette.warmBg)
      .padding(top = 14.dp, bottom = 8.dp),
  ) {
    Text(
      text = title.uppercase(),
      style = EditorialKickerStyle,
      color = palette.mutedForeground,
    )
  }
}

@Composable
private fun ExpiringRow(
  item: Item,
  language: AppLanguage,
  nowMillis: Long,
  isEditing: Boolean,
  deleteAria: String,
  onCardClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val palette = LocalPaperInkPalette.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        if (isEditing) palette.foreground.copy(alpha = 0.06f) else Color.Transparent
      ),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      ItemCard(
        item = item,
        language = language,
        nowMillis = nowMillis,
        onCardClick = onCardClick,
        modifier = Modifier.weight(1f),
      )

      if (isEditing) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(
              role = Role.Button,
              onClickLabel = deleteAria,
              onClick = onDelete,
            )
            .testTag("delete_item_${item.id}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(36.dp)
              .border(1.dp, palette.expired.copy(alpha = 0.45f))
              .background(palette.surface),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = deleteAria,
              tint = palette.expired,
              modifier = Modifier.size(17.dp),
            )
          }
        }
      }
    }

    HairlineDivider()
  }
}

@Composable
private fun EditorialEmptyState(
  dict: StringsDict,
  isSeeding: Boolean,
  onSeedSample: () -> Unit,
) {
  val palette = LocalPaperInkPalette.current

  Column(
    horizontalAlignment = Alignment.Start,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 72.dp)
      .testTag("home_empty_state"),
  ) {
    Text(
      text = dict.emptyTitle,
      style = MaterialTheme.typography.displaySmall,
      color = palette.foreground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = dict.emptyBody,
      style = MaterialTheme.typography.bodyMedium,
      color = palette.mutedForeground,
      modifier = Modifier.widthIn(max = 288.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    OutlinedButton(
      onClick = onSeedSample,
      enabled = !isSeeding,
      shape = RectangleShape,
      border = BorderStroke(1.dp, palette.foreground),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = palette.surface,
        contentColor = palette.foreground,
      ),
      modifier = Modifier
        .height(48.dp)
        .testTag("seed_sample_items_button"),
    ) {
      if (isSeeding) {
        CircularProgressIndicator(
          color = palette.foreground,
          strokeWidth = 2.dp,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = dict.sampleBtn,
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

@Composable
private fun EditorialNoMatch(text: String) {
  val palette = LocalPaperInkPalette.current

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 64.dp)
      .testTag("home_no_match"),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = palette.mutedForeground,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun HairlineDivider() {
  val palette = LocalPaperInkPalette.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(palette.border),
  )
}
