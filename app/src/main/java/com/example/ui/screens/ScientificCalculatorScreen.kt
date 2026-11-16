package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.domain.math.NumberFormatter
import com.example.domain.model.AngleUnit
import com.example.domain.model.AppSettings
import com.example.ui.components.CalcKeyButton
import com.example.ui.components.CalcKeyRole
import com.example.ui.components.CompactPillKey
import com.example.ui.components.rememberFeedbackTrigger
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.viewmodel.CalcPadState
import com.example.ui.viewmodel.CalculatorViewModel

@Composable
fun ScientificCalculatorScreen(
    state: CalcPadState,
    angleUnit: AngleUnit,
    isSecondMode: Boolean,
    settings: AppSettings,
    viewModel: CalculatorViewModel,
    onShowCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onFeedback = rememberFeedbackTrigger(settings)
    val exprScroll = rememberScrollState()

    LaunchedEffect(state.expression) {
        exprScroll.animateScrollTo(exprScroll.maxValue)
    }

    val formattedExpression = remember(state.expression, settings) {
        if (state.expression.isEmpty()) "0"
        else NumberFormatter.formatEditableExpression(state.expression, settings)
    }

    val mainDisplayText = when {
        state.errorMessage != null -> state.errorMessage
        state.justEvaluated && state.finalResult.isNotEmpty() -> state.finalResult
        state.livePreview.isNotEmpty() -> "= ${state.livePreview}"
        else -> ""
    }

    val exprSize by animateFloatAsState(
        targetValue = when {
            state.justEvaluated -> 22f
            formattedExpression.length > 16 -> 26f
            else -> 38f
        },
        animationSpec = tween(120),
        label = "sci_expr_sz"
    )

    val resultSize by animateFloatAsState(
        targetValue = when {
            state.errorMessage != null -> 16f
            state.justEvaluated && mainDisplayText.length > 14 -> 30f
            state.justEvaluated -> 42f
            else -> 22f
        },
        animationSpec = tween(120),
        label = "sci_res_sz"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter
    ) {
        val compactHeight = maxHeight < 600.dp

        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = if (compactHeight) 8.dp else 14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Open Pure-Black Display Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 0.24f else 0.26f)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("sci_angle_toggle")
                                .clickable {
                                    onFeedback()
                                    viewModel.toggleAngleUnit()
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (angleUnit == AngleUnit.DEG) "DEG" else "RAD",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSecondMode) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .testTag("sci_2nd_toggle")
                                .clickable {
                                    onFeedback()
                                    viewModel.toggleSecondFunctions()
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "2nd",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSecondMode) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    val copyTarget = if (state.justEvaluated) state.finalResult else state.livePreview
                    AnimatedVisibility(
                        visible = copyTarget.isNotEmpty() && state.errorMessage == null,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(100))
                    ) {
                        IconButton(
                            onClick = {
                                copyToClipboard(context, copyTarget)
                                onShowCopied("Скопійовано: $copyTarget")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Копіювати результат",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(exprScroll),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = formattedExpression,
                            color = if (state.justEvaluated) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            fontFamily = CalcDisplayFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = exprSize.sp,
                            maxLines = 1,
                            modifier = Modifier.testTag("sci_expression_text")
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = mainDisplayText,
                        color = when {
                            state.errorMessage != null -> MaterialTheme.colorScheme.error
                            state.justEvaluated -> MaterialTheme.colorScheme.onBackground
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontFamily = CalcDisplayFontFamily,
                        fontWeight = if (state.justEvaluated) FontWeight.Medium else FontWeight.Normal,
                        fontSize = resultSize.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sci_result_text")
                    )
                }
            }

            // 3 compact scientific pill rows above the unified 5x4 circular numpad
            val pillHeight = if (compactHeight) 32.dp else 36.dp
            val pillGap = 6.dp
            val sinLabel = if (isSecondMode) "sin⁻¹" else "sin"
            val cosLabel = if (isSecondMode) "cos⁻¹" else "cos"
            val tanLabel = if (isSecondMode) "tan⁻¹" else "tan"

            Column(verticalArrangement = Arrangement.spacedBy(pillGap)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(pillHeight),
                    horizontalArrangement = Arrangement.spacedBy(pillGap)
                ) {
                    CompactPillKey(sinLabel, { viewModel.onFunction("$sinLabel(", true) }, Modifier.weight(1f), "sci_sin", isActive = isSecondMode, fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey(cosLabel, { viewModel.onFunction("$cosLabel(", true) }, Modifier.weight(1f), "sci_cos", isActive = isSecondMode, fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey(tanLabel, { viewModel.onFunction("$tanLabel(", true) }, Modifier.weight(1f), "sci_tan", isActive = isSecondMode, fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey("π", { viewModel.onConstant("π", true) }, Modifier.weight(1f), "sci_pi", "Число Пі", fontSize = 16.sp, onFeedback = onFeedback)
                    CompactPillKey("e", { viewModel.onConstant("e", true) }, Modifier.weight(1f), "sci_e", "Число Ейлера", fontSize = 15.sp, onFeedback = onFeedback)
                    CompactPillKey("n!", { viewModel.onFactorial(true) }, Modifier.weight(1f), "sci_fact", "Факторіал", fontSize = 13.sp, onFeedback = onFeedback)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(pillHeight),
                    horizontalArrangement = Arrangement.spacedBy(pillGap)
                ) {
                    CompactPillKey("log", { viewModel.onFunction("log(", true) }, Modifier.weight(1f), "sci_log", "Десятковий логарифм", fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey("ln", { viewModel.onFunction("ln(", true) }, Modifier.weight(1f), "sci_ln", "Натуральний логарифм", fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey("10ˣ", { viewModel.onTenPower(true) }, Modifier.weight(1f), "sci_10x", "Степінь 10", fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey("x²", { viewModel.onSquare(true) }, Modifier.weight(1f), "sci_sq", "Квадрат", fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey("xʸ", { viewModel.onOperator("^", true) }, Modifier.weight(1f), "sci_pow", "Степінь", fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey("|x|", { viewModel.onAbsoluteValue(true) }, Modifier.weight(1f), "sci_abs", "Модуль", fontSize = 13.sp, onFeedback = onFeedback)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(pillHeight),
                    horizontalArrangement = Arrangement.spacedBy(pillGap)
                ) {
                    CompactPillKey("√", { viewModel.onFunction("√(", true) }, Modifier.weight(1f), "sci_sqrt", "Квадратний корінь", fontSize = 16.sp, onFeedback = onFeedback)
                    CompactPillKey("ⁿ√", { viewModel.onNthRoot(true) }, Modifier.weight(1f), "sci_nroot", "Корінь n-го степеня", fontSize = 13.sp, onFeedback = onFeedback)
                    CompactPillKey("(", { viewModel.onOpenParenthesis(true) }, Modifier.weight(1f), "sci_lparen", "Відкрити дужку", fontSize = 14.sp, onFeedback = onFeedback)
                    CompactPillKey(")", { viewModel.onCloseParenthesis(true) }, Modifier.weight(1f), "sci_rparen", "Закрити дужку", fontSize = 14.sp, onFeedback = onFeedback)
                    CompactPillKey("+/−", { viewModel.onToggleSign(true) }, Modifier.weight(1f), "sci_sign", "Зміна знака", fontSize = 13.sp, onFeedback = onFeedback)
                }
            }

            Spacer(Modifier.height(if (compactHeight) 8.dp else 10.dp))

            // Unified 5x4 Circular Keypad
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 0.56f else 0.54f),
                contentAlignment = Alignment.Center
            ) {
                val gap = if (compactHeight) 8.dp else 11.dp
                val maxW = (maxWidth - gap * 3) / 4
                val maxH = (maxHeight - gap * 4) / 5
                val keySize = min(maxW, maxH).coerceAtMost(74.dp)
                val digitFont = (keySize.value * 0.36f).coerceIn(18f, 26f).sp
                val opFont = (keySize.value * 0.42f).coerceIn(20f, 30f).sp
                val acFont = (keySize.value * 0.28f).coerceIn(15f, 20f).sp

                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("AC", CalcKeyRole.CLEAR, { viewModel.onClear(true) }, Modifier.size(keySize), "sci_clear", "Очистити", acFont, onFeedback = onFeedback)
                        CalcKeyButton("%", CalcKeyRole.FUNCTION, { viewModel.onPercent(true) }, Modifier.size(keySize), "sci_pct", "Відсотки", digitFont, onFeedback = onFeedback)
                        CalcKeyButton("⌫", CalcKeyRole.FUNCTION, { viewModel.onBackspace(true) }, Modifier.size(keySize), "sci_backspace", "Стерти", icon = Icons.AutoMirrored.Outlined.Backspace, onFeedback = onFeedback)
                        CalcKeyButton("÷", CalcKeyRole.OPERATOR, { viewModel.onOperator("÷", true) }, Modifier.size(keySize), "sci_div", "Ділення", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("7", CalcKeyRole.DIGIT, { viewModel.onDigit("7", true) }, Modifier.size(keySize), "sci_7", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("8", CalcKeyRole.DIGIT, { viewModel.onDigit("8", true) }, Modifier.size(keySize), "sci_8", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("9", CalcKeyRole.DIGIT, { viewModel.onDigit("9", true) }, Modifier.size(keySize), "sci_9", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("×", CalcKeyRole.OPERATOR, { viewModel.onOperator("×", true) }, Modifier.size(keySize), "sci_mul", "Множення", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("4", CalcKeyRole.DIGIT, { viewModel.onDigit("4", true) }, Modifier.size(keySize), "sci_4", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("5", CalcKeyRole.DIGIT, { viewModel.onDigit("5", true) }, Modifier.size(keySize), "sci_5", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("6", CalcKeyRole.DIGIT, { viewModel.onDigit("6", true) }, Modifier.size(keySize), "sci_6", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("−", CalcKeyRole.OPERATOR, { viewModel.onOperator("−", true) }, Modifier.size(keySize), "sci_sub", "Віднімання", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("1", CalcKeyRole.DIGIT, { viewModel.onDigit("1", true) }, Modifier.size(keySize), "sci_1", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("2", CalcKeyRole.DIGIT, { viewModel.onDigit("2", true) }, Modifier.size(keySize), "sci_2", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("3", CalcKeyRole.DIGIT, { viewModel.onDigit("3", true) }, Modifier.size(keySize), "sci_3", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("+", CalcKeyRole.OPERATOR, { viewModel.onOperator("+", true) }, Modifier.size(keySize), "sci_add", "Додавання", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("00", CalcKeyRole.DIGIT, { viewModel.onDoubleZero(true) }, Modifier.size(keySize), "sci_00", "Два нулі", fontSize = (digitFont.value * 0.9f).sp, onFeedback = onFeedback)
                        CalcKeyButton("0", CalcKeyRole.DIGIT, { viewModel.onDigit("0", true) }, Modifier.size(keySize), "sci_0", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton(settings.decimalSeparator.symbol, CalcKeyRole.DIGIT, { viewModel.onDecimal(true) }, Modifier.size(keySize), "sci_decimal", "Десятковий роздільник", digitFont, onFeedback = onFeedback)
                        CalcKeyButton("=", CalcKeyRole.EQUALS, { viewModel.onEquals(true) }, Modifier.size(keySize), "sci_equals", "Обчислити", opFont, onFeedback = onFeedback)
                    }
                }
            }
        }
    }
}
