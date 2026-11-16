package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.HistoryRepository
import com.example.domain.math.NumberFormatter
import com.example.domain.math.StandardScientificEngine
import com.example.domain.model.AngleUnit
import com.example.domain.model.AppSettings
import com.example.domain.model.EvalResult
import com.example.domain.model.HistoryMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalcPadState(
    val expression: String = "",
    val livePreview: String = "",
    val finalResult: String = "",
    val rawResult: String = "",
    val errorMessage: String? = null,
    val justEvaluated: Boolean = false
)

class CalculatorViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository,
    private val engine: StandardScientificEngine = StandardScientificEngine()
) : ViewModel() {

    private val settingsFlow: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val angleUnit: StateFlow<AngleUnit> = settingsDataStore.angleUnitFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AngleUnit.DEG)

    private val _standardState = MutableStateFlow(CalcPadState())
    val standardState: StateFlow<CalcPadState> = _standardState.asStateFlow()

    private val _scientificState = MutableStateFlow(CalcPadState())
    val scientificState: StateFlow<CalcPadState> = _scientificState.asStateFlow()

    private val _isSecondFunctions = MutableStateFlow(false)
    val isSecondFunctions: StateFlow<Boolean> = _isSecondFunctions.asStateFlow()

    init {
        viewModelScope.launch {
            settingsFlow.collect {
                recomputePreview(isScientific = false)
                recomputePreview(isScientific = true)
            }
        }
        viewModelScope.launch {
            angleUnit.collect {
                recomputePreview(isScientific = true)
            }
        }
    }

    fun toggleAngleUnit() {
        val next = if (angleUnit.value == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG
        viewModelScope.launch {
            settingsDataStore.setAngleUnit(next)
        }
    }

    fun toggleSecondFunctions() {
        _isSecondFunctions.update { !it }
    }

    fun onDigit(digit: String, isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            val next = if (base == "0" && digit != ".") digit else base + digit
            state.copy(expression = next, errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onDoubleZero(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            val lastToken = base.takeLastWhile { it.isDigit() || it == '.' || it == ',' }
            val next = when {
                base.isEmpty() || lastToken == "0" -> if (base.isEmpty()) "0" else base
                lastToken.isEmpty() -> "${base}0"
                else -> "${base}00"
            }
            state.copy(expression = next, errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onDecimal(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            // Check current trailing number segment for existing decimal point
            val lastToken = base.takeLastWhile { it.isDigit() || it == '.' || it == ',' }
            if (lastToken.contains('.') || lastToken.contains(',')) {
                return@updatePad state
            }
            val next = if (base.isEmpty() || !base.last().isDigit()) {
                "${base}0."
            } else {
                "$base."
            }
            state.copy(expression = next, errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onOperator(op: String, isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated && state.rawResult.isNotEmpty()) {
                state.rawResult.replace('−', '-')
            } else {
                state.expression
            }
            val operators = setOf('+', '−', '-', '×', '÷', '^')
            val next = when {
                base.isEmpty() && op == "−" -> "−"
                base.isEmpty() -> ""
                base.last() in operators -> {
                    if (op == "−" && base.last() in setOf('×', '÷', '^')) {
                        "$base−"
                    } else if (base.length == 1 && base.last() in operators) {
                        if (op == "−") "−" else ""
                    } else {
                        base.dropLast(1) + op
                    }
                }
                base.last() == '(' && op != "−" -> base
                else -> base + op
            }
            state.copy(expression = next, errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onFunction(funcPrefix: String, isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(
                expression = base + funcPrefix,
                errorMessage = null,
                justEvaluated = false
            )
        }
        recomputePreview(isScientific)
    }

    fun onConstant(symbol: String, isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(
                expression = base + symbol,
                errorMessage = null,
                justEvaluated = false
            )
        }
        recomputePreview(isScientific)
    }

    fun onPercent(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated && state.rawResult.isNotEmpty()) {
                state.rawResult
            } else {
                state.expression
            }
            if (base.isEmpty()) return@updatePad state
            val last = base.last()
            if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '!') {
                state.copy(expression = "$base%", errorMessage = null, justEvaluated = false)
            } else {
                state
            }
        }
        recomputePreview(isScientific)
    }

    fun onFactorial(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated && state.rawResult.isNotEmpty()) {
                state.rawResult
            } else {
                state.expression
            }
            if (base.isEmpty()) return@updatePad state
            val last = base.last()
            if (last.isDigit() || last == ')') {
                state.copy(expression = "$base!", errorMessage = null, justEvaluated = false)
            } else {
                state
            }
        }
        recomputePreview(isScientific)
    }

    fun onSquare(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated && state.rawResult.isNotEmpty()) {
                state.rawResult
            } else {
                state.expression
            }
            if (base.isEmpty()) return@updatePad state
            val last = base.last()
            if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '!') {
                state.copy(expression = "${base}²", errorMessage = null, justEvaluated = false)
            } else {
                state
            }
        }
        recomputePreview(isScientific)
    }

    fun onTenPower(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(expression = "${base}10^(", errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onNthRoot(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated && state.rawResult.isNotEmpty()) {
                state.rawResult
            } else {
                state.expression
            }
            if (base.isEmpty()) return@updatePad state
            val last = base.last()
            if (last.isDigit() || last == ')' || last == 'π' || last == 'e') {
                state.copy(expression = "${base}ʸ√(", errorMessage = null, justEvaluated = false)
            } else {
                state
            }
        }
        recomputePreview(isScientific)
    }

    fun onSmartParenthesis(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            val openCount = base.count { it == '(' }
            val closeCount = base.count { it == ')' }
            val last = base.lastOrNull()
            val canClose = openCount > closeCount && last != null &&
                (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!' || last == '²')
            val token = if (canClose) ")" else "("
            state.copy(expression = base + token, errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onOpenParenthesis(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(expression = "$base(", errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onCloseParenthesis(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = state.expression
            val openCount = base.count { it == '(' }
            val closeCount = base.count { it == ')' }
            if (openCount > closeCount) {
                state.copy(expression = "$base)", errorMessage = null, justEvaluated = false)
            } else {
                state
            }
        }
        recomputePreview(isScientific)
    }

    fun onToggleSign(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated && state.rawResult.isNotEmpty()) {
                state.rawResult
            } else {
                state.expression
            }
            if (base.isEmpty()) {
                return@updatePad state.copy(expression = "−", errorMessage = null, justEvaluated = false)
            }
            // Toggle sign of trailing number or wrap/unwrap (-...)
            if (base.startsWith("−") && base.drop(1).all { it.isDigit() || it == '.' }) {
                return@updatePad state.copy(expression = base.drop(1), justEvaluated = false)
            }
            if (base.all { it.isDigit() || it == '.' }) {
                return@updatePad state.copy(expression = "−$base", justEvaluated = false)
            }
            // Find start of last numeric token
            var i = base.length - 1
            while (i >= 0 && (base[i].isDigit() || base[i] == '.')) i--
            val prefix = base.substring(0, i + 1)
            val num = base.substring(i + 1)
            val next = when {
                num.isEmpty() -> {
                    if (prefix.endsWith("(−")) prefix.dropLast(2)
                    else "${base}(−"
                }
                prefix.endsWith("(−") -> prefix.dropLast(2) + num
                prefix.endsWith("−") && (prefix.length == 1 || prefix[prefix.length - 2] in setOf('(', '+', '−', '×', '÷', '^')) ->
                    prefix.dropLast(1) + num
                else -> "$prefix(−$num)"
            }
            state.copy(expression = next, errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onAbsoluteValue(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            val base = if (state.justEvaluated && state.rawResult.isNotEmpty()) {
                "abs(${state.rawResult})"
            } else {
                "${state.expression}abs("
            }
            state.copy(expression = base, errorMessage = null, justEvaluated = false)
        }
        recomputePreview(isScientific)
    }

    fun onBackspace(isScientific: Boolean) {
        updatePad(isScientific) { state ->
            if (state.justEvaluated) {
                return@updatePad CalcPadState()
            }
            val expr = state.expression
            if (expr.isEmpty()) return@updatePad state
            val suffixes = listOf("sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "asin(", "acos(", "atan(", "sqrt(", "sin(", "cos(", "tan(", "log(", "abs(", "ln(", "ʸ√(", "10^(", "√(")
            val matched = suffixes.firstOrNull { expr.endsWith(it) }
            val next = if (matched != null) expr.dropLast(matched.length) else expr.dropLast(1)
            state.copy(expression = next, errorMessage = null)
        }
        recomputePreview(isScientific)
    }

    fun onClear(isScientific: Boolean) {
        updatePad(isScientific) { CalcPadState() }
    }

    fun onEquals(isScientific: Boolean) {
        val state = if (isScientific) _scientificState.value else _standardState.value
        val expr = state.expression
        if (expr.isBlank()) return

        val settings = settingsFlow.value
        val eval = engine.evaluate(expr, angleUnit.value, settings)
        when (eval) {
            is EvalResult.Success -> {
                val formattedExpr = NumberFormatter.formatEditableExpression(expr, settings)
                updatePad(isScientific) {
                    it.copy(
                        finalResult = eval.formattedDisplay,
                        rawResult = eval.numericString,
                        livePreview = "",
                        errorMessage = null,
                        justEvaluated = true
                    )
                }
                if (formattedExpr != eval.formattedDisplay) {
                    viewModelScope.launch {
                        historyRepository.saveEntry(
                            mode = if (isScientific) HistoryMode.SCIENTIFIC else HistoryMode.STANDARD,
                            expressionDisplay = formattedExpr,
                            resultDisplay = eval.formattedDisplay,
                            rawResult = eval.numericString
                        )
                    }
                }
            }
            is EvalResult.Error -> {
                updatePad(isScientific) {
                    it.copy(errorMessage = eval.messageUkr, justEvaluated = false)
                }
            }
        }
    }

    fun loadResultIntoCalculator(rawResult: String, isScientific: Boolean) {
        val clean = rawResult.replace("−", "-")
        updatePad(isScientific) {
            CalcPadState(
                expression = clean.replace('-', '−'),
                livePreview = "",
                finalResult = "",
                rawResult = "",
                errorMessage = null,
                justEvaluated = false
            )
        }
        recomputePreview(isScientific)
    }

    private fun recomputePreview(isScientific: Boolean) {
        val state = if (isScientific) _scientificState.value else _standardState.value
        if (state.justEvaluated) return
        val expr = state.expression
        if (expr.isBlank()) {
            updatePad(isScientific) { it.copy(livePreview = "", errorMessage = null) }
            return
        }
        val hasOperatorOrFunc = expr.any {
            it in setOf('+', '−', '-', '×', '÷', '^', '%', '!', '²', '√', 'π', 'e', '(', ')') || it.isLetter()
        }
        if (!hasOperatorOrFunc) {
            updatePad(isScientific) { it.copy(livePreview = "", errorMessage = null) }
            return
        }
        val eval = engine.evaluate(expr, angleUnit.value, settingsFlow.value)
        when (eval) {
            is EvalResult.Success -> {
                updatePad(isScientific) { it.copy(livePreview = eval.formattedDisplay, errorMessage = null) }
            }
            is EvalResult.Error -> {
                // Don't flash error mid-typing; only show error on explicit '='
                updatePad(isScientific) { it.copy(livePreview = "") }
            }
        }
    }

    private fun updatePad(isScientific: Boolean, transform: (CalcPadState) -> CalcPadState) {
        if (isScientific) {
            _scientificState.update(transform)
        } else {
            _standardState.update(transform)
        }
    }
}
