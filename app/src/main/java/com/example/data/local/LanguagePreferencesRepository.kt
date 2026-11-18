package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.minipiryDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "minipiry_preferences"
)

class LanguagePreferencesRepository(private val appContext: Context) {
  private val languageKey = stringPreferencesKey("app_language")
  private val notifiedIdsKey = stringSetPreferencesKey("notified_item_ids")

  val languageFlow: Flow<AppLanguage> = appContext.minipiryDataStore.data.map { prefs ->
    AppLanguage.fromCode(prefs[languageKey])
  }

  suspend fun getLanguage(): AppLanguage {
    return languageFlow.first()
  }

  suspend fun setLanguage(language: AppLanguage) {
    appContext.minipiryDataStore.edit { prefs ->
      prefs[languageKey] = language.code
    }
  }

  suspend fun getNotifiedItemIds(): Set<String> {
    return appContext.minipiryDataStore.data.first()[notifiedIdsKey].orEmpty()
  }

  suspend fun markItemsNotified(ids: Collection<String>) {
    if (ids.isEmpty()) return
    appContext.minipiryDataStore.edit { prefs ->
      val current = prefs[notifiedIdsKey].orEmpty()
      prefs[notifiedIdsKey] = current + ids
    }
  }

  suspend fun clearNotifiedHistory() {
    appContext.minipiryDataStore.edit { prefs ->
      prefs.remove(notifiedIdsKey)
    }
  }
}
