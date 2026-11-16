package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.HistoryRepository
import com.example.domain.math.PowerCursor
import com.example.domain.math.PowerEvalException
import com.example.domain.math.PowerEvaluation
import com.example.domain.math.PowerEvaluator
import com.example.domain.math.PowerNode
import com.example.domain.math.PowerOp
import com.example.domain.math.PowerSerializer
import com.example.domain.math.PowerSlotKind
import com.example.domain.math.PowerSlotStep
import com.example.domain.model.AppSettings
import com.example.domain.model.HistoryMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PowerUiState(
    val nodes: List<PowerNode> = emptyList(),
    val cursor: PowerCursor = PowerCursor(),
    val liveEvaluation: PowerEvaluation? = null,
    val finalEvaluation: PowerEvaluation? = null,
    val errorMessage: String? = null,
    val justEvaluated: Boolean = false
)

class PowerViewModel(
    settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val settingsFlow: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _uiState = MutableStateFlow(PowerUiState())
    val uiState: StateFlow<PowerUiState> = _uiState.asStateFlow()

    private var rootNodes: MutableList<PowerNode> = mutableListOf()
    private var cursor: PowerCursor = PowerCursor()

    init {
        viewModelScope.launch {
            settingsFlow.collect {
                commitState(justEvaluated = _uiState.value.justEvaluated)
            }
        }
    }

    fun onDigit(digit: String) {
        if (_uiState.value.justEvaluated) {
            rootNodes = mutableListOf()
            cursor = PowerCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is PowerNode.NumberToken) {
            val nextText = if (prev.text == "0") digit else prev.text + digit
            list[idx - 1] = PowerNode.NumberToken(nextText)
        } else {
            list.add(idx, PowerNode.NumberToken(digit))
            cursor = cursor.copy(index = idx + 1)
        }
        commitState(justEvaluated = false)
    }

    fun onDecimalPoint() {
        if (_uiState.value.justEvaluated) {
            rootNodes = mutableListOf()
            cursor = PowerCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is PowerNode.NumberToken) {
            if (!prev.text.contains('.')) {
                list[idx - 1] = PowerNode.NumberToken(prev.text + ".")
            }
        } else {
            list.add(idx, PowerNode.NumberToken("0."))
            cursor = cursor.copy(index = idx + 1)
        }
        commitState(justEvaluated = false)
    }

    fun onPowerKey(fixedExponent: String? = null) {
        if (_uiState.value.justEvaluated && _uiState.value.finalEvaluation != null) {
            val prevRaw = _uiState.value.finalEvaluation!!.rawNumeric.replace('−', '-')
            rootNodes = mutableListOf(PowerNode.NumberToken(prevRaw))
            cursor = PowerCursor(index = 1)
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)

        if (prev is PowerNode.NumberToken || prev is PowerNode.ParenToken || prev is PowerNode.PowerToken) {
            val lifted = list.removeAt(idx - 1)
            val expNodes = if (fixedExponent != null) {
                mutableListOf<PowerNode>(PowerNode.NumberToken(fixedExponent))
            } else {
                mutableListOf()
            }
            val powNode = PowerNode.PowerToken(
                base = mutableListOf(lifted),
                exponent = expNodes
            )
            list.add(idx - 1, powNode)
            cursor = if (fixedExponent != null) {
                PowerCursor(path = cursor.path, index = idx)
            } else {
                PowerCursor(
                    path = cursor.path + PowerSlotStep(idx - 1, PowerSlotKind.EXPONENT),
                    index = 0
                )
            }
        } else {
            val expNodes = if (fixedExponent != null) {
                mutableListOf<PowerNode>(PowerNode.NumberToken(fixedExponent))
            } else {
                mutableListOf()
            }
            val powNode = PowerNode.PowerToken(base = mutableListOf(), exponent = expNodes)
            list.add(idx, powNode)
            cursor = PowerCursor(
                path = cursor.path + PowerSlotStep(idx, PowerSlotKind.BASE),
                index = 0
            )
        }
        commitState(justEvaluated = false)
    }

    fun onParentheses() {
        if (_uiState.value.justEvaluated) {
            rootNodes = mutableListOf()
            cursor = PowerCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val paren = PowerNode.ParenToken()
        list.add(idx, paren)
        cursor = PowerCursor(
            path = cursor.path + PowerSlotStep(idx, PowerSlotKind.PAREN_INNER),
            index = 0
        )
        commitState(justEvaluated = false)
    }

    fun onOperator(op: PowerOp) {
        if (_uiState.value.justEvaluated && _uiState.value.finalEvaluation != null) {
            val prevRaw = _uiState.value.finalEvaluation!!.rawNumeric.replace('−', '-')
            rootNodes = mutableListOf(PowerNode.NumberToken(prevRaw))
            cursor = PowerCursor(index = 1)
        }

        // Smart step-out: after typing an exponent like 2^3, tapping ×, ÷, +, − steps back to baseline!
        // Exception: allow leading minus at start of empty exponent (e.g. 2^-3).
        if (cursor.path.isNotEmpty()) {
            val lastStep = cursor.path.last()
            val currentList = resolveList(rootNodes, cursor.path)
            val keepNegativeStart = lastStep.slotKind == PowerSlotKind.EXPONENT &&
                currentList.isEmpty() && op == PowerOp.SUB
            if (lastStep.slotKind == PowerSlotKind.EXPONENT &&
                !keepNegativeStart &&
                cursor.index == currentList.size &&
                currentList.isNotEmpty() &&
                currentList.none { it is PowerNode.OperatorToken && (it.op == PowerOp.ADD || it.op == PowerOp.MUL || it.op == PowerOp.DIV) } &&
                !(currentList.size == 1 && currentList.first() is PowerNode.OperatorToken)
            ) {
                cursor = PowerCursor(
                    path = cursor.path.dropLast(1),
                    index = lastStep.nodeIndex + 1
                )
            }
        }

        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        when {
            idx == 0 && op != PowerOp.SUB -> return
            prev is PowerNode.OperatorToken -> {
                if (op == PowerOp.SUB && (prev.op == PowerOp.MUL || prev.op == PowerOp.DIV)) {
                    list.add(idx, PowerNode.OperatorToken(op))
                    cursor = cursor.copy(index = idx + 1)
                } else if (idx > 1 || op == PowerOp.SUB) {
                    list[idx - 1] = PowerNode.OperatorToken(op)
                } else {
                    list.removeAt(idx - 1)
                    cursor = cursor.copy(index = idx - 1)
                }
            }
            else -> {
                list.add(idx, PowerNode.OperatorToken(op))
                cursor = cursor.copy(index = idx + 1)
            }
        }
        commitState(justEvaluated = false)
    }

    fun onToggleSign() {
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is PowerNode.NumberToken) {
            val toggled = if (prev.text.startsWith("-")) prev.text.drop(1) else "-${prev.text}"
            list[idx - 1] = PowerNode.NumberToken(toggled)
        } else if (prev is PowerNode.OperatorToken && prev.op == PowerOp.SUB && idx == 1) {
            list.removeAt(0)
            cursor = cursor.copy(index = 0)
        } else if (idx == 0) {
            list.add(0, PowerNode.OperatorToken(PowerOp.SUB))
            cursor = cursor.copy(index = 1)
        }
        commitState(justEvaluated = false)
    }

    fun onBackspace() {
        if (_uiState.value.justEvaluated) {
            commitState(justEvaluated = false, clearFinal = true)
            return
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        if (idx > 0) {
            val prev = list[idx - 1]
            when (prev) {
                is PowerNode.NumberToken -> {
                    if (prev.text.length > 1 && prev.text != "-0") {
                        list[idx - 1] = PowerNode.NumberToken(prev.text.dropLast(1))
                    } else {
                        list.removeAt(idx - 1)
                        cursor = cursor.copy(index = idx - 1)
                    }
                }
                is PowerNode.OperatorToken -> {
                    list.removeAt(idx - 1)
                    cursor = cursor.copy(index = idx - 1)
                }
                is PowerNode.PowerToken -> {
                    if (prev.base.isEmpty() && prev.exponent.isEmpty()) {
                        list.removeAt(idx - 1)
                        cursor = cursor.copy(index = idx - 1)
                    } else if (prev.exponent.isNotEmpty()) {
                        cursor = PowerCursor(
                            path = cursor.path + PowerSlotStep(idx - 1, PowerSlotKind.EXPONENT),
                            index = prev.exponent.size
                        )
                    } else {
                        cursor = PowerCursor(
                            path = cursor.path + PowerSlotStep(idx - 1, PowerSlotKind.BASE),
                            index = prev.base.size
                        )
                    }
                }
                is PowerNode.ParenToken -> {
                    if (prev.inner.isEmpty()) {
                        list.removeAt(idx - 1)
                        cursor = cursor.copy(index = idx - 1)
                    } else {
                        cursor = PowerCursor(
                            path = cursor.path + PowerSlotStep(idx - 1, PowerSlotKind.PAREN_INNER),
                            index = prev.inner.size
                        )
                    }
                }
            }
        } else if (cursor.path.isNotEmpty()) {
            val step = cursor.path.last()
            val parentPath = cursor.path.dropLast(1)
            val parentList = resolveList(rootNodes, parentPath)
            val parentNode = parentList.getOrNull(step.nodeIndex)
            when (parentNode) {
                is PowerNode.PowerToken -> {
                    if (step.slotKind == PowerSlotKind.EXPONENT && parentNode.exponent.isEmpty()) {
                        // Unwrap power back into its base
                        parentList.removeAt(step.nodeIndex)
                        parentList.addAll(step.nodeIndex, parentNode.base)
                        cursor = PowerCursor(parentPath, step.nodeIndex + parentNode.base.size)
                    } else if (step.slotKind == PowerSlotKind.BASE && parentNode.base.isEmpty() && parentNode.exponent.isEmpty()) {
                        parentList.removeAt(step.nodeIndex)
                        cursor = PowerCursor(parentPath, step.nodeIndex)
                    } else {
                        onMoveLeft()
                        return
                    }
                }
                is PowerNode.ParenToken -> {
                    if (parentNode.inner.isEmpty()) {
                        parentList.removeAt(step.nodeIndex)
                        cursor = PowerCursor(parentPath, step.nodeIndex)
                    } else {
                        onMoveLeft()
                        return
                    }
                }
                else -> {}
            }
        }
        commitState(justEvaluated = false)
    }

    fun onMoveLeft() {
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        if (idx > 0) {
            val prev = list[idx - 1]
            cursor = when (prev) {
                is PowerNode.PowerToken -> PowerCursor(
                    path = cursor.path + PowerSlotStep(idx - 1, PowerSlotKind.EXPONENT),
                    index = prev.exponent.size
                )
                is PowerNode.ParenToken -> PowerCursor(
                    path = cursor.path + PowerSlotStep(idx - 1, PowerSlotKind.PAREN_INNER),
                    index = prev.inner.size
                )
                else -> cursor.copy(index = idx - 1)
            }
        } else if (cursor.path.isNotEmpty()) {
            val step = cursor.path.last()
            val parentPath = cursor.path.dropLast(1)
            val parentList = resolveList(rootNodes, parentPath)
            val parentNode = parentList.getOrNull(step.nodeIndex)
            cursor = if (parentNode is PowerNode.PowerToken && step.slotKind == PowerSlotKind.EXPONENT) {
                PowerCursor(parentPath + PowerSlotStep(step.nodeIndex, PowerSlotKind.BASE), parentNode.base.size)
            } else {
                PowerCursor(parentPath, step.nodeIndex)
            }
        }
        commitState(justEvaluated = false)
    }

    fun onMoveRight() {
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        if (idx < list.size) {
            val next = list[idx]
            cursor = when (next) {
                is PowerNode.PowerToken -> PowerCursor(
                    path = cursor.path + PowerSlotStep(idx, PowerSlotKind.BASE),
                    index = 0
                )
                is PowerNode.ParenToken -> PowerCursor(
                    path = cursor.path + PowerSlotStep(idx, PowerSlotKind.PAREN_INNER),
                    index = 0
                )
                else -> cursor.copy(index = idx + 1)
            }
        } else if (cursor.path.isNotEmpty()) {
            val step = cursor.path.last()
            val parentPath = cursor.path.dropLast(1)
            val parentList = resolveList(rootNodes, parentPath)
            val parentNode = parentList.getOrNull(step.nodeIndex)
            cursor = if (parentNode is PowerNode.PowerToken && step.slotKind == PowerSlotKind.BASE) {
                PowerCursor(parentPath + PowerSlotStep(step.nodeIndex, PowerSlotKind.EXPONENT), 0)
            } else {
                PowerCursor(parentPath, step.nodeIndex + 1)
            }
        }
        commitState(justEvaluated = false)
    }

    fun onFocusSlot(path: List<PowerSlotStep>) {
        val list = resolveList(rootNodes, path)
        cursor = PowerCursor(path = path, index = list.size)
        commitState(justEvaluated = false)
    }

    fun onClear() {
        rootNodes = mutableListOf()
        cursor = PowerCursor()
        _uiState.update {
            PowerUiState()
        }
    }

    fun onEvaluate() {
        if (rootNodes.isEmpty()) return
        try {
            val settings = settingsFlow.value
            val eval = PowerEvaluator.evaluate(rootNodes, settings)
            val exprPlain = PowerEvaluator.toPlainText(rootNodes)
            val json = PowerSerializer.toJson(rootNodes)
            _uiState.update {
                it.copy(
                    finalEvaluation = eval,
                    liveEvaluation = eval,
                    errorMessage = null,
                    justEvaluated = true
                )
            }
            viewModelScope.launch {
                historyRepository.saveEntry(
                    mode = HistoryMode.POWERS,
                    expressionDisplay = exprPlain,
                    resultDisplay = eval.formattedResult,
                    rawResult = eval.rawNumeric,
                    powerJson = json
                )
            }
        } catch (e: PowerEvalException) {
            _uiState.update { it.copy(errorMessage = e.messageUkr, justEvaluated = false) }
        }
    }

    fun loadFromRawOrJson(rawResult: String, powerJson: String?) {
        val loaded = PowerSerializer.fromJson(powerJson)
        if (loaded != null) {
            rootNodes = loaded
            cursor = PowerCursor(index = rootNodes.size)
            commitState(justEvaluated = false, clearFinal = true)
            return
        }
        rootNodes = mutableListOf(PowerNode.NumberToken(rawResult.replace("−", "-").trim()))
        cursor = PowerCursor(index = rootNodes.size)
        commitState(justEvaluated = false, clearFinal = true)
    }

    private fun commitState(justEvaluated: Boolean, clearFinal: Boolean = false) {
        val snapshot = PowerEvaluator.cloneNodes(rootNodes)
        val settings = settingsFlow.value
        val live = if (snapshot.isEmpty()) null else runCatching {
            PowerEvaluator.evaluate(snapshot, settings)
        }.getOrNull()
        _uiState.update { state ->
            state.copy(
                nodes = snapshot,
                cursor = cursor,
                liveEvaluation = live,
                finalEvaluation = if (clearFinal) null else if (!justEvaluated) null else state.finalEvaluation,
                errorMessage = null,
                justEvaluated = justEvaluated
            )
        }
    }

    private fun resolveList(
        root: MutableList<PowerNode>,
        path: List<PowerSlotStep>
    ): MutableList<PowerNode> {
        var current = root
        for (step in path) {
            val node = current.getOrNull(step.nodeIndex) ?: return current
            current = when {
                node is PowerNode.PowerToken && step.slotKind == PowerSlotKind.BASE -> node.base
                node is PowerNode.PowerToken && step.slotKind == PowerSlotKind.EXPONENT -> node.exponent
                node is PowerNode.ParenToken && step.slotKind == PowerSlotKind.PAREN_INNER -> node.inner
                else -> return current
            }
        }
        return current
    }
}
