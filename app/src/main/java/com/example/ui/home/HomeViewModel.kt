package com.example.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.remote.FirestoreItemsRepository
import com.example.domain.i18n.I18nDict
import com.example.domain.logic.HomeSelectionResult
import com.example.domain.logic.ItemSelectors
import com.example.domain.model.AppLanguage
import com.example.domain.model.Category
import com.example.domain.model.CategoryFilter
import com.example.domain.model.HomeTabFilter
import com.example.domain.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ItemsStreamState {
  data object Loading : ItemsStreamState
  data class Success(val items: List<Item>) : ItemsStreamState
  data class Error(val message: String) : ItemsStreamState
}

data class HomeUiState(
  val streamState: ItemsStreamState = ItemsStreamState.Loading,
  val allItems: List<Item> = emptyList(),
  val archivedItems: List<Item> = emptyList(),
  val searchQuery: String = "",
  val categoryFilter: CategoryFilter = CategoryFilter.ALL,
  val tabFilter: HomeTabFilter = HomeTabFilter.ALL,
  val isEditing: Boolean = false,
  val isRefreshing: Boolean = false,
  val isSeeding: Boolean = false,
  val isSavingItem: Boolean = false,
  val nowMillis: Long = System.currentTimeMillis(),
  val selection: HomeSelectionResult = ItemSelectors.selectHomeItems(emptyList()),
)

private data class HomeFlags(
  val isEditing: Boolean,
  val isRefreshing: Boolean,
  val isSeeding: Boolean,
  val isSavingItem: Boolean,
  val nowMillis: Long,
)

class HomeViewModel(
  private val repository: FirestoreItemsRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val searchQueryFlow = MutableStateFlow("")
  private val categoryFilterFlow = MutableStateFlow(CategoryFilter.ALL)
  private val tabFilterFlow = MutableStateFlow(HomeTabFilter.ALL)
  private val isEditingFlow = MutableStateFlow(false)
  private val isRefreshingFlow = MutableStateFlow(false)
  private val isSeedingFlow = MutableStateFlow(false)
  private val isSavingItemFlow = MutableStateFlow(false)
  private val clockTickFlow = MutableStateFlow(System.currentTimeMillis())

  private val _snackbarEvents = Channel<String>(Channel.BUFFERED)
  val snackbarEvents = _snackbarEvents.receiveAsFlow()

  private val itemsStream: StateFlow<ItemsStreamState> = repository.observeItems(currentUserId)
    .map<List<Item>, ItemsStreamState> { ItemsStreamState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing items for user", rootError)
      emit(ItemsStreamState.Error(rootError.message ?: "Failed to load items"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = ItemsStreamState.Loading,
    )

  private val flagsFlow = combine(
    isEditingFlow,
    isRefreshingFlow,
    isSeedingFlow,
    isSavingItemFlow,
    clockTickFlow,
  ) { editing, refreshing, seeding, saving, now ->
    HomeFlags(
      isEditing = editing,
      isRefreshing = refreshing,
      isSeeding = seeding,
      isSavingItem = saving,
      nowMillis = now,
    )
  }

  val uiState: StateFlow<HomeUiState> = combine(
    itemsStream,
    searchQueryFlow,
    categoryFilterFlow,
    tabFilterFlow,
    flagsFlow,
  ) { stream, query, catFilter, tab, flags ->
    val items = (stream as? ItemsStreamState.Success)?.items.orEmpty()
    val selection = ItemSelectors.selectHomeItems(
      items = items,
      query = query,
      category = catFilter,
      filter = tab,
      nowMillis = flags.nowMillis,
    )
    val archived = ItemSelectors.selectArchivedItems(items)
    HomeUiState(
      streamState = stream,
      allItems = items,
      archivedItems = archived,
      searchQuery = query,
      categoryFilter = catFilter,
      tabFilter = tab,
      isEditing = flags.isEditing && selection.allActive.isNotEmpty(),
      isRefreshing = flags.isRefreshing,
      isSeeding = flags.isSeeding,
      isSavingItem = flags.isSavingItem,
      nowMillis = flags.nowMillis,
      selection = selection,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = HomeUiState(),
  )

  fun onSearchQueryChange(query: String) {
    searchQueryFlow.value = query
  }

  fun onCategoryFilterChange(filter: CategoryFilter) {
    categoryFilterFlow.value = filter
  }

  fun onTabFilterChange(tab: HomeTabFilter) {
    tabFilterFlow.value = tab
  }

  fun toggleEditing() {
    isEditingFlow.value = !isEditingFlow.value
  }

  fun refresh(language: AppLanguage) {
    if (isRefreshingFlow.value) return
    viewModelScope.launch {
      isRefreshingFlow.value = true
      clockTickFlow.value = System.currentTimeMillis()
      val result = repository.refreshItems(currentUserId)
      isRefreshingFlow.value = false
      val dict = I18nDict.of(language)
      result.fold(
        onSuccess = { _snackbarEvents.send(dict.refreshed) },
        onFailure = { e -> _snackbarEvents.send(e.localizedMessage ?: dict.offline) },
      )
    }
  }

  fun addItem(
    name: String,
    category: Category,
    expiryDate: String,
    note: String?,
    language: AppLanguage,
    onSuccess: () -> Unit,
  ) {
    if (isSavingItemFlow.value) return
    viewModelScope.launch {
      isSavingItemFlow.value = true
      val dict = I18nDict.of(language)
      val result = repository.addItem(
        userId = currentUserId,
        name = name,
        category = category,
        expiryDate = expiryDate,
        note = note,
      )
      isSavingItemFlow.value = false
      result.fold(
        onSuccess = {
          onSuccess()
          _snackbarEvents.send(dict.addAdded)
        },
        onFailure = { e ->
          _snackbarEvents.send(e.localizedMessage ?: dict.authFail)
        },
      )
    }
  }

  fun updateItem(
    item: Item,
    name: String,
    category: Category,
    expiryDate: String,
    note: String?,
    language: AppLanguage,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      val dict = I18nDict.of(language)
      val result = repository.updateItem(
        userId = currentUserId,
        itemId = item.id,
        name = name,
        category = category,
        expiryDate = expiryDate,
        note = note,
        usedUp = item.usedUp,
      )
      result.fold(
        onSuccess = {
          onSuccess()
          _snackbarEvents.send(dict.detailUpdated)
        },
        onFailure = { e ->
          _snackbarEvents.send(e.localizedMessage ?: dict.authFail)
        },
      )
    }
  }

  fun markItemUsedUp(
    item: Item,
    language: AppLanguage,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val dict = I18nDict.of(language)
      val result = repository.setUsedUp(currentUserId, item.id, usedUp = true)
      result.fold(
        onSuccess = {
          onSuccess()
          _snackbarEvents.send(dict.detailMarkedUsed)
        },
        onFailure = { e ->
          _snackbarEvents.send(e.localizedMessage ?: dict.authFail)
        },
      )
    }
  }

  fun restoreArchivedItem(item: Item, language: AppLanguage) {
    viewModelScope.launch {
      val dict = I18nDict.of(language)
      val result = repository.setUsedUp(currentUserId, item.id, usedUp = false)
      result.fold(
        onSuccess = { _snackbarEvents.send(dict.archiveRestored) },
        onFailure = { e -> _snackbarEvents.send(e.localizedMessage ?: dict.authFail) },
      )
    }
  }

  fun deleteItem(
    item: Item,
    language: AppLanguage,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val dict = I18nDict.of(language)
      val result = repository.deleteItem(currentUserId, item.id)
      result.fold(
        onSuccess = {
          onSuccess()
          _snackbarEvents.send(dict.deleted)
        },
        onFailure = { e -> _snackbarEvents.send(e.localizedMessage ?: dict.authFail) },
      )
    }
  }

  fun seedSampleItems(language: AppLanguage) {
    if (isSeedingFlow.value) return
    viewModelScope.launch {
      isSeedingFlow.value = true
      val dict = I18nDict.of(language)
      val result = repository.seedPreviewItems(currentUserId, language)
      isSeedingFlow.value = false
      result.fold(
        onSuccess = { _snackbarEvents.send(dict.sampleDone) },
        onFailure = { e -> _snackbarEvents.send(e.localizedMessage ?: dict.authFail) },
      )
    }
  }

  fun postMessage(message: String) {
    viewModelScope.launch {
      _snackbarEvents.send(message)
    }
  }

  companion object {
    private const val TAG = "HomeViewModel"
    private const val STOP_TIMEOUT_MILLIS = 5000L

    fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        HomeViewModel(
          repository = FirestoreItemsRepository(db),
          currentUserId = currentUserId,
        )
      }
    }
  }
}
