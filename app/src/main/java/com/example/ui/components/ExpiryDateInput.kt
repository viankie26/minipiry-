package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.DateLogic
import com.example.domain.logic.ExpiryInputLogic
import com.example.domain.logic.ExpiryPreset
import com.example.domain.model.AppLanguage
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette
import java.util.Calendar
import java.util.TimeZone

/**
 * Exact Compose port of `src/components/ExpiryDateInput.tsx` from `viankie26/baozhiqi`:
 * - 3 segmented numeric inputs (`年 后两位` / `月` / `日`) with `2dp` bottom ink border (`focus:border-expired`).
 * - Right-aligned `44dp` square `2dp` ink-bordered Calendar picker button.
 * - Wrapping row of 7 relative presets (`3天后`, `1周后`, `半个月`, `1个月`, `3个月`, `6个月`, `1年`).
 * - Live formatted date preview or `年填后两位即可，如 26 = 2026` hint.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDateInput(
  value: String,
  language: AppLanguage,
  onChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current

  var year by remember { mutableStateOf("") }
  var month by remember { mutableStateOf("") }
  var day by remember { mutableStateOf("") }
  var lastEmitted by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }

  LaunchedEffect(value) {
    if (value == lastEmitted) return@LaunchedEffect
    lastEmitted = null
    val parts = ExpiryInputLogic.splitExpiryDate(value)
    year = parts.year
    month = parts.month
    day = parts.day
  }

  fun updateSegments(nextYear: String = year, nextMonth: String = month, nextDay: String = day) {
    year = nextYear
    month = nextMonth
    day = nextDay
    val iso = ExpiryInputLogic.composeExpiryDate(nextYear, nextMonth, nextDay).orEmpty()
    lastEmitted = iso
    onChange(iso)
  }

  fun applyIso(iso: String) {
    val parts = ExpiryInputLogic.splitExpiryDate(iso)
    year = parts.year
    month = parts.month
    day = parts.day
    lastEmitted = iso
    onChange(iso)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      SegmentedDateField(
        value = year,
        placeholder = "26",
        maxLength = 4,
        label = dict.dateYear,
        hint = dict.dateYearHintShort,
        testTag = "expiry_year_input",
        onValueChange = { raw ->
          val digits = raw.filter { it.isDigit() }.take(4)
          updateSegments(nextYear = digits)
        },
      )

      SegmentedDateField(
        value = month,
        placeholder = "9",
        maxLength = 2,
        label = dict.dateMonth,
        testTag = "expiry_month_input",
        onValueChange = { raw ->
          val digits = raw.filter { it.isDigit() }.take(2)
          updateSegments(nextMonth = digits)
        },
      )

      SegmentedDateField(
        value = day,
        placeholder = "15",
        maxLength = 2,
        label = dict.dateDay,
        testTag = "expiry_day_input",
        onValueChange = { raw ->
          val digits = raw.filter { it.isDigit() }.take(2)
          updateSegments(nextDay = digits)
        },
      )

      Spacer(modifier = Modifier.weight(1f))

      // Calendar icon button (`h-11 w-11 border-2 border-foreground`)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .padding(bottom = 6.dp)
          .minimumInteractiveComponentSize()
          .clickable(
            role = Role.Button,
            onClickLabel = dict.dateOpenCal,
            onClick = { showDatePicker = true },
          )
          .testTag("expiry_open_calendar_button"),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(44.dp)
            .border(2.dp, palette.foreground)
            .background(palette.warmBg),
        ) {
          Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = dict.dateOpenCal,
            tint = palette.foreground,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 7 preset buttons (`DATE_PRESETS`)
    val nowMillis = remember { System.currentTimeMillis() }
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      ExpiryPreset.entries.forEach { preset ->
        val targetIso = ExpiryInputLogic.applyExpiryPreset(preset, nowMillis)
        val active = value == targetIso
        val label = dict.presetLabel(preset)

        Surface(
          shape = RectangleShape,
          color = if (active) palette.foreground else palette.warmBg,
          border = BorderStroke(
            width = 1.dp,
            color = if (active) palette.foreground else palette.border,
          ),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(
              role = Role.Button,
              onClickLabel = dict.datePresetAria(label),
              onClick = { applyIso(targetIso) },
            )
            .testTag("expiry_preset_${preset.name.lowercase()}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .height(40.dp)
              .padding(horizontal = 14.dp),
          ) {
            Text(
              text = label.uppercase(),
              style = EditorialKickerStyle,
              color = if (active) palette.primaryForeground else palette.mutedForeground,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = if (value.isNotBlank()) {
        DateLogic.formatDate(value, language)
      } else {
        dict.dateYearHint
      },
      style = EditorialKickerStyle,
      color = palette.mutedForeground,
    )
  }

  if (showDatePicker) {
    val initialUtcMillis = remember(value) {
      val parsed = ExpiryInputLogic.parseStrictYmd(value)
      if (parsed != null) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          clear()
          set(parsed.first, parsed.second - 1, parsed.third, 0, 0, 0)
        }.timeInMillis
      } else {
        null
      }
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      shape = RectangleShape,
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { utcMillis ->
              val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
              }
              val iso = ExpiryInputLogic.formatYmd(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH) + 1,
                utcCal.get(Calendar.DAY_OF_MONTH),
              )
              applyIso(iso)
            }
            showDatePicker = false
          },
          modifier = Modifier.testTag("date_picker_confirm"),
        ) {
          Text(
            text = dict.done,
            color = palette.foreground,
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(
            text = dict.accountCancel,
            color = palette.mutedForeground,
          )
        }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }
}

@Composable
private fun SegmentedDateField(
  value: String,
  placeholder: String,
  maxLength: Int,
  label: String,
  testTag: String,
  onValueChange: (String) -> Unit,
  hint: String? = null,
) {
  val palette = LocalPaperInkPalette.current
  var isFocused by remember { mutableStateOf(false) }

  Column(modifier = Modifier.width(68.dp)) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp),
    ) {
      if (value.isEmpty()) {
        Text(
          text = placeholder,
          style = MaterialTheme.typography.titleMedium,
          color = palette.mutedForeground.copy(alpha = 0.55f),
          textAlign = TextAlign.Center,
        )
      }
      BasicTextField(
        value = value,
        onValueChange = { next ->
          if (next.length <= maxLength) {
            onValueChange(next)
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.titleMedium.copy(
          color = palette.foreground,
          textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(palette.expired),
        modifier = Modifier
          .fillMaxWidth()
          .onFocusChanged { isFocused = it.isFocused }
          .testTag(testTag),
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(if (isFocused) palette.expired else palette.foreground),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = buildString {
        append(label)
        if (!hint.isNullOrBlank()) {
          append(" ")
          append(hint)
        }
      }.uppercase(),
      style = EditorialKickerStyle,
      color = palette.mutedForeground,
      maxLines = 1,
    )
  }
}
