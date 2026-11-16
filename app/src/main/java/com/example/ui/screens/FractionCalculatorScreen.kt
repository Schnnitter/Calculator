package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.domain.math.FractionOp
import com.example.domain.model.AppSettings
import com.example.ui.components.CalcKeyButton
import com.example.ui.components.CalcKeyRole
import com.example.ui.components.CompactPillKey
import com.example.ui.components.ExactFractionTextbookView
import com.example.ui.components.FractionFormulaEditorView
import com.example.ui.components.rememberFeedbackTrigger
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.LocalCalcColors
import com.example.ui.viewmodel.FractionUiState
import com.example.ui.viewmodel.FractionViewModel
import java.math.BigInteger

@Composable
fun FractionCalculatorScreen(
    state: FractionUiState,
    settings: AppSettings,
    viewModel: FractionViewModel,
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
            // Open Borderless Textbook Fraction Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 0.34f else 0.36f)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("frac_toggle_format")
                            .clickable {
                                onFeedback()
                                viewModel.onToggleResultFormat()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "Перемкнути формат дробу",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (state.showMixedPrimary) "Мішаний вигляд" else "Неправильний дріб",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = displayedEval != null && state.errorMessage == null,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(100))
                    ) {
                        IconButton(
                            onClick = {
                                displayedEval?.let { eval ->
                                    val txt = if (state.showMixedPrimary && eval.reduced.isImproper) {
                                        eval.reduced.toMixedPlainCanonical()
                                    } else {
                                        eval.reduced.toPlainCanonical()
                                    }
                                    copyToClipboard(context, txt)
                                    onShowCopied("Скопійовано: $txt")
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Копіювати дріб",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                FractionFormulaEditorView(
                    nodes = state.nodes,
                    cursor = state.cursor,
                    decimalSeparator = settings.decimalSeparator,
                    onFocusSlot = viewModel::onFocusSlot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fraction_formula_editor")
                )

                HorizontalDivider(
                    color = calcColors.subtleBorder.copy(alpha = 0.45f),
                    thickness = 0.8.dp
                )

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
                            modifier = Modifier.testTag("fraction_error_text")
                        )
                    } else if (displayedEval != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "= ",
                                color = if (state.justEvaluated) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 24.sp,
                                fontFamily = CalcDisplayFontFamily,
                                fontWeight = FontWeight.Medium
                            )

                            ExactFractionTextbookView(
                                fraction = displayedEval.reduced,
                                showMixed = state.showMixedPrimary,
                                fontSize = if (state.justEvaluated) 22.sp else 18.sp,
                                color = if (state.justEvaluated) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.testTag("fraction_result_view")
                            )

                            if (displayedEval.reduced.isImproper) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "= ",
                                    color = calcColors.mutedText,
                                    fontSize = 15.sp
                                )
                                ExactFractionTextbookView(
                                    fraction = displayedEval.reduced,
                                    showMixed = !state.showMixedPrimary,
                                    fontSize = 14.sp,
                                    color = calcColors.mutedText
                                )
                            }

                            if (displayedEval.reduced.denominator > BigInteger.ONE) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "≈ ${displayedEval.reduced.toDecimalString(settings)}",
                                    color = calcColors.mutedText,
                                    fontSize = 14.sp,
                                    fontFamily = CalcDisplayFontFamily
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Введіть дріб",
                            color = calcColors.mutedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Sleek pill toolbar: ◄, ►, Скоротити, ( ), +/−
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
                    testTag = "frac_left",
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
                    testTag = "frac_right",
                    contentDesc = "Курсор праворуч",
                    shape = RoundedCornerShape(50),
                    onFeedback = onFeedback
                )
                CompactPillKey(
                    label = "Скоротити",
                    onClick = viewModel::onConvertDecimalOrReduce,
                    modifier = Modifier.weight(1.9f),
                    testTag = "frac_reduce",
                    contentDesc = "Скоротити або перетворити десятковий дріб",
                    fontSize = 13.sp,
                    onFeedback = onFeedback
                )
                CompactPillKey("( )", viewModel::onParentheses, Modifier.weight(1f), "frac_paren", "Дужки", fontSize = 14.sp, onFeedback = onFeedback)
                CompactPillKey("+/−", viewModel::onToggleSign, Modifier.weight(1f), "frac_sign", "Знак", fontSize = 14.sp, onFeedback = onFeedback)
            }

            Spacer(Modifier.height(if (compactHeight) 8.dp else 12.dp))

            // 5x4 Circular Fraction Keypad
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
                    // Row 1: a/b, a b/c, ⌫, ÷
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton(
                            label = "a/b",
                            role = CalcKeyRole.FUNCTION,
                            onClick = viewModel::onFractionBar,
                            modifier = Modifier.size(keySize),
                            testTag = "frac_template",
                            contentDesc = "Звичайний дріб",
                            onFeedback = onFeedback,
                            customContent = { fg -> FractionGlyphContent(isMixed = false, fg = fg) }
                        )
                        CalcKeyButton(
                            label = "a b/c",
                            role = CalcKeyRole.FUNCTION,
                            onClick = viewModel::onMixedFraction,
                            modifier = Modifier.size(keySize),
                            testTag = "frac_mixed_template",
                            contentDesc = "Мішаний дріб",
                            onFeedback = onFeedback,
                            customContent = { fg -> FractionGlyphContent(isMixed = true, fg = fg) }
                        )
                        CalcKeyButton("⌫", CalcKeyRole.FUNCTION, viewModel::onBackspace, Modifier.size(keySize), "frac_backspace", "Стерти", icon = Icons.AutoMirrored.Outlined.Backspace, onFeedback = onFeedback)
                        CalcKeyButton("÷", CalcKeyRole.OPERATOR, { viewModel.onOperator(FractionOp.DIV) }, Modifier.size(keySize), "frac_div", "Ділення", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("7", CalcKeyRole.DIGIT, { viewModel.onDigit("7") }, Modifier.size(keySize), "frac_7", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("8", CalcKeyRole.DIGIT, { viewModel.onDigit("8") }, Modifier.size(keySize), "frac_8", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("9", CalcKeyRole.DIGIT, { viewModel.onDigit("9") }, Modifier.size(keySize), "frac_9", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("×", CalcKeyRole.OPERATOR, { viewModel.onOperator(FractionOp.MUL) }, Modifier.size(keySize), "frac_mul", "Множення", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("4", CalcKeyRole.DIGIT, { viewModel.onDigit("4") }, Modifier.size(keySize), "frac_4", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("5", CalcKeyRole.DIGIT, { viewModel.onDigit("5") }, Modifier.size(keySize), "frac_5", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("6", CalcKeyRole.DIGIT, { viewModel.onDigit("6") }, Modifier.size(keySize), "frac_6", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("−", CalcKeyRole.OPERATOR, { viewModel.onOperator(FractionOp.SUB) }, Modifier.size(keySize), "frac_sub", "Віднімання", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("1", CalcKeyRole.DIGIT, { viewModel.onDigit("1") }, Modifier.size(keySize), "frac_1", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("2", CalcKeyRole.DIGIT, { viewModel.onDigit("2") }, Modifier.size(keySize), "frac_2", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("3", CalcKeyRole.DIGIT, { viewModel.onDigit("3") }, Modifier.size(keySize), "frac_3", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("+", CalcKeyRole.OPERATOR, { viewModel.onOperator(FractionOp.ADD) }, Modifier.size(keySize), "frac_add", "Додавання", opFont, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("AC", CalcKeyRole.CLEAR, viewModel::onClear, Modifier.size(keySize), "frac_clear", "Очистити", acFont, onFeedback = onFeedback)
                        CalcKeyButton("0", CalcKeyRole.DIGIT, { viewModel.onDigit("0") }, Modifier.size(keySize), "frac_0", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton(settings.decimalSeparator.symbol, CalcKeyRole.DIGIT, viewModel::onDecimalPoint, Modifier.size(keySize), "frac_decimal", "Десятковий роздільник", digitFont, onFeedback = onFeedback)
                        CalcKeyButton("=", CalcKeyRole.EQUALS, viewModel::onEvaluate, Modifier.size(keySize), "frac_equals", "Обчислити", opFont, onFeedback = onFeedback)
                    }
                }
            }
        }
    }
}

@Composable
private fun FractionGlyphContent(isMixed: Boolean, fg: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isMixed) {
            Text(
                text = "a",
                color = fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = CalcDisplayFontFamily
            )
        }
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isMixed) "b" else "a",
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 18.dp)
                    .height(1.6.dp)
                    .background(fg)
            )
            Text(
                text = if (isMixed) "c" else "b",
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
