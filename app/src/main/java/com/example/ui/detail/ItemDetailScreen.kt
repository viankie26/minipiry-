package com.example.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.DateLogic
import com.example.domain.logic.FormErrorCode
import com.example.domain.logic.FormField
import com.example.domain.logic.ItemFormValidator
import com.example.domain.model.AppLanguage
import com.example.domain.model.Category
import com.example.domain.model.Item
import com.example.domain.model.Urgency
import com.example.ui.add.CategoryFourColumnGrid
import com.example.ui.add.EditorialFormField
import com.example.ui.add.EditorialUnderlineInput
import com.example.ui.components.ExpiryDateInput
import com.example.ui.components.PageHeader
import com.example.ui.theme.ArchivoBlackFamily
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette
import com.example.ui.util.CalendarExporter
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Exact Compose port of `src/routes/_authenticated/item.$id.tsx` from `viankie26/baozhiqi`.
 */
@Composable
fun ItemDetailScreen(
  item: Item?,
  language: AppLanguage,
  nowMillis: Long,
  onSaveEdits: (name: String, category: Category, expiryDate: String, note: String?) -> Unit,
  onMarkUsedUp: () -> Unit,
  onDeleteItem: () -> Unit,
  onNavigateBack: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  if (item == null) {
    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      containerColor = palette.warmBg,
    ) { innerPadding ->
      Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxSize()
          .background(palette.warmBg)
          .padding(innerPadding)
          .padding(horizontal = 24.dp),
      ) {
        Text(
          text = dict.detailNotFound,
          style = MaterialTheme.typography.headlineSmall,
          color = palette.foreground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .height(48.dp)
            .background(palette.foreground)
            .clickable(role = Role.Button, onClick = onNavigateBack)
            .padding(horizontal = 20.dp)
            .testTag("detail_back_home_button"),
        ) {
          Text(
            text = dict.detailBackHome.uppercase(),
            style = EditorialKickerStyle,
            color = palette.primaryForeground,
          )
        }
      }
    }
    return
  }

  var name by remember(item.id) { mutableStateOf(item.name) }
  var category by remember(item.id) { mutableStateOf(item.category) }
  var expiryDate by remember(item.id) { mutableStateOf(item.expiryDate) }
  var note by remember(item.id) { mutableStateOf(item.note.orEmpty()) }

  val days = DateLogic.daysUntil(item.expiryDate, nowMillis)
  val urgency = DateLogic.urgencyFromDays(days)
  val dayTone = when (urgency) {
    Urgency.EXPIRED -> palette.expired
    Urgency.SOON -> palette.soon
    Urgency.NORMAL -> palette.foreground
  }
  val dayLabel = when {
    days < 0 -> dict.detailDaysOver
    days == 0 -> dict.detailTodayExp
    else -> dict.detailDaysLeft
  }

  fun handleSave() {
    val validation = ItemFormValidator.validateItemForm(
      name = name,
      category = category,
      expiryDate = expiryDate,
      note = note,
    )
    if (!validation.isValid) {
      val msg = if (validation.errors[FormField.NAME] == FormErrorCode.NAME_REQUIRED) {
        dict.detailNeedName
      } else {
        dict.detailNeedDate
      }
      scope.launch { snackbarHostState.showSnackbar(msg) }
      return
    }
    val draft = checkNotNull(validation.draft)
    onSaveEdits(draft.name, draft.category, draft.expiryDate, draft.note)
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = palette.warmBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PageHeader(
        title = item.name,
        kicker = dict.detailKicker,
        backAriaLabel = dict.navBackAria,
        onBack = onNavigateBack,
      )
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(palette.warmBg),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(palette.rule),
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
          // 1. Used up (`border-2 border-foreground flex-1`)
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .border(2.dp, palette.foreground)
              .background(palette.warmBg)
              .clickable(role = Role.Button, onClick = onMarkUsedUp)
              .testTag("detail_used_up_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.CheckCircle,
              contentDescription = null,
              tint = palette.foreground,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = dict.detailUsedUp.uppercase(),
              style = EditorialKickerStyle,
              color = palette.foreground,
            )
          }

          // 2. Save (`flex-[1.4] bg-foreground text-background`)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1.4f)
              .height(50.dp)
              .background(palette.foreground)
              .clickable(role = Role.Button, onClick = ::handleSave)
              .testTag("detail_save_button"),
          ) {
            Text(
              text = dict.detailSave.uppercase(),
              style = EditorialKickerStyle,
              color = palette.primaryForeground,
            )
          }

          // 3. Delete (`border-2 border-foreground px-4`)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(50.dp)
              .border(2.dp, palette.foreground)
              .background(palette.warmBg)
              .clickable(
                role = Role.Button,
                onClickLabel = dict.detailDeleteAria,
                onClick = onDeleteItem,
              )
              .testTag("detail_delete_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = dict.detailDeleteAria,
              tint = palette.foreground,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(palette.warmBg)
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
          .verticalScroll(rememberScrollState())
          .padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
      ) {
        // Hero countdown row (`text-[3.5rem] leading-none tabular-nums` + 2-line kicker)
        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        ) {
          Text(
            text = abs(days).toString(),
            style = TextStyle(
              fontFamily = ArchivoBlackFamily,
              fontWeight = FontWeight.Black,
              fontSize = 56.sp,
              lineHeight = 56.sp,
              fontFeatureSettings = "tnum",
            ),
            color = dayTone,
          )

          Column(
            modifier = Modifier
              .weight(1f)
              .padding(bottom = 6.dp),
          ) {
            Text(
              text = dayLabel.uppercase(),
              style = EditorialKickerStyle,
              color = palette.mutedForeground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = DateLogic.formatDate(item.expiryDate, language).uppercase(),
              style = EditorialKickerStyle,
              color = palette.mutedForeground,
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(palette.rule),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sync single item to calendar (`border-2 border-foreground py-3`)
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(2.dp, palette.foreground)
            .background(palette.warmBg)
            .clickable(
              role = Role.Button,
              onClick = {
                val res = CalendarExporter.exportItemsToCalendar(
                  context = context,
                  items = listOf(item),
                  language = language,
                  fileName = "minipiry-${item.id}.ics",
                )
                res.fold(
                  onSuccess = { scope.launch { snackbarHostState.showSnackbar(dict.detailCalReady) } },
                  onFailure = { scope.launch { snackbarHostState.showSnackbar(dict.calFail) } },
                )
              },
            )
            .testTag("detail_sync_calendar_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = palette.foreground,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = dict.detailSyncCal.uppercase(),
            style = EditorialKickerStyle,
            color = palette.foreground,
          )
        }

        Spacer(modifier = Modifier.height(28.dp))

        EditorialFormField(label = dict.detailFieldName) {
          EditorialUnderlineInput(
            value = name,
            placeholder = "",
            testTag = "detail_name_input",
            onValueChange = { name = it },
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        EditorialFormField(label = dict.detailFieldCategory) {
          CategoryFourColumnGrid(
            selected = category,
            language = language,
            onSelect = { category = it },
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        EditorialFormField(label = dict.detailFieldExpiry) {
          ExpiryDateInput(
            value = expiryDate,
            language = language,
            onChange = { expiryDate = it },
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        EditorialFormField(label = dict.detailFieldNote) {
          EditorialUnderlineInput(
            value = note,
            placeholder = dict.detailNotePh,
            testTag = "detail_note_input",
            onValueChange = { note = it },
          )
        }
      }
    }
  }
}
