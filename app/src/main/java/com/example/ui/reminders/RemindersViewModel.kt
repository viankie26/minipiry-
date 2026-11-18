package com.example.ui.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.reminder.ExpiryReminderScheduler
import com.example.data.remote.FirestoreUserSettingsRepository
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.ReminderSettings
import com.example.domain.model.AppLanguage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemindersUiState(
  val enabled: Boolean = true,
  val daysBefore: Int = 3,
  val remindHour: Int = 9,
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
)

class RemindersViewModel(
  private val settingsRepo: FirestoreUserSettingsRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _uiState = MutableStateFlow(RemindersUiState())
  val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

  private val _snackbarEvents = Channel<String>(Channel.BUFFERED)
  val snackbarEvents = _snackbarEvents.receiveAsFlow()

  init {
    viewModelScope.launch {
      val loaded = settingsRepo.loadSettings(currentUserId).getOrNull()
      _uiState.update { state ->
        if (loaded != null) {
          state.copy(
            enabled = loaded.enabled,
            daysBefore = loaded.daysBefore,
            remindHour = loaded.remindHour,
            isLoading = false,
          )
        } else {
          state.copy(isLoading = false)
        }
      }
    }
  }

  fun setEnabled(enabled: Boolean) {
    _uiState.update { it.copy(enabled = enabled) }
  }

  fun setDaysBefore(days: Int) {
    _uiState.update { it.copy(daysBefore = days) }
  }

  fun setRemindHour(hour: Int) {
    _uiState.update { it.copy(remindHour = hour.coerceIn(0, 23)) }
  }

  fun onDevicePermissionGranted(context: Context, language: AppLanguage) {
    _uiState.update { it.copy(enabled = true) }
    saveSettings(context, language, customSuccessToast = I18nDict.of(language).remEnabledToast)
  }

  fun saveSettings(
    context: Context,
    language: AppLanguage,
    customSuccessToast: String? = null,
  ) {
    val snapshot = _uiState.value
    if (snapshot.isSaving) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }
      val dict = I18nDict.of(language)
      val settings = ReminderSettings(
        enabled = snapshot.enabled,
        daysBefore = snapshot.daysBefore,
        remindHour = snapshot.remindHour,
        remindMinute = 0,
      )
      val result = settingsRepo.saveSettings(currentUserId, settings, language)
      _uiState.update { it.copy(isSaving = false) }
      result.fold(
        onSuccess = {
          ExpiryReminderScheduler.syncSchedule(
            context = context.applicationContext,
            settings = settings,
            channelName = dict.notifChannelName,
          )
          _snackbarEvents.send(customSuccessToast ?: dict.remSaved)
        },
        onFailure = {
          _snackbarEvents.send(dict.remSaveFail)
        },
      )
    }
  }

  fun postMessage(msg: String) {
    viewModelScope.launch { _snackbarEvents.send(msg) }
  }

  companion object {
    fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
        RemindersViewModel(
          settingsRepo = FirestoreUserSettingsRepository(db),
          currentUserId = currentUserId,
        )
      }
    }
  }
}
