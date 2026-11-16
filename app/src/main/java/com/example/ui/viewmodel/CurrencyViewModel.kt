package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.CurrencyRepository
import com.example.domain.math.NumberFormatter
import com.example.domain.model.AppSettings
import com.example.domain.model.CurrencyCatalog
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.CurrencyRate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class CurrencyUiState(
    val fromCode: String = "USD",
    val toCode: String = "UAH",
    val amountInput: String = "100",
    val convertedFormatted: String = "—",
    val unitRateText: String = "",
    val rates: Map<String, CurrencyRate> = emptyMap(),
    val availableCurrencies: List<CurrencyInfo> = CurrencyCatalog.getCoreList(),
    val extraPinnedCodes: Set<String> = emptySet(),
    val updatedAtMillis: Long? = null,
    val sourceLabel: String = "",
    val isFromCache: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CurrencyViewModel(
    private val currencyRepository: CurrencyRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val settingsFlow: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val (savedFrom, savedTo, extras) = settingsDataStore.currencySelectionFlow.first()
            _uiState.update {
                it.copy(fromCode = savedFrom, toCode = savedTo, extraPinnedCodes = extras)
            }
            loadCachedThenRefresh()
        }
        viewModelScope.launch {
            settingsFlow.collect {
                recalculate()
            }
        }
    }

    private suspend fun loadCachedThenRefresh() {
        val cached = currencyRepository.getCachedRates()
        if (cached != null) {
            applyRates(
                rates = cached.rates,
                updatedAtMillis = cached.updatedAtMillis,
                isFromCache = true,
                sourceLabel = cached.sourceLabel
            )
        }
        refreshRates()
    }

    fun refreshRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = currencyRepository.fetchLiveRates()
            result.onSuccess { live ->
                applyRates(
                    rates = live.rates,
                    updatedAtMillis = live.updatedAtMillis,
                    isFromCache = false,
                    sourceLabel = live.sourceLabel
                )
            }.onFailure {
                val hasCache = _uiState.value.rates.isNotEmpty()
                val msg = if (hasCache) {
                    "Не вдалося оновити курси онлайн. Показано останні збережені дані."
                } else {
                    "Немає підключення до мережі. Не вдалося завантажити актуальні курси валют."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFromCache = hasCache,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun onDigit(digit: String) {
        _uiState.update { state ->
            val next = if (state.amountInput == "0") digit else {
                if (state.amountInput.length < 15) state.amountInput + digit else state.amountInput
            }
            state.copy(amountInput = next)
        }
        recalculate()
    }

    fun onDecimalPoint() {
        _uiState.update { state ->
            if (state.amountInput.contains('.')) return@update state
            val next = if (state.amountInput.isEmpty()) "0." else "${state.amountInput}."
            state.copy(amountInput = next)
        }
        recalculate()
    }

    fun onBackspace() {
        _uiState.update { state ->
            val next = if (state.amountInput.length <= 1) "0" else state.amountInput.dropLast(1)
            state.copy(amountInput = next)
        }
        recalculate()
    }

    fun onClear() {
        _uiState.update { it.copy(amountInput = "0") }
        recalculate()
    }

    fun onAmountTextChanged(raw: String) {
        val sanitized = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
        val parts = sanitized.split('.')
        val clean = if (parts.size > 2) "${parts[0]}.${parts.drop(1).joinToString("")}" else sanitized
        _uiState.update { it.copy(amountInput = clean.ifEmpty { "0" }) }
        recalculate()
    }

    fun swapCurrencies() {
        val curr = _uiState.value
        val newFrom = curr.toCode
        val newTo = curr.fromCode
        _uiState.update { it.copy(fromCode = newFrom, toCode = newTo) }
        recalculate()
        viewModelScope.launch {
            settingsDataStore.setCurrencyPair(newFrom, newTo)
        }
    }

    fun selectFromCurrency(code: String) {
        val upper = code.uppercase()
        val curr = _uiState.value
        if (upper == curr.toCode) {
            swapCurrencies()
            return
        }
        _uiState.update { it.copy(fromCode = upper) }
        pinIfExtra(upper)
        recalculate()
        viewModelScope.launch {
            settingsDataStore.setCurrencyPair(upper, _uiState.value.toCode)
        }
    }

    fun selectToCurrency(code: String) {
        val upper = code.uppercase()
        val curr = _uiState.value
        if (upper == curr.fromCode) {
            swapCurrencies()
            return
        }
        _uiState.update { it.copy(toCode = upper) }
        pinIfExtra(upper)
        recalculate()
        viewModelScope.launch {
            settingsDataStore.setCurrencyPair(_uiState.value.fromCode, upper)
        }
    }

    private fun pinIfExtra(code: String) {
        if (code !in CurrencyCatalog.coreCodes && code !in _uiState.value.extraPinnedCodes) {
            viewModelScope.launch {
                settingsDataStore.addExtraCurrency(code)
            }
            _uiState.update { it.copy(extraPinnedCodes = it.extraPinnedCodes + code) }
        }
    }

    private fun applyRates(
        rates: Map<String, CurrencyRate>,
        updatedAtMillis: Long,
        isFromCache: Boolean,
        sourceLabel: String
    ) {
        val coreSet = CurrencyCatalog.coreCodes.toSet()
        val ordered = buildList {
            for (code in CurrencyCatalog.coreCodes) {
                if (rates.containsKey(code) || rates.isEmpty()) {
                    add(CurrencyCatalog.getInfo(code))
                }
            }
            val others = rates.keys
                .filter { it !in coreSet }
                .sorted()
                .map { CurrencyCatalog.getInfo(it) }
            addAll(others)
        }

        _uiState.update {
            it.copy(
                rates = rates,
                availableCurrencies = ordered,
                updatedAtMillis = updatedAtMillis,
                isFromCache = isFromCache,
                sourceLabel = sourceLabel,
                isLoading = false,
                errorMessage = null
            )
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val rates = state.rates
        if (rates.isEmpty()) {
            _uiState.update { it.copy(convertedFormatted = "—", unitRateText = "") }
            return
        }
        val fromRateStr = rates[state.fromCode]?.ratePerUsd
        val toRateStr = rates[state.toCode]?.ratePerUsd
        if (fromRateStr == null || toRateStr == null) {
            _uiState.update {
                it.copy(
                    convertedFormatted = "—",
                    unitRateText = "Курс для ${state.fromCode}/${state.toCode} недоступний"
                )
            }
            return
        }

        val mc = MathContext(16, RoundingMode.HALF_UP)
        val fromPerUsd = BigDecimal(fromRateStr)
        val toPerUsd = BigDecimal(toRateStr)
        if (fromPerUsd.compareTo(BigDecimal.ZERO) == 0) return

        val unitRate = toPerUsd.divide(fromPerUsd, mc)
        val amountBd = state.amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val converted = amountBd.multiply(unitRate, mc)

        val settings = settingsFlow.value
        val formattedConverted = NumberFormatter.formatCurrencyAmount(converted, settings)
        val formattedUnitRate = NumberFormatter.formatRate(unitRate, settings)

        _uiState.update {
            it.copy(
                convertedFormatted = formattedConverted,
                unitRateText = "1 ${state.fromCode} = $formattedUnitRate ${state.toCode}"
            )
        }
    }
}
