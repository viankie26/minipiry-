package com.example.ui.add

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.FormErrorCode
import com.example.domain.logic.FormField
import com.example.domain.logic.ItemFormValidator
import com.example.domain.logic.VoiceTranscriptParser
import com.example.domain.model.AppLanguage
import com.example.domain.model.Category
import com.example.domain.model.PresetItems
import com.example.ui.components.ExpiryDateInput
import com.example.ui.components.PageHeader
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Exact Compose port of `src/routes/_authenticated/add.tsx` from `viankie26/baozhiqi`
 * in the Paper & Ink Swiss Editorial aesthetic.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(
  language: AppLanguage,
  isSaving: Boolean,
  onSaveItem: (name: String, category: Category, expiryDate: String, note: String?) -> Unit,
  onNavigateBack: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var name by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(Category.FOOD) }
  var expiryDate by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var transcript by remember { mutableStateOf("") }

  val speechLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data
        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        ?.firstOrNull()
        ?.trim()
        .orEmpty()
      if (spoken.isEmpty()) {
        scope.launch { snackbarHostState.showSnackbar(dict.addVoiceEmpty) }
      } else {
        val parsed = VoiceTranscriptParser.parse(spoken)
        transcript = parsed.transcript
        parsed.name?.let { name = it }
        parsed.category?.let { category = it }
        parsed.expiryDate?.let { expiryDate = it }
        scope.launch { snackbarHostState.showSnackbar(dict.addRecognized) }
      }
    }
  }

  fun handleLaunchVoice() {
    val localeTag = if (language == AppLanguage.ZH) Locale.CHINA.toLanguageTag() else Locale.US.toLanguageTag()
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
      putExtra(RecognizerIntent.EXTRA_PROMPT, dict.addTapSay)
    }
    try {
      speechLauncher.launch(intent)
    } catch (_: ActivityNotFoundException) {
      scope.launch { snackbarHostState.showSnackbar(dict.addVoiceFail) }
    }
  }

  fun handleSave() {
    if (isSaving) return
    val validation = ItemFormValidator.validateItemForm(
      name = name,
      category = category,
      expiryDate = expiryDate,
      note = note,
    )
    if (!validation.isValid) {
      val msg = if (validation.errors[FormField.NAME] == FormErrorCode.NAME_REQUIRED) {
        dict.addNeedName
      } else {
        dict.addNeedDate
      }
      scope.launch { snackbarHostState.showSnackbar(msg) }
      return
    }
    val draft = checkNotNull(validation.draft)
    onSaveItem(draft.name, draft.category, draft.expiryDate, draft.note)
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = palette.warmBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PageHeader(
        title = dict.addTitle,
        kicker = dict.addKicker,
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
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .background(if (isSaving) palette.muted else palette.foreground)
              .clickable(
                enabled = !isSaving,
                role = Role.Button,
                onClick = ::handleSave,
              )
              .testTag("add_save_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                color = palette.mutedForeground,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
              )
            } else {
              Text(
                text = dict.addSave.uppercase(),
                style = EditorialKickerStyle,
                color = palette.primaryForeground,
              )
            }
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
        // Voice entry block (`border-b-2 border-foreground py-6`)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = ::handleLaunchVoice)
            .padding(vertical = 22.dp)
            .testTag("add_voice_button"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(48.dp)
              .background(palette.foreground),
          ) {
            Icon(
              imageVector = Icons.Outlined.Mic,
              contentDescription = dict.addVoice,
              tint = palette.primaryForeground,
              modifier = Modifier.size(22.dp),
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = dict.addVoice,
              style = MaterialTheme.typography.headlineSmall,
              color = palette.foreground,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = dict.addTapSay.uppercase(),
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

        if (transcript.isNotBlank()) {
          Text(
            text = "“$transcript”",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = palette.mutedForeground,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp),
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(palette.border),
          )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 1. Field: Name
        EditorialFormField(label = dict.addFieldName) {
          EditorialUnderlineInput(
            value = name,
            placeholder = dict.addNamePh,
            testTag = "add_name_input",
            onValueChange = { name = it },
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Field: Category 4-col grid + Preset items
        EditorialFormField(label = dict.addFieldCategory) {
          CategoryFourColumnGrid(
            selected = category,
            language = language,
            onSelect = { category = it },
          )

          Spacer(modifier = Modifier.height(12.dp))

          val presets = PresetItems.forCategory(category, language)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            presets.forEach { preset ->
              val active = name == preset
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
                    onClick = { name = if (active) "" else preset },
                  )
                  .testTag("preset_item_$preset"),
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .height(38.dp)
                    .padding(horizontal = 12.dp),
                ) {
                  Text(
                    text = preset.uppercase(),
                    style = EditorialKickerStyle,
                    color = if (active) palette.primaryForeground else palette.mutedForeground,
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Field: Expiry Date
        EditorialFormField(label = dict.addFieldExpiry) {
          ExpiryDateInput(
            value = expiryDate,
            language = language,
            onChange = { expiryDate = it },
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Field: Note
        EditorialFormField(label = dict.addFieldNote) {
          EditorialUnderlineInput(
            value = note,
            placeholder = dict.addNotePh,
            testTag = "add_note_input",
            onValueChange = { note = it },
          )
        }
      }
    }
  }
}

@Composable
internal fun EditorialFormField(
  label: String,
  content: @Composable () -> Unit,
) {
  val palette = LocalPaperInkPalette.current
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = label.uppercase(),
      style = EditorialKickerStyle,
      color = palette.mutedForeground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    content()
  }
}

@Composable
internal fun EditorialUnderlineInput(
  value: String,
  placeholder: String,
  testTag: String,
  onValueChange: (String) -> Unit,
) {
  val palette = LocalPaperInkPalette.current
  var isFocused by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Box(
      contentAlignment = Alignment.CenterStart,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp),
    ) {
      if (value.isEmpty()) {
        Text(
          text = placeholder,
          style = MaterialTheme.typography.titleMedium,
          color = palette.mutedForeground.copy(alpha = 0.65f),
        )
      }
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = palette.foreground),
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
  }
}

@Composable
internal fun CategoryFourColumnGrid(
  selected: Category,
  language: AppLanguage,
  onSelect: (Category) -> Unit,
) {
  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
      .border(1.dp, palette.border),
  ) {
    Category.entries.forEachIndexed { idx, option ->
      if (idx > 0) {
        Box(
          modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(palette.border),
        )
      }
      val active = option == selected
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1f)
          .minimumInteractiveComponentSize()
          .background(if (active) palette.foreground else palette.warmBg)
          .clickable(
            role = Role.RadioButton,
            onClick = { onSelect(option) },
          )
          .padding(vertical = 14.dp)
          .testTag("form_category_${option.wireValue}"),
      ) {
        Text(
          text = dict.categoryName(option).uppercase(),
          style = EditorialKickerStyle,
          color = if (active) palette.primaryForeground else palette.mutedForeground,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
