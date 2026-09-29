package com.example.straidlov_alchimie_kalkulaka.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.straidlov_alchimie_kalkulaka.data.CombinationRepository
import com.example.straidlov_alchimie_kalkulaka.data.CombinationStats
import com.example.straidlov_alchimie_kalkulaka.data.FirebaseSyncState
import com.example.straidlov_alchimie_kalkulaka.data.FirebaseSyncLogEntry
import com.example.straidlov_alchimie_kalkulaka.model.CombinationStatus
import com.example.straidlov_alchimie_kalkulaka.model.GridCombination
import com.example.straidlov_alchimie_kalkulaka.model.GridUtils
import com.example.straidlov_alchimie_kalkulaka.model.StateConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val titleCz: String) {
    GRID("Mřížka"),
    HISTORY("Historie"),
    STATS("Statistiky"),
    SETTINGS("Nastavení")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CombinationRepository(application)

    private val _activeTab = MutableStateFlow(AppTab.GRID)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _isHistoryPreview = MutableStateFlow(false)
    val isHistoryPreview: StateFlow<Boolean> = _isHistoryPreview.asStateFlow()

    private val _currentGrid = MutableStateFlow(IntArray(9))
    val currentGrid: StateFlow<IntArray> = _currentGrid.asStateFlow()

    private val _currentId = MutableStateFlow(0)
    val currentId: StateFlow<Int> = _currentId.asStateFlow()

    private val _currentRelicName = MutableStateFlow("")
    val currentRelicName: StateFlow<String> = _currentRelicName.asStateFlow()

    val stateConfig: StateFlow<StateConfig> = repository.stateConfig
    val stats: StateFlow<CombinationStats> = repository.stats
    val firebaseSyncState: StateFlow<FirebaseSyncState> = repository.firebaseSyncState
    val firebaseSyncError: StateFlow<String?> = repository.firebaseSyncError
    val firebaseSyncLog: StateFlow<List<FirebaseSyncLogEntry>> = repository.firebaseSyncLog

    private val _historyFilterStatus = MutableStateFlow<CombinationStatus?>(null)
    val historyFilterStatus: StateFlow<CombinationStatus?> = _historyFilterStatus.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // Current combination derived state
    val currentCombination: StateFlow<GridCombination> = combine(
        _currentId,
        repository.dataVersion
    ) { id, _ ->
        repository.getCombination(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GridCombination(0))

    // History items list
    val historyItems: StateFlow<List<GridCombination>> = combine(
        _historyFilterStatus,
        _historySearchQuery,
        repository.dataVersion
    ) { filter, query, _ ->
        repository.getSavedCombinations(filter, query)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            repository.loadData()
            loadRecommended()
        }
    }

    fun selectTab(tab: AppTab) {
        _isHistoryPreview.value = false
        _activeTab.value = tab
    }

    fun cycleCell(cellIndex: Int) {
        if (cellIndex !in 0..8) return
        val updated = _currentGrid.value.copyOf()
        updated[cellIndex] = (updated[cellIndex] + 1) % 4
        updateGridAndId(updated)
    }

    fun setCell(cellIndex: Int, state: Int) {
        if (cellIndex !in 0..8) return
        val updated = _currentGrid.value.copyOf()
        updated[cellIndex] = state.coerceIn(0, 3)
        updateGridAndId(updated)
    }

    fun setGrid(grid: IntArray) {
        if (grid.size != 9) return
        updateGridAndId(grid)
    }

    fun loadById(id: Int) {
        val validId = id.coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
        val decoded = GridUtils.decodeIndex(validId)
        updateGridAndId(decoded, updateNoteFromRepo = true)
    }

    fun openHistoryPreview(id: Int) {
        loadById(id)
        _isHistoryPreview.value = true
        _activeTab.value = AppTab.GRID
    }

    fun editCurrentCombination() {
        _isHistoryPreview.value = false
    }

    fun markStatus(
        status: CombinationStatus,
        relicName: String? = null,
        onSaved: () -> Unit = {}
    ) {
        val id = _currentId.value
        val author = stateConfig.value.defaultAuthor.trim().ifBlank { null }
        val storedRelicName = when (status) {
            CombinationStatus.FUNCTIONAL -> relicName?.trim().orEmpty()
            CombinationStatus.NONFUNCTIONAL -> ""
            CombinationStatus.UNTESTED -> relicName
        }
        viewModelScope.launch {
            repository.setStatus(id, status, storedRelicName, author)
            updateGridAndId(_currentGrid.value)
            if (status == CombinationStatus.FUNCTIONAL || status == CombinationStatus.NONFUNCTIONAL) {
                loadRecommended()
            }
            onSaved()
        }
    }

    fun loadNextUntested() {
        val nextId = repository.findRecommendedUntested(_currentId.value)
        loadById(nextId)
    }

    fun loadRecommended() {
        val recommendedId = repository.findRecommendedUntested(_currentId.value)
        loadById(recommendedId)
    }

    fun loadRandomUntested() {
        val randomId = repository.findRandomUntested()
        loadById(randomId)
    }

    fun setHistoryFilter(status: CombinationStatus?) {
        _historyFilterStatus.value = status
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun updateStateConfig(config: StateConfig) {
        viewModelScope.launch {
            repository.saveStateConfig(config)
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            repository.forceSync()
        }
    }

    fun exportJson(): String {
        return repository.exportJson()
    }

    fun importJson(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importJson(jsonString)
            if (success) {
                updateGridAndId(_currentGrid.value)
            }
            onResult(success)
        }
    }

    fun importXml(xmlString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importXml(xmlString)
            if (success) {
                updateGridAndId(_currentGrid.value)
            }
            onResult(success)
        }
    }

    private fun updateGridAndId(grid: IntArray, updateNoteFromRepo: Boolean = true) {
        _currentGrid.value = grid
        val newId = GridUtils.encodeGrid(grid)
        _currentId.value = newId
        if (updateNoteFromRepo) {
            val combo = repository.getCombination(newId)
            _currentRelicName.value = combo.note
        }
    }
}
