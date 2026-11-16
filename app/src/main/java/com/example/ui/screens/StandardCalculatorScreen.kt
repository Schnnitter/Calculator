package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
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
import com.example.domain.model.AppSettings
import com.example.domain.model.HistoryItem
import com.example.ui.components.CalcKeyButton
import com.example.ui.components.CalcKeyRole
import com.example.ui.components.CompactPillKey
import com.example.ui.components.rememberFeedbackTrigger
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.viewmodel.CalcPadState
import com.example.ui.viewmodel.CalculatorViewModel

@Composable
fun StandardCalculatorScreen(
    state: CalcPadState,
    settings: AppSettings,
    latestHistory: HistoryItem?,
    viewModel: CalculatorViewModel,
    onShowCopied: (String) -> Unit,
    onReuseLatestHistory: (HistoryItem) -> Unit,
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

    val exprSizeTarget = when {
        state.justEvaluated -> 26f
        formattedExpression.length > 15 -> 32f
        formattedExpression.length > 10 -> 42f
        else -> 56f
    }
    val exprSizeSp by animateFloatAsState(
        targetValue = exprSizeTarget,
        animationSpec = tween(durationMillis = 120),
        label = "expr_size"
    )

    val resultSizeTarget = when {
        state.errorMessage != null -> 18f
        state.justEvaluated && mainDisplayText.length > 13 -> 36f
        state.justEvaluated -> 54f
        else -> 26f
    }
    val resultSizeSp by animateFloatAsState(
        targetValue = resultSizeTarget,
        animationSpec = tween(durationMillis = 120),
        label = "res_size"
    )

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
            // Borderless Open Display Canvas (matches screenshot aesthetic)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 0.30f else 0.34f)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Subtle history chip + copy action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (latestHistory != null) {
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .clickable { onReuseLatestHistory(latestHistory) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Останнє обчислення",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${latestHistory.expressionDisplay} = ${latestHistory.resultDisplay}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
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

                // Right-aligned floating numbers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
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
                            fontSize = exprSizeSp.sp,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.testTag("standard_expression_text")
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = mainDisplayText,
                        color = when {
                            state.errorMessage != null -> MaterialTheme.colorScheme.error
                            state.justEvaluated -> MaterialTheme.colorScheme.onBackground
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontFamily = CalcDisplayFontFamily,
                        fontWeight = if (state.justEvaluated) FontWeight.Medium else FontWeight.Normal,
                        fontSize = resultSizeSp.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("standard_result_text")
                    )
                }
            }

            // Quiet quick-operation pills: √, x², xʸ, ( ), +/−
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactPillKey("√", { viewModel.onFunction("√(", false) }, Modifier.weight(1f), "std_sqrt", "Корінь", onFeedback = onFeedback)
                CompactPillKey("x²", { viewModel.onSquare(false) }, Modifier.weight(1f), "std_square", "Квадрат", onFeedback = onFeedback)
                CompactPillKey("xʸ", { viewModel.onOperator("^", false) }, Modifier.weight(1f), "std_power", "Степінь", onFeedback = onFeedback)
                CompactPillKey("( )", { viewModel.onSmartParenthesis(false) }, Modifier.weight(1f), "std_paren", "Дужки", onFeedback = onFeedback)
                CompactPillKey("+/−", { viewModel.onToggleSign(false) }, Modifier.weight(1f), "std_sign", "Знак", onFeedback = onFeedback)
            }

            Spacer(Modifier.height(if (compactHeight) 8.dp else 12.dp))

            // 5x4 Circular Keypad (1:1 match to screenshot layout)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 0.70f else 0.66f),
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
                    // Row 1: AC, %, ⌫, ÷
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("AC", CalcKeyRole.CLEAR, { viewModel.onClear(false) }, Modifier.size(keySize), "std_clear", "Очистити", acFont, onFeedback = onFeedback)
                        CalcKeyButton("%", CalcKeyRole.FUNCTION, { viewModel.onPercent(false) }, Modifier.size(keySize), "std_percent", "Відсотки", digitFont, onFeedback = onFeedback)
                        CalcKeyButton("⌫", CalcKeyRole.FUNCTION, { viewModel.onBackspace(false) }, Modifier.size(keySize), "std_backspace", "Стерти", icon = Icons.AutoMirrored.Outlined.Backspace, onFeedback = onFeedback)
                        CalcKeyButton("÷", CalcKeyRole.OPERATOR, { viewModel.onOperator("÷", false) }, Modifier.size(keySize), "std_div", "Ділення", opFont, onFeedback = onFeedback)
                    }
                    // Row 2: 7, 8, 9, ×
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("7", CalcKeyRole.DIGIT, { viewModel.onDigit("7", false) }, Modifier.size(keySize), "std_7", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("8", CalcKeyRole.DIGIT, { viewModel.onDigit("8", false) }, Modifier.size(keySize), "std_8", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("9", CalcKeyRole.DIGIT, { viewModel.onDigit("9", false) }, Modifier.size(keySize), "std_9", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("×", CalcKeyRole.OPERATOR, { viewModel.onOperator("×", false) }, Modifier.size(keySize), "std_mul", "Множення", opFont, onFeedback = onFeedback)
                    }
                    // Row 3: 4, 5, 6, −
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("4", CalcKeyRole.DIGIT, { viewModel.onDigit("4", false) }, Modifier.size(keySize), "std_4", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("5", CalcKeyRole.DIGIT, { viewModel.onDigit("5", false) }, Modifier.size(keySize), "std_5", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("6", CalcKeyRole.DIGIT, { viewModel.onDigit("6", false) }, Modifier.size(keySize), "std_6", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("−", CalcKeyRole.OPERATOR, { viewModel.onOperator("−", false) }, Modifier.size(keySize), "std_sub", "Віднімання", opFont, onFeedback = onFeedback)
                    }
                    // Row 4: 1, 2, 3, +
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("1", CalcKeyRole.DIGIT, { viewModel.onDigit("1", false) }, Modifier.size(keySize), "std_1", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("2", CalcKeyRole.DIGIT, { viewModel.onDigit("2", false) }, Modifier.size(keySize), "std_2", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("3", CalcKeyRole.DIGIT, { viewModel.onDigit("3", false) }, Modifier.size(keySize), "std_3", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("+", CalcKeyRole.OPERATOR, { viewModel.onOperator("+", false) }, Modifier.size(keySize), "std_add", "Додавання", opFont, onFeedback = onFeedback)
                    }
                    // Row 5: 00, 0, ,, =
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("00", CalcKeyRole.DIGIT, { viewModel.onDoubleZero(false) }, Modifier.size(keySize), "std_00", "Два нулі", fontSize = (digitFont.value * 0.9f).sp, onFeedback = onFeedback)
                        CalcKeyButton("0", CalcKeyRole.DIGIT, { viewModel.onDigit("0", false) }, Modifier.size(keySize), "std_0", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton(
                            label = settings.decimalSeparator.symbol,
                            role = CalcKeyRole.DIGIT,
                            onClick = { viewModel.onDecimal(false) },
                            modifier = Modifier.size(keySize),
                            testTag = "std_decimal",
                            contentDesc = "Десятковий роздільник",
                            fontSize = digitFont,
                            onFeedback = onFeedback
                        )
                        CalcKeyButton("=", CalcKeyRole.EQUALS, { viewModel.onEquals(false) }, Modifier.size(keySize), "std_equals", "Обчислити", opFont, onFeedback = onFeedback)
                    }
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Калькулятор", text))
}
