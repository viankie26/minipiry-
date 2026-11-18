package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.GoogleAuthManager
import com.example.data.auth.authStateFlow
import com.example.data.local.LanguagePreferencesRepository
import com.example.data.reminder.ExpiryReminderScheduler
import com.example.data.remote.FirestoreUserSettingsRepository
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.ReminderSettings
import com.example.domain.model.AppLanguage
import com.example.ui.add.AddItemScreen
import com.example.ui.archive.ArchiveScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.detail.ItemDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.privacy.PrivacyScreen
import com.example.ui.reminders.RemindersScreen
import com.example.ui.reminders.RemindersViewModel
import com.example.ui.theme.MinipiryTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed interface AuthenticatedRoute {
  data object Home : AuthenticatedRoute
  data object Add : AuthenticatedRoute
  data class Detail(val itemId: String) : AuthenticatedRoute
  data object Archive : AuthenticatedRoute
  data object Reminders : AuthenticatedRoute
  data object Privacy : AuthenticatedRoute
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )
    setContent {
      MinipiryTheme {
        MinipiryAppRoot()
      }
    }
  }
}

@Composable
fun MinipiryAppRoot() {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val scope = rememberCoroutineScope()
  val languageRepo = remember(appContext) { LanguagePreferencesRepository(appContext) }
  val language by languageRepo.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ZH)

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val onSelectLanguage: (AppLanguage) -> Unit = { target ->
    scope.launch { languageRepo.setLanguage(target) }
  }
  val onToggleLanguage: () -> Unit = {
    val next = if (language == AppLanguage.ZH) AppLanguage.EN else AppLanguage.ZH
    scope.launch { languageRepo.setLanguage(next) }
  }

  val user = currentUser
  if (user == null) {
    var showPrivacyFromAuth by remember { mutableStateOf(false) }
    if (showPrivacyFromAuth) {
      PrivacyScreen(
        language = language,
        onSelectLanguage = onSelectLanguage,
        onNavigateBack = { showPrivacyFromAuth = false },
      )
    } else {
      AuthScreen(
        language = language,
        onSelectLanguage = onSelectLanguage,
        onOpenPrivacy = { showPrivacyFromAuth = true },
        onAuthSuccess = { /* authStateFlow updates automatically */ },
      )
    }
  } else {
    val uid = user.uid
    val credentialManager = remember(context) { CredentialManager.create(context) }

    key(uid) {
      val homeViewModel: HomeViewModel = viewModel(
        key = "home_$uid",
        factory = HomeViewModel.factory(uid),
      )
      val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
      val sharedSnackbarHost = remember { SnackbarHostState() }

      val backStack = remember { mutableStateListOf<AuthenticatedRoute>(AuthenticatedRoute.Home) }
      val currentRoute = backStack.lastOrNull() ?: AuthenticatedRoute.Home

      fun navigateTo(route: AuthenticatedRoute) {
        if (backStack.lastOrNull() != route) {
          backStack.add(route)
        }
      }

      fun navigateToRootTab(tabRoute: AuthenticatedRoute) {
        backStack.clear()
        backStack.add(tabRoute)
      }

      fun popBack() {
        if (backStack.size > 1) {
          backStack.removeAt(backStack.lastIndex)
        } else {
          backStack[0] = AuthenticatedRoute.Home
        }
      }

      LaunchedEffect(homeViewModel) {
        homeViewModel.snackbarEvents.collect { message ->
          sharedSnackbarHost.showSnackbar(message)
        }
      }

      // Sync saved reminder schedule onto WorkManager upon sign-in if permitted
      LaunchedEffect(uid) {
        val settingsRepo = FirestoreUserSettingsRepository(appContext)
        val saved = settingsRepo.loadSettings(uid).getOrNull() ?: ReminderSettings()
        ExpiryReminderScheduler.syncSchedule(
          context = appContext,
          settings = saved,
          channelName = I18nDict.of(language).notifChannelName,
        )
      }

      when (currentRoute) {
        is AuthenticatedRoute.Home -> {
          HomeScreen(
            viewModel = homeViewModel,
            language = language,
            snackbarHostState = sharedSnackbarHost,
            onToggleLanguage = onToggleLanguage,
            onNavigateAdd = { navigateTo(AuthenticatedRoute.Add) },
            onNavigateArchive = { navigateToRootTab(AuthenticatedRoute.Archive) },
            onNavigateReminders = { navigateTo(AuthenticatedRoute.Reminders) },
            onOpenDetail = { id -> navigateTo(AuthenticatedRoute.Detail(id)) },
            onSignOut = {
              GoogleAuthManager.signOut(
                credentialManager = credentialManager,
                scope = scope,
              )
            },
          )
        }

        is AuthenticatedRoute.Add -> {
          AddItemScreen(
            language = language,
            isSaving = homeUiState.isSavingItem,
            onSaveItem = { name, category, expiryDate, note ->
              homeViewModel.addItem(
                name = name,
                category = category,
                expiryDate = expiryDate,
                note = note,
                language = language,
                onSuccess = { navigateToRootTab(AuthenticatedRoute.Home) },
              )
            },
            onNavigateBack = ::popBack,
          )
        }

        is AuthenticatedRoute.Detail -> {
          val targetItem = homeUiState.allItems.firstOrNull { it.id == currentRoute.itemId }
          ItemDetailScreen(
            item = targetItem,
            language = language,
            nowMillis = homeUiState.nowMillis,
            onSaveEdits = { name, category, expiryDate, note ->
              if (targetItem != null) {
                homeViewModel.updateItem(
                  item = targetItem,
                  name = name,
                  category = category,
                  expiryDate = expiryDate,
                  note = note,
                  language = language,
                  onSuccess = { navigateToRootTab(AuthenticatedRoute.Home) },
                )
              }
            },
            onMarkUsedUp = {
              if (targetItem != null) {
                homeViewModel.markItemUsedUp(
                  item = targetItem,
                  language = language,
                  onSuccess = { navigateToRootTab(AuthenticatedRoute.Archive) },
                )
              }
            },
            onDeleteItem = {
              if (targetItem != null) {
                homeViewModel.deleteItem(
                  item = targetItem,
                  language = language,
                  onSuccess = { popBack() },
                )
              }
            },
            onNavigateBack = ::popBack,
          )
        }

        is AuthenticatedRoute.Archive -> {
          ArchiveScreen(
            archivedItems = homeUiState.archivedItems,
            language = language,
            nowMillis = homeUiState.nowMillis,
            snackbarHostState = sharedSnackbarHost,
            onOpenDetail = { id -> navigateTo(AuthenticatedRoute.Detail(id)) },
            onRestoreItem = { item -> homeViewModel.restoreArchivedItem(item, language) },
            onDeleteItem = { item -> homeViewModel.deleteItem(item, language) },
            onNavigateHome = { navigateToRootTab(AuthenticatedRoute.Home) },
            onNavigateAdd = { navigateTo(AuthenticatedRoute.Add) },
          )
        }

        is AuthenticatedRoute.Reminders -> {
          val remindersViewModel: RemindersViewModel = viewModel(
            key = "reminders_$uid",
            factory = RemindersViewModel.factory(uid),
          )
          RemindersScreen(
            viewModel = remindersViewModel,
            language = language,
            onSelectLanguage = onSelectLanguage,
            onOpenPrivacy = { navigateTo(AuthenticatedRoute.Privacy) },
            onNavigateBack = ::popBack,
          )
        }

        is AuthenticatedRoute.Privacy -> {
          PrivacyScreen(
            language = language,
            onSelectLanguage = onSelectLanguage,
            onNavigateBack = ::popBack,
          )
        }
      }
    }
  }
}
