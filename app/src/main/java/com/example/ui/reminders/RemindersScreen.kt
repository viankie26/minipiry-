package com.example.ui.reminders

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.GoogleAuthManager
import com.example.data.reminder.ExpiryReminderScheduler
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.ReminderScheduleLogic
import com.example.domain.model.AppLanguage
import com.example.ui.components.LanguageSwitch
import com.example.ui.components.PageHeader
import com.example.ui.theme.EditorialKickerStyle
import com.example.ui.theme.LocalPaperInkPalette
import kotlinx.coroutines.launch

/**
 * Exact Compose port of `src/routes/_authenticated/reminders.tsx` + `DeleteAccountButton.tsx`
 * from `viankie26/baozhiqi`, backed by Android Notification Permission + WorkManager + Firestore.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
  viewModel: RemindersViewModel,
  language: AppLanguage,
  onSelectLanguage: (AppLanguage) -> Unit,
  onOpenPrivacy: () -> Unit,
  onNavigateBack: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val dict = I18nDict.of(language)
  val palette = LocalPaperInkPalette.current
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  var devicePermitted by remember {
    mutableStateOf(ExpiryReminderScheduler.hasNotificationPermission(context))
  }
  var showHourDialog by remember { mutableStateOf(false) }
  var showDeleteAccountDialog by remember { mutableStateOf(false) }
  var isDeletingAccount by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        devicePermitted = ExpiryReminderScheduler.hasNotificationPermission(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(viewModel) {
    viewModel.snackbarEvents.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    devicePermitted = granted && ExpiryReminderScheduler.hasNotificationPermission(context)
    if (devicePermitted) {
      viewModel.onDevicePermissionGranted(context, language)
    } else {
      viewModel.postMessage(dict.remNoPermission)
    }
  }

  fun handleEnableDeviceNotifications() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      devicePermitted = ExpiryReminderScheduler.hasNotificationPermission(context)
      if (devicePermitted) {
        viewModel.onDevicePermissionGranted(context, language)
      } else {
        viewModel.postMessage(dict.remNoPermission)
      }
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = palette.warmBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PageHeader(
        title = dict.remTitle,
        kicker = dict.remKicker,
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
              .background(if (uiState.isSaving) palette.muted else palette.foreground)
              .clickable(
                enabled = !uiState.isSaving,
                role = Role.Button,
                onClick = { viewModel.saveSettings(context, language) },
              )
              .testTag("reminders_save_button"),
          ) {
            if (uiState.isSaving) {
              CircularProgressIndicator(
                color = palette.mutedForeground,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
              )
            } else {
              Text(
                text = dict.remSave.uppercase(),
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
          .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 44.dp),
      ) {
        // 1. Language section (`border-b border-border pb-5`)
        Text(
          text = dict.remLanguage.uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LanguageSwitch(
          language = language,
          onSelectLanguage = onSelectLanguage,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.border),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. This device section (`border-b-2 border-foreground pb-6`)
        Text(
          text = dict.remThisDevice.uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (devicePermitted && uiState.enabled) {
          Text(
            text = dict.remDeviceOn,
            style = MaterialTheme.typography.titleMedium,
            color = palette.foreground,
          )
        } else {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .background(palette.foreground)
              .clickable(
                role = Role.Button,
                onClick = ::handleEnableDeviceNotifications,
              )
              .testTag("reminders_enable_device_button"),
          ) {
            Text(
              text = dict.remEnableBtn.uppercase(),
              style = EditorialKickerStyle,
              color = palette.primaryForeground,
            )
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(palette.rule),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Receive expiry reminders checkbox row
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(
              role = Role.Checkbox,
              onClick = { viewModel.setEnabled(!uiState.enabled) },
            )
            .padding(vertical = 4.dp)
            .testTag("reminders_receive_toggle"),
        ) {
          Text(
            text = dict.remReceive,
            style = MaterialTheme.typography.titleMedium,
            color = palette.foreground,
          )
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(24.dp)
              .border(2.dp, palette.foreground)
              .background(if (uiState.enabled) palette.foreground else palette.warmBg),
          ) {
            if (uiState.enabled) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = palette.primaryForeground,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Days before options (`DAYS_BEFORE_OPTIONS = [0, 1, 2, 3, 5, 7, 14]`)
        Text(
          text = dict.remDaysBefore.uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          ReminderScheduleLogic.DAYS_BEFORE_OPTIONS.forEach { d ->
            val active = uiState.daysBefore == d
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
                  role = Role.RadioButton,
                  onClick = { viewModel.setDaysBefore(d) },
                )
                .testTag("days_before_$d"),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .height(38.dp)
                  .padding(horizontal = 14.dp),
              ) {
                Text(
                  text = ReminderScheduleLogic.daysLabel(d, language).uppercase(),
                  style = EditorialKickerStyle,
                  color = if (active) palette.primaryForeground else palette.mutedForeground,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Reminder time (`00:00` .. `23:00`)
        Text(
          text = dict.remTime.uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val formattedHour = "%02d:00".format(uiState.remindHour)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(
              role = Role.Button,
              onClick = { showHourDialog = true },
            )
            .testTag("reminders_hour_selector"),
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 10.dp),
          ) {
            Text(
              text = formattedHour,
              style = MaterialTheme.typography.titleMedium,
              color = palette.foreground,
            )
            Icon(
              imageVector = Icons.Outlined.KeyboardArrowDown,
              contentDescription = null,
              tint = palette.foreground,
              modifier = Modifier.size(20.dp),
            )
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(2.dp)
              .background(palette.foreground),
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = dict.remExplain(uiState.daysBefore, formattedHour),
          style = MaterialTheme.typography.bodySmall,
          color = palette.mutedForeground,
        )

        Spacer(modifier = Modifier.height(36.dp))

        // 6. Account & Privacy & Delete Account (`border-t border-border pt-6`)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.border),
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = dict.remAccount.uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          contentAlignment = Alignment.CenterStart,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onOpenPrivacy)
            .testTag("reminders_privacy_link"),
        ) {
          Text(
            text = dict.remPrivacy,
            style = MaterialTheme.typography.bodyMedium.copy(
              textDecoration = TextDecoration.Underline,
            ),
            color = palette.mutedForeground,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // DeleteAccountButton (`border border-destructive py-3 text-destructive`)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, palette.expired)
            .background(palette.warmBg)
            .clickable(
              role = Role.Button,
              onClick = { showDeleteAccountDialog = true },
            )
            .testTag("delete_account_button"),
        ) {
          Text(
            text = dict.accountDeleteBtn.uppercase(),
            style = EditorialKickerStyle,
            color = palette.expired,
          )
        }
      }
    }
  }

  // 24-hour selector dialog
  if (showHourDialog) {
    AlertDialog(
      onDismissRequest = { showHourDialog = false },
      shape = RectangleShape,
      containerColor = palette.surface,
      title = {
        Text(
          text = dict.remTime.uppercase(),
          style = EditorialKickerStyle,
          color = palette.mutedForeground,
        )
      },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp),
        ) {
          items(ReminderScheduleLogic.HOURS) { h ->
            val label = "%02d:00".format(h)
            val selected = h == uiState.remindHour
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) palette.foreground else palette.surface)
                .clickable {
                  viewModel.setRemindHour(h)
                  showHourDialog = false
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("hour_option_$h"),
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) palette.primaryForeground else palette.foreground,
              )
              if (selected) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  tint = palette.primaryForeground,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showHourDialog = false }) {
          Text(text = dict.accountCancel, color = palette.foreground)
        }
      },
    )
  }

  // Permanent account deletion confirmation dialog
  if (showDeleteAccountDialog) {
    AlertDialog(
      onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
      shape = RectangleShape,
      containerColor = palette.surface,
      title = {
        Text(
          text = dict.accountConfirmTitle,
          style = MaterialTheme.typography.titleLarge,
          color = palette.foreground,
        )
      },
      text = {
        Text(
          text = dict.accountConfirmBody,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.mutedForeground,
        )
      },
      dismissButton = {
        TextButton(
          enabled = !isDeletingAccount,
          onClick = { showDeleteAccountDialog = false },
        ) {
          Text(text = dict.accountCancel, color = palette.foreground)
        }
      },
      confirmButton = {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .height(42.dp)
            .background(palette.expired)
            .clickable(
              enabled = !isDeletingAccount,
              role = Role.Button,
              onClick = {
                isDeletingAccount = true
                scope.launch {
                  val res = GoogleAuthManager.deleteAccountAndAllData(
                    context = context,
                    credentialManager = credentialManager,
                  )
                  isDeletingAccount = false
                  showDeleteAccountDialog = false
                  res.onFailure {
                    snackbarHostState.showSnackbar(dict.accountFail)
                  }
                }
              },
            )
            .padding(horizontal = 16.dp)
            .testTag("confirm_delete_account_button"),
        ) {
          Text(
            text = (if (isDeletingAccount) dict.accountDeleting else dict.accountConfirm).uppercase(),
            style = EditorialKickerStyle,
            color = palette.statusBadgeText,
          )
        }
      },
    )
  }
}
