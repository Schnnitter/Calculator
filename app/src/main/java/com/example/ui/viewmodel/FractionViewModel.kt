package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.HistoryRepository
import com.example.domain.math.ExactFraction
import com.example.domain.math.FractionCursor
import com.example.domain.math.FractionEvalException
import com.example.domain.math.FractionEvaluation
import com.example.domain.math.FractionEvaluator
import com.example.domain.math.FractionNode
import com.example.domain.math.FractionOp
import com.example.domain.math.FractionSerializer
import com.example.domain.math.SlotKind
import com.example.domain.math.SlotStep
import com.example.domain.model.AppSettings
import com.example.domain.model.FractionDisplayPreference
import com.example.domain.model.HistoryMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FractionUiState(
    val nodes: List<FractionNode> = emptyList(),
    val cursor: FractionCursor = FractionCursor(),
    val liveEvaluation: FractionEvaluation? = null,
    val finalEvaluation: FractionEvaluation? = null,
    val showMixedPrimary: Boolean = true,
    val errorMessage: String? = null,
    val justEvaluated: Boolean = false
)

class FractionViewModel(
    settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val settingsFlow: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _uiState = MutableStateFlow(FractionUiState())
    val uiState: StateFlow<FractionUiState> = _uiState.asStateFlow()

    private var rootNodes: MutableList<FractionNode> = mutableListOf()
    private var cursor: FractionCursor = FractionCursor()

    init {
        viewModelScope.launch {
            settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        showMixedPrimary = settings.fractionDisplayPreference == FractionDisplayPreference.MIXED_FIRST
                    )
                }
            }
        }
    }

    fun onDigit(digit: String) {
        if (_uiState.value.justEvaluated) {
            rootNodes = mutableListOf()
            cursor = FractionCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is FractionNode.NumberToken) {
            val nextText = if (prev.text == "0") digit else prev.text + digit
            list[idx - 1] = FractionNode.NumberToken(nextText)
        } else {
            list.add(idx, FractionNode.NumberToken(digit))
            cursor = cursor.copy(index = idx + 1)
        }
        commitState(justEvaluated = false)
    }

    fun onDecimalPoint() {
        if (_uiState.value.justEvaluated) {
            rootNodes = mutableListOf()
            cursor = FractionCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is FractionNode.NumberToken) {
            if (!prev.text.contains('.')) {
                list[idx - 1] = FractionNode.NumberToken(prev.text + ".")
            }
        } else {
            list.add(idx, FractionNode.NumberToken("0."))
            cursor = cursor.copy(index = idx + 1)
        }
        commitState(justEvaluated = false)
    }

    fun onFractionBar() {
        if (_uiState.value.justEvaluated && _uiState.value.finalEvaluation != null) {
            // Start a fraction from the previous result if it's an integer, or clear
            rootNodes = mutableListOf()
            cursor = FractionCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is FractionNode.NumberToken || prev is FractionNode.ParenToken || prev is FractionNode.FractionToken) {
            val lifted = list.removeAt(idx - 1)
            val newFrac = FractionNode.FractionToken(
                numerator = mutableListOf(lifted),
                denominator = mutableListOf()
            )
            list.add(idx - 1, newFrac)
            cursor = FractionCursor(
                path = cursor.path + SlotStep(idx - 1, SlotKind.FRAC_DEN),
                index = 0
            )
        } else {
            val newFrac = FractionNode.FractionToken()
            list.add(idx, newFrac)
            cursor = FractionCursor(
                path = cursor.path + SlotStep(idx, SlotKind.FRAC_NUM),
                index = 0
            )
        }
        commitState(justEvaluated = false)
    }

    fun onMixedFraction() {
        if (_uiState.value.justEvaluated) {
            rootNodes = mutableListOf()
            cursor = FractionCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is FractionNode.NumberToken) {
            val wholeNode = list.removeAt(idx - 1)
            val mixed = FractionNode.MixedFractionToken(
                whole = mutableListOf(wholeNode),
                numerator = mutableListOf(),
                denominator = mutableListOf()
            )
            list.add(idx - 1, mixed)
            cursor = FractionCursor(
                path = cursor.path + SlotStep(idx - 1, SlotKind.MIXED_NUM),
                index = 0
            )
        } else {
            val mixed = FractionNode.MixedFractionToken()
            list.add(idx, mixed)
            cursor = FractionCursor(
                path = cursor.path + SlotStep(idx, SlotKind.MIXED_WHOLE),
                index = 0
            )
        }
        commitState(justEvaluated = false)
    }

    fun onParentheses() {
        if (_uiState.value.justEvaluated) {
            rootNodes = mutableListOf()
            cursor = FractionCursor()
        }
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val paren = FractionNode.ParenToken()
        list.add(idx, paren)
        cursor = FractionCursor(
            path = cursor.path + SlotStep(idx, SlotKind.PAREN_INNER),
            index = 0
        )
        commitState(justEvaluated = false)
    }

    fun onOperator(op: FractionOp) {
        if (_uiState.value.justEvaluated && _uiState.value.finalEvaluation != null) {
            val prevEval = _uiState.value.finalEvaluation!!.reduced
            rootNodes = fractionToNodes(prevEval)
            cursor = FractionCursor(index = rootNodes.size)
        }

        // Smart step-out: when user finishes typing a simple denominator and taps +, −, ×, ÷,
        // automatically step out to the parent level so `1/2 + 1/4` works naturally in one flow!
        if (cursor.path.isNotEmpty()) {
            val lastStep = cursor.path.last()
            val currentList = resolveList(rootNodes, cursor.path)
            if ((lastStep.slotKind == SlotKind.FRAC_DEN || lastStep.slotKind == SlotKind.MIXED_DEN) &&
                cursor.index == currentList.size &&
                currentList.isNotEmpty() &&
                currentList.none { it is FractionNode.OperatorToken }
            ) {
                cursor = FractionCursor(
                    path = cursor.path.dropLast(1),
                    index = lastStep.nodeIndex + 1
                )
            }
        }

        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        when {
            idx == 0 && op != FractionOp.SUB -> return
            prev is FractionNode.OperatorToken -> {
                if (op == FractionOp.SUB && (prev.op == FractionOp.MUL || prev.op == FractionOp.DIV)) {
                    list.add(idx, FractionNode.OperatorToken(op))
                    cursor = cursor.copy(index = idx + 1)
                } else if (idx > 1 || op == FractionOp.SUB) {
                    list[idx - 1] = FractionNode.OperatorToken(op)
                } else {
                    list.removeAt(idx - 1)
                    cursor = cursor.copy(index = idx - 1)
                }
            }
            else -> {
                list.add(idx, FractionNode.OperatorToken(op))
                cursor = cursor.copy(index = idx + 1)
            }
        }
        commitState(justEvaluated = false)
    }

    fun onToggleSign() {
        val list = resolveList(rootNodes, cursor.path)
        val idx = cursor.index.coerceIn(0, list.size)
        val prev = list.getOrNull(idx - 1)
        if (prev is FractionNode.NumberToken) {
            val toggled = if (prev.text.startsWith("-")) prev.text.drop(1) else "-${prev.text}"
            list[idx - 1] = FractionNode.NumberToken(toggled)
        } else if (prev is FractionNode.OperatorToken && prev.op == FractionOp.SUB && idx == 1) {
            list.removeAt(0)
            cursor = cursor.copy(index = 0)
        } else if (idx == 0) {
            list.add(0, FractionNode.OperatorToken(FractionOp.SUB))
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
                is FractionNode.NumberToken -> {
                    if (prev.text.length > 1 && prev.text != "-0") {
                        list[idx - 1] = FractionNode.NumberToken(prev.text.dropLast(1))
                    } else {
                        list.removeAt(idx - 1)
                        cursor = cursor.copy(index = idx - 1)
                    }
                }
                is FractionNode.OperatorToken -> {
                    list.removeAt(idx - 1)
                    cursor = cursor.copy(index = idx - 1)
                }
                is FractionNode.FractionToken -> {
                    if (prev.numerator.isEmpty() && prev.denominator.isEmpty()) {
                        list.removeAt(idx - 1)
                        cursor = cursor.copy(index = idx - 1)
                    } else if (prev.denominator.isNotEmpty()) {
                        cursor = FractionCursor(
                            path = cursor.path + SlotStep(idx - 1, SlotKind.FRAC_DEN),
                            index = prev.denominator.size
                        )
                    } else {
                        cursor = FractionCursor(
                            path = cursor.path + SlotStep(idx - 1, SlotKind.FRAC_NUM),
                            index = prev.numerator.size
                        )
                    }
                }
                is FractionNode.MixedFractionToken -> {
                    if (prev.whole.isEmpty() && prev.numerator.isEmpty() && prev.denominator.isEmpty()) {
                        list.removeAt(idx - 1)
                        cursor = cursor.copy(index = idx - 1)
                    } else if (prev.denominator.isNotEmpty()) {
                        cursor = FractionCursor(
                            path = cursor.path + SlotStep(idx - 1, SlotKind.MIXED_DEN),
                            index = prev.denominator.size
                        )
                    } else if (prev.numerator.isNotEmpty()) {
                        cursor = FractionCursor(
                            path = cursor.path + SlotStep(idx - 1, SlotKind.MIXED_NUM),
                            index = prev.numerator.size
                        )
                    } else {
                        cursor = FractionCursor(
                            path = cursor.path + SlotStep(idx - 1, SlotKind.MIXED_WHOLE),
                            index = prev.whole.size
                        )
                    }
                }
                is FractionNode.ParenToken -> {
                    if (prev.inner.isEmpty()) {
                        list.removeAt(idx - 1)
                        cursor = cursor.copy(index = idx - 1)
                    } else {
                        cursor = FractionCursor(
                            path = cursor.path + SlotStep(idx - 1, SlotKind.PAREN_INNER),
                            index = prev.inner.size
                        )
                    }
                }
            }
        } else if (cursor.path.isNotEmpty()) {
            // Cursor is at index 0 inside a sub-slot
            val step = cursor.path.last()
            val parentPath = cursor.path.dropLast(1)
            val parentList = resolveList(rootNodes, parentPath)
            val parentNode = parentList.getOrNull(step.nodeIndex)
            when (parentNode) {
                is FractionNode.FractionToken -> {
                    if (step.slotKind == SlotKind.FRAC_DEN && parentNode.denominator.isEmpty()) {
                        // Unwrap fraction back into its numerator
                        parentList.removeAt(step.nodeIndex)
                        parentList.addAll(step.nodeIndex, parentNode.numerator)
                        cursor = FractionCursor(parentPath, step.nodeIndex + parentNode.numerator.size)
                    } else if (step.slotKind == SlotKind.FRAC_NUM && parentNode.numerator.isEmpty() && parentNode.denominator.isEmpty()) {
                        parentList.removeAt(step.nodeIndex)
                        cursor = FractionCursor(parentPath, step.nodeIndex)
                    } else {
                        onMoveLeft()
                        return
                    }
                }
                is FractionNode.MixedFractionToken -> {
                    if (parentNode.whole.isEmpty() && parentNode.numerator.isEmpty() && parentNode.denominator.isEmpty()) {
                        parentList.removeAt(step.nodeIndex)
                        cursor = FractionCursor(parentPath, step.nodeIndex)
                    } else if (step.slotKind == SlotKind.MIXED_NUM && parentNode.numerator.isEmpty() && parentNode.denominator.isEmpty()) {
                        parentList.removeAt(step.nodeIndex)
                        parentList.addAll(step.nodeIndex, parentNode.whole)
                        cursor = FractionCursor(parentPath, step.nodeIndex + parentNode.whole.size)
                    } else {
                        onMoveLeft()
                        return
                    }
                }
                is FractionNode.ParenToken -> {
                    if (parentNode.inner.isEmpty()) {
                        parentList.removeAt(step.nodeIndex)
                        cursor = FractionCursor(parentPath, step.nodeIndex)
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
                is FractionNode.FractionToken -> FractionCursor(
                    path = cursor.path + SlotStep(idx - 1, SlotKind.FRAC_DEN),
                    index = prev.denominator.size
                )
                is FractionNode.MixedFractionToken -> FractionCursor(
                    path = cursor.path + SlotStep(idx - 1, SlotKind.MIXED_DEN),
                    index = prev.denominator.size
                )
                is FractionNode.ParenToken -> FractionCursor(
                    path = cursor.path + SlotStep(idx - 1, SlotKind.PAREN_INNER),
                    index = prev.inner.size
                )
                else -> cursor.copy(index = idx - 1)
            }
        } else if (cursor.path.isNotEmpty()) {
            val step = cursor.path.last()
            val parentPath = cursor.path.dropLast(1)
            val parentList = resolveList(rootNodes, parentPath)
            val parentNode = parentList.getOrNull(step.nodeIndex)
            cursor = when {
                parentNode is FractionNode.FractionToken && step.slotKind == SlotKind.FRAC_DEN ->
                    FractionCursor(parentPath + SlotStep(step.nodeIndex, SlotKind.FRAC_NUM), parentNode.numerator.size)
                parentNode is FractionNode.MixedFractionToken && step.slotKind == SlotKind.MIXED_DEN ->
                    FractionCursor(parentPath + SlotStep(step.nodeIndex, SlotKind.MIXED_NUM), parentNode.numerator.size)
                parentNode is FractionNode.MixedFractionToken && step.slotKind == SlotKind.MIXED_NUM ->
                    FractionCursor(parentPath + SlotStep(step.nodeIndex, SlotKind.MIXED_WHOLE), parentNode.whole.size)
                else -> FractionCursor(parentPath, step.nodeIndex)
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
                is FractionNode.FractionToken -> FractionCursor(
                    path = cursor.path + SlotStep(idx, SlotKind.FRAC_NUM),
                    index = 0
                )
                is FractionNode.MixedFractionToken -> FractionCursor(
                    path = cursor.path + SlotStep(idx, SlotKind.MIXED_WHOLE),
                    index = 0
                )
                is FractionNode.ParenToken -> FractionCursor(
                    path = cursor.path + SlotStep(idx, SlotKind.PAREN_INNER),
                    index = 0
                )
                else -> cursor.copy(index = idx + 1)
            }
        } else if (cursor.path.isNotEmpty()) {
            val step = cursor.path.last()
            val parentPath = cursor.path.dropLast(1)
            val parentList = resolveList(rootNodes, parentPath)
            val parentNode = parentList.getOrNull(step.nodeIndex)
            cursor = when {
                parentNode is FractionNode.FractionToken && step.slotKind == SlotKind.FRAC_NUM ->
                    FractionCursor(parentPath + SlotStep(step.nodeIndex, SlotKind.FRAC_DEN), 0)
                parentNode is FractionNode.MixedFractionToken && step.slotKind == SlotKind.MIXED_WHOLE ->
                    FractionCursor(parentPath + SlotStep(step.nodeIndex, SlotKind.MIXED_NUM), 0)
                parentNode is FractionNode.MixedFractionToken && step.slotKind == SlotKind.MIXED_NUM ->
                    FractionCursor(parentPath + SlotStep(step.nodeIndex, SlotKind.MIXED_DEN), 0)
                else -> FractionCursor(parentPath, step.nodeIndex + 1)
            }
        }
        commitState(justEvaluated = false)
    }

    fun onFocusSlot(path: List<SlotStep>) {
        val list = resolveList(rootNodes, path)
        cursor = FractionCursor(path = path, index = list.size)
        commitState(justEvaluated = false)
    }

    fun onClear() {
        rootNodes = mutableListOf()
        cursor = FractionCursor()
        _uiState.update {
            it.copy(
                nodes = emptyList(),
                cursor = cursor,
                liveEvaluation = null,
                finalEvaluation = null,
                errorMessage = null,
                justEvaluated = false
            )
        }
    }

    fun onToggleResultFormat() {
        _uiState.update { it.copy(showMixedPrimary = !it.showMixedPrimary) }
    }

    fun onConvertDecimalOrReduce() {
        if (rootNodes.isEmpty()) return
        try {
            val eval = FractionEvaluator.evaluate(rootNodes)
            val useMixed = _uiState.value.showMixedPrimary
            rootNodes = if (useMixed && eval.mixed.whole != java.math.BigInteger.ZERO && eval.mixed.numerator != java.math.BigInteger.ZERO) {
                mixedToNodes(eval)
            } else {
                fractionToNodes(eval.reduced)
            }
            cursor = FractionCursor(index = rootNodes.size)
            commitState(justEvaluated = false)
            _uiState.update { it.copy(finalEvaluation = eval, errorMessage = null) }
        } catch (e: FractionEvalException) {
            _uiState.update { it.copy(errorMessage = e.messageUkr) }
        }
    }

    fun onEvaluate() {
        if (rootNodes.isEmpty()) return
        try {
            val eval = FractionEvaluator.evaluate(rootNodes)
            val exprPlain = FractionEvaluator.toPlainText(rootNodes)
            val resultPlain = if (_uiState.value.showMixedPrimary && eval.reduced.isImproper) {
                "${eval.reduced.toMixedPlainCanonical()} (${eval.reduced.toPlainCanonical()})"
            } else {
                eval.reduced.toPlainCanonical()
            }
            val json = FractionSerializer.toJson(rootNodes)
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
                    mode = HistoryMode.FRACTIONS,
                    expressionDisplay = exprPlain,
                    resultDisplay = resultPlain,
                    rawResult = eval.reduced.toPlainCanonical(),
                    fractionJson = json
                )
            }
        } catch (e: FractionEvalException) {
            _uiState.update { it.copy(errorMessage = e.messageUkr, justEvaluated = false) }
        }
    }

    fun loadFromRawOrJson(rawResult: String, fractionJson: String?) {
        val loaded = FractionSerializer.fromJson(fractionJson)
        if (loaded != null) {
            rootNodes = loaded
            cursor = FractionCursor(index = rootNodes.size)
            commitState(justEvaluated = false, clearFinal = true)
            return
        }
        // Parse plain fraction e.g. "3/4" or decimal "0.75"
        val slashParts = rawResult.replace("−", "-").split("/")
        rootNodes = if (slashParts.size == 2) {
            mutableListOf(
                FractionNode.FractionToken(
                    numerator = mutableListOf(FractionNode.NumberToken(slashParts[0].trim())),
                    denominator = mutableListOf(FractionNode.NumberToken(slashParts[1].trim()))
                )
            )
        } else {
            mutableListOf(FractionNode.NumberToken(rawResult.replace("−", "-").trim()))
        }
        cursor = FractionCursor(index = rootNodes.size)
        commitState(justEvaluated = false, clearFinal = true)
    }

    private fun commitState(justEvaluated: Boolean, clearFinal: Boolean = false) {
        val snapshot = FractionEvaluator.cloneNodes(rootNodes)
        val live = if (snapshot.isEmpty()) {
            null
        } else {
            runCatching { FractionEvaluator.evaluate(snapshot) }.getOrNull()
        }
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

    private fun fractionToNodes(f: ExactFraction): MutableList<FractionNode> {
        val r = f.reduced()
        return if (r.isInteger) {
            mutableListOf(FractionNode.NumberToken(r.numerator.toString()))
        } else {
            val isNeg = r.numerator < java.math.BigInteger.ZERO
            val frac = FractionNode.FractionToken(
                numerator = mutableListOf(FractionNode.NumberToken(r.numerator.abs().toString())),
                denominator = mutableListOf(FractionNode.NumberToken(r.denominator.toString()))
            )
            if (isNeg) mutableListOf(FractionNode.OperatorToken(FractionOp.SUB), frac)
            else mutableListOf(frac)
        }
    }

    private fun mixedToNodes(eval: FractionEvaluation): MutableList<FractionNode> {
        val m = eval.mixed
        val mixedNode = FractionNode.MixedFractionToken(
            whole = mutableListOf(FractionNode.NumberToken(m.whole.toString())),
            numerator = mutableListOf(FractionNode.NumberToken(m.numerator.toString())),
            denominator = mutableListOf(FractionNode.NumberToken(m.denominator.toString()))
        )
        return if (m.isNegative) mutableListOf(FractionNode.OperatorToken(FractionOp.SUB), mixedNode)
        else mutableListOf(mixedNode)
    }

    private fun resolveList(
        root: MutableList<FractionNode>,
        path: List<SlotStep>
    ): MutableList<FractionNode> {
        var current = root
        for (step in path) {
            val node = current.getOrNull(step.nodeIndex) ?: return current
            current = when {
                node is FractionNode.FractionToken && step.slotKind == SlotKind.FRAC_NUM -> node.numerator
                node is FractionNode.FractionToken && step.slotKind == SlotKind.FRAC_DEN -> node.denominator
                node is FractionNode.MixedFractionToken && step.slotKind == SlotKind.MIXED_WHOLE -> node.whole
                node is FractionNode.MixedFractionToken && step.slotKind == SlotKind.MIXED_NUM -> node.numerator
                node is FractionNode.MixedFractionToken && step.slotKind == SlotKind.MIXED_DEN -> node.denominator
                node is FractionNode.ParenToken && step.slotKind == SlotKind.PAREN_INNER -> node.inner
                else -> return current
            }
        }
        return current
    }
}
