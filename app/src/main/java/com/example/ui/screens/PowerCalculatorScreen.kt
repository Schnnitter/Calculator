package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.domain.math.PowerOp
import com.example.domain.model.AppSettings
import com.example.ui.components.CalcKeyButton
import com.example.ui.components.CalcKeyRole
import com.example.ui.components.CompactPillKey
import com.example.ui.components.PowerFormulaEditorView
import com.example.ui.components.StaticSuperscriptPower
import com.example.ui.components.rememberFeedbackTrigger
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.LocalCalcColors
import com.example.ui.viewmodel.PowerUiState
import com.example.ui.viewmodel.PowerViewModel

@Composable
fun PowerCalculatorScreen(
    state: PowerUiState,
    settings: AppSettings,
    viewModel: PowerViewModel,
    onShowCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val calcColors = LocalCalcColors.current
    val context = LocalContext.current
    val onFeedback = rememberFeedbackTrigger(settings)
    val displayedEval = state.finalEvaluation ?: state.liveEvaluation

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter
    ) {
        val compactHeight = maxHeight < 580.dp

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(bottom = if (compactHeight) 10.dp else 18.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Open Borderless Powers Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 0.34f else 0.36f)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: law hint badge (when same-base rule applies) + copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lawHint = displayedEval?.simplifiedPower?.lawHintUkr
                    if (!lawHint.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = lawHint,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    AnimatedVisibility(
                        visible = displayedEval != null && state.errorMessage == null,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(100))
                    ) {
                        IconButton(
                            onClick = {
                                displayedEval?.let { eval ->
                                    copyToClipboard(context, eval.formattedResult)
                                    onShowCopied("Скопійовано: ${eval.formattedResult}")
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Копіювати результат",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Interactive raised superscript editor
                PowerFormulaEditorView(
                    nodes = state.nodes,
                    cursor = state.cursor,
                    decimalSeparator = settings.decimalSeparator,
                    onFocusSlot = viewModel::onFocusSlot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("power_formula_editor")
                )

                HorizontalDivider(
                    color = calcColors.subtleBorder.copy(alpha = 0.45f),
                    thickness = 0.8.dp
                )

                // Result row: simplified power (if same base) + numeric value + optional fraction hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("power_error_text")
                        )
                    } else if (displayedEval != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            displayedEval.simplifiedPower?.let { simplified ->
                                Text(
                                    text = "= ",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 20.sp,
                                    fontFamily = CalcDisplayFontFamily
                                )
                                StaticSuperscriptPower(
                                    base = simplified.baseText,
                                    exponent = simplified.exponentText,
                                    fontSize = 22.sp,
                                    baseColor = MaterialTheme.colorScheme.secondary,
                                    expColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.testTag("power_simplified_view")
                                )
                                Spacer(Modifier.width(12.dp))
                            }

                            Text(
                                text = "= ${displayedEval.formattedResult}",
                                color = if (state.justEvaluated) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = if (state.justEvaluated) 30.sp else 22.sp,
                                fontFamily = CalcDisplayFontFamily,
                                fontWeight = if (state.justEvaluated) FontWeight.Medium else FontWeight.Normal,
                                modifier = Modifier.testTag("power_result_text")
                            )

                            displayedEval.fractionHint?.let { frac ->
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "($frac)",
                                    color = calcColors.mutedText,
                                    fontSize = 15.sp,
                                    fontFamily = CalcDisplayFontFamily
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Множте та діліть степені",
                            color = calcColors.mutedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Pill Navigation & Quick Exponent Toolbar: ◄, ►, x², x³, +/−
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKeyButton(
                    label = "◄",
                    role = CalcKeyRole.FUNCTION,
                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    onClick = viewModel::onMoveLeft,
                    modifier = Modifier.weight(1f),
                    testTag = "pow_left",
                    contentDesc = "Курсор ліворуч",
                    shape = RoundedCornerShape(50),
                    onFeedback = onFeedback
                )
                CalcKeyButton(
                    label = "►",
                    role = CalcKeyRole.FUNCTION,
                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    onClick = viewModel::onMoveRight,
                    modifier = Modifier.weight(1f),
                    testTag = "pow_right",
                    contentDesc = "Курсор праворуч",
                    shape = RoundedCornerShape(50),
                    onFeedback = onFeedback
                )
                CompactPillKey("x²", { viewModel.onPowerKey("2") }, Modifier.weight(1f), "pow_sq", "У квадрат", fontSize = 15.sp, onFeedback = onFeedback)
                CompactPillKey("x³", { viewModel.onPowerKey("3") }, Modifier.weight(1f), "pow_cube", "У куб", fontSize = 15.sp, onFeedback = onFeedback)
                CompactPillKey("+/−", viewModel::onToggleSign, Modifier.weight(1f), "pow_sign", "Знак", fontSize = 14.sp, onFeedback = onFeedback)
            }

            Spacer(Modifier.height(if (compactHeight) 8.dp else 12.dp))

            // 5x4 Circular Powers Keypad
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 0.66f else 0.64f),
                contentAlignment = Alignment.Center
            ) {
                val gap = if (compactHeight) 10.dp else 14.dp
                val maxW = (maxWidth - gap * 3) / 4
                val maxH = (maxHeight - gap * 4) / 5
                val keySize = min(maxW, maxH).coerceAtMost(84.dp)
                val digitFont = (keySize.value * 0.36f).coerceIn(20f, 30f).sp
                val opFont = (keySize.value * 0.42f).coerceIn(22f, 34f).sp
                val acFont = (keySize.value * 0.28f).coerceIn(17f, 22f).sp

                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row 1: xⁿ, ( ), ⌫, ÷
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton(
                            label = "xⁿ",
                            role = CalcKeyRole.FUNCTION,
                            onClick = { viewModel.onPowerKey(null) },
                            modifier = Modifier.size(keySize),
                            testTag = "pow_template",
                            contentDesc = "Степінь x у степені n",
                            onFeedback = onFeedback,
                            customContent = { fg ->
                                StaticSuperscriptPower(
                                    base = "x",
                                    exponent = "n",
                                    fontSize = (digitFont.value * 0.9f).sp,
                                    baseColor = fg,
                                    expColor = fg
                                )
                            }
                        )
                        CalcKeyButton("( )", CalcKeyRole.FUNCTION, viewModel::onParentheses, Modifier.size(keySize), "pow_paren", "Дужки", (digitFont.value * 0.85f).sp, onFeedback = onFeedback)
                        CalcKeyButton("⌫", CalcKeyRole.FUNCTION, viewModel::onBackspace, Modifier.size(keySize), "pow_backspace", "Стерти", icon = Icons.AutoMirrored.Outlined.Backspace, onFeedback = onFeedback)
                        CalcKeyButton("÷", CalcKeyRole.OPERATOR, { viewModel.onOperator(PowerOp.DIV) }, Modifier.size(keySize), "pow_div", "Ділити степені", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("7", CalcKeyRole.DIGIT, { viewModel.onDigit("7") }, Modifier.size(keySize), "pow_7", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("8", CalcKeyRole.DIGIT, { viewModel.onDigit("8") }, Modifier.size(keySize), "pow_8", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("9", CalcKeyRole.DIGIT, { viewModel.onDigit("9") }, Modifier.size(keySize), "pow_9", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("×", CalcKeyRole.OPERATOR, { viewModel.onOperator(PowerOp.MUL) }, Modifier.size(keySize), "pow_mul", "Множити степені", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("4", CalcKeyRole.DIGIT, { viewModel.onDigit("4") }, Modifier.size(keySize), "pow_4", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("5", CalcKeyRole.DIGIT, { viewModel.onDigit("5") }, Modifier.size(keySize), "pow_5", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("6", CalcKeyRole.DIGIT, { viewModel.onDigit("6") }, Modifier.size(keySize), "pow_6", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("−", CalcKeyRole.OPERATOR, { viewModel.onOperator(PowerOp.SUB) }, Modifier.size(keySize), "pow_sub", "Віднімання", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("1", CalcKeyRole.DIGIT, { viewModel.onDigit("1") }, Modifier.size(keySize), "pow_1", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("2", CalcKeyRole.DIGIT, { viewModel.onDigit("2") }, Modifier.size(keySize), "pow_2", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("3", CalcKeyRole.DIGIT, { viewModel.onDigit("3") }, Modifier.size(keySize), "pow_3", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("+", CalcKeyRole.OPERATOR, { viewModel.onOperator(PowerOp.ADD) }, Modifier.size(keySize), "pow_add", "Додавання", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("AC", CalcKeyRole.CLEAR, viewModel::onClear, Modifier.size(keySize), "pow_clear", "Очистити", acFont, onFeedback = onFeedback)
                        CalcKeyButton("0", CalcKeyRole.DIGIT, { viewModel.onDigit("0") }, Modifier.size(keySize), "pow_0", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton(settings.decimalSeparator.symbol, CalcKeyRole.DIGIT, viewModel::onDecimalPoint, Modifier.size(keySize), "pow_decimal", "Десятковий роздільник", digitFont, onFeedback = onFeedback)
                        CalcKeyButton("=", CalcKeyRole.EQUALS, viewModel::onEvaluate, Modifier.size(keySize), "pow_equals", "Обчислити", opFont, onFeedback = onFeedback)
                    }
                }
            }
        }
    }
}
