package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.HistoryRepository
import com.example.domain.math.NumberFormatter
import com.example.domain.model.AppSettings
import com.example.domain.model.HistoryMode
import com.example.domain.physics.PhysicsCatalog
import com.example.domain.physics.PhysicsCategory
import com.example.domain.physics.PhysicsEvalException
import com.example.domain.physics.PhysicsFormula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhysicsUiState(
    val searchQuery: String = "",
    val selectedCategory: PhysicsCategory? = null,
    val favoritesOnly: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val filteredFormulas: List<PhysicsFormula> = PhysicsCatalog.allFormulas,
    val activeFormula: PhysicsFormula? = null,
    val selectedTargetIndex: Int = 0,
    val inputTexts: Map<String, String> = emptyMap(),
    val selectedUnitIndices: Map<String, Int> = emptyMap(),
    val formattedResult: String? = null,
    val rawResult: String? = null,
    val resultUnitLabel: String? = null,
    val substitutionText: String? = null,
    val errorMessage: String? = null
)

class PhysicsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val settingsFlow: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _uiState = MutableStateFlow(PhysicsUiState())
    val uiState: StateFlow<PhysicsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.favoritePhysicsIdsFlow.collect { favs ->
                _uiState.update { state ->
                    val filtered = PhysicsCatalog.filter(
                        query = state.searchQuery,
                        category = state.selectedCategory,
                        favoritesOnly = state.favoritesOnly,
                        favoriteIds = favs
                    )
                    state.copy(favoriteIds = favs, filteredFormulas = filtered)
                }
            }
        }
        viewModelScope.launch {
            settingsFlow.collect {
                recalculate()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = PhysicsCatalog.filter(
                query = query,
                category = state.selectedCategory,
                favoritesOnly = state.favoritesOnly,
                favoriteIds = state.favoriteIds
            )
            state.copy(searchQuery = query, filteredFormulas = filtered)
        }
    }

    fun onSelectCategory(category: PhysicsCategory?) {
        _uiState.update { state ->
            val nextCategory = if (state.selectedCategory == category) null else category
            val filtered = PhysicsCatalog.filter(
                query = state.searchQuery,
                category = nextCategory,
                favoritesOnly = state.favoritesOnly,
                favoriteIds = state.favoriteIds
            )
            state.copy(selectedCategory = nextCategory, filteredFormulas = filtered)
        }
    }

    fun onToggleFavoritesOnly() {
        _uiState.update { state ->
            val nextFavOnly = !state.favoritesOnly
            val filtered = PhysicsCatalog.filter(
                query = state.searchQuery,
                category = state.selectedCategory,
                favoritesOnly = nextFavOnly,
                favoriteIds = state.favoriteIds
            )
            state.copy(favoritesOnly = nextFavOnly, filteredFormulas = filtered)
        }
    }

    fun onToggleFavorite(formulaId: String) {
        viewModelScope.launch {
            settingsDataStore.toggleFavoritePhysics(formulaId)
        }
    }

    fun clearFilters() {
        _uiState.update { state ->
            val filtered = PhysicsCatalog.filter(
                query = "",
                category = null,
                favoritesOnly = false,
                favoriteIds = state.favoriteIds
            )
            state.copy(
                searchQuery = "",
                selectedCategory = null,
                favoritesOnly = false,
                filteredFormulas = filtered
            )
        }
    }

    fun openFormula(formula: PhysicsFormula) {
        val defaultUnits = formula.variables.associate { it.id to 0 }
        _uiState.update {
            it.copy(
                activeFormula = formula,
                selectedTargetIndex = 0,
                inputTexts = emptyMap(),
                selectedUnitIndices = defaultUnits,
                formattedResult = null,
                rawResult = null,
                resultUnitLabel = null,
                substitutionText = null,
                errorMessage = null
            )
        }
    }

    fun closeFormula() {
        _uiState.update {
            it.copy(
                activeFormula = null,
                formattedResult = null,
                rawResult = null,
                resultUnitLabel = null,
                substitutionText = null,
                errorMessage = null
            )
        }
    }

    fun selectTarget(index: Int) {
        _uiState.update { state ->
            val formula = state.activeFormula ?: return@update state
            val clamped = index.coerceIn(0, formula.solveTargets.lastIndex)
            state.copy(selectedTargetIndex = clamped)
        }
        recalculate()
    }

    fun onInputChange(varId: String, value: String) {
        val cleaned = value.filter { ch ->
            ch.isDigit() || ch == '.' || ch == ',' || ch == '-' || ch == '−' || ch == 'e' || ch == 'E' || ch == '+'
        }
        _uiState.update { state ->
            state.copy(inputTexts = state.inputTexts + (varId to cleaned))
        }
        recalculate()
    }

    fun onUnitChange(varId: String, unitIndex: Int) {
        _uiState.update { state ->
            state.copy(selectedUnitIndices = state.selectedUnitIndices + (varId to unitIndex))
        }
        recalculate()
    }

    fun clearInputs() {
        _uiState.update {
            it.copy(
                inputTexts = emptyMap(),
                formattedResult = null,
                rawResult = null,
                resultUnitLabel = null,
                substitutionText = null,
                errorMessage = null
            )
        }
    }

    fun saveToHistory(): Boolean {
        val state = _uiState.value
        val formula = state.activeFormula ?: return false
        val formatted = state.formattedResult ?: return false
        val raw = state.rawResult ?: return false
        val unit = state.resultUnitLabel ?: ""
        val target = formula.solveTargets[state.selectedTargetIndex]
        val exprSummary = state.substitutionText?.let { "${formula.titleUkr}: $it" }
            ?: "${formula.titleUkr} (${target.equationDisplay})"
        val resultWithUnit = if (unit.isNotEmpty() && unit != "—") "$formatted $unit" else formatted

        viewModelScope.launch {
            historyRepository.saveEntry(
                mode = HistoryMode.PHYSICS,
                expressionDisplay = exprSummary,
                resultDisplay = resultWithUnit,
                rawResult = raw
            )
        }
        return true
    }

    private fun recalculate() {
        val state = _uiState.value
        val formula = state.activeFormula ?: return
        val target = formula.solveTargets.getOrNull(state.selectedTargetIndex) ?: return
        val settings = settingsFlow.value

        val rawInputs = target.requiredVarIds.map { varId ->
            varId to (state.inputTexts[varId]?.trim().orEmpty())
        }
        if (rawInputs.any { it.second.isEmpty() }) {
            _uiState.update {
                it.copy(
                    formattedResult = null,
                    rawResult = null,
                    resultUnitLabel = null,
                    substitutionText = null,
                    errorMessage = null
                )
            }
            return
        }

        val siValues = mutableMapOf<String, Double>()
        val inputSummaryParts = mutableListOf<String>()

        for ((varId, rawStr) in rawInputs) {
            val variable = formula.varById(varId)
            val normalized = rawStr.replace(',', '.').replace('−', '-')
            val parsed = normalized.toDoubleOrNull()
            if (parsed == null || parsed.isNaN() || parsed.isInfinite()) {
                _uiState.update {
                    it.copy(
                        formattedResult = null,
                        rawResult = null,
                        resultUnitLabel = null,
                        substitutionText = null,
                        errorMessage = "Перевірте числове значення: ${variable.nameUkr}"
                    )
                }
                return
            }
            val unitIdx = (state.selectedUnitIndices[varId] ?: 0).coerceIn(0, variable.units.lastIndex)
            val unit = variable.units[unitIdx]
            siValues[varId] = unit.toSi(parsed)
            val unitSuffix = if (unit.label == "—") "" else " ${unit.label}"
            inputSummaryParts.add("${variable.symbol} = $rawStr$unitSuffix")
        }

        try {
            val resultSi = target.computeSi(siValues)
            if (resultSi.isNaN() || resultSi.isInfinite()) {
                throw PhysicsEvalException("Значення поза областю визначення")
            }
            val targetVar = formula.varById(target.targetVarId)
            val outUnitIdx = (state.selectedUnitIndices[target.targetVarId] ?: 0)
                .coerceIn(0, targetVar.units.lastIndex)
            val outUnit = targetVar.units[outUnitIdx]
            val outValue = outUnit.fromSi(resultSi)
            val formatted = NumberFormatter.formatPhysicsDouble(outValue, settings)
            val rawOut = if (outValue == 0.0) "0" else outValue.toString()

            _uiState.update {
                it.copy(
                    formattedResult = formatted,
                    rawResult = rawOut,
                    resultUnitLabel = outUnit.label,
                    substitutionText = "${target.equationDisplay} (${inputSummaryParts.joinToString(", ")})",
                    errorMessage = null
                )
            }
        } catch (e: PhysicsEvalException) {
            _uiState.update {
                it.copy(
                    formattedResult = null,
                    rawResult = null,
                    resultUnitLabel = null,
                    substitutionText = null,
                    errorMessage = e.messageUkr
                )
            }
        }
    }
}
