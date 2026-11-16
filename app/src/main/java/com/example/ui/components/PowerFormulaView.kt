package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.math.PowerCursor
import com.example.domain.math.PowerNode
import com.example.domain.math.PowerSlotKind
import com.example.domain.math.PowerSlotStep
import com.example.domain.model.DecimalSeparator
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.LocalCalcColors

@Composable
fun PowerFormulaEditorView(
    nodes: List<PowerNode>,
    cursor: PowerCursor,
    decimalSeparator: DecimalSeparator,
    onFocusSlot: (List<PowerSlotStep>) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "power_cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "power_cursor_alpha"
    )

    val scrollState = rememberScrollState()
    LaunchedEffect(nodes.size, cursor) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onFocusSlot(emptyList()) }
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (nodes.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Напр. ",
                    color = LocalCalcColors.current.mutedText,
                    fontSize = 15.sp,
                    fontFamily = CalcDisplayFontFamily
                )
                StaticSuperscriptPower(
                    base = "2",
                    exponent = "3",
                    fontSize = 18.sp,
                    baseColor = LocalCalcColors.current.mutedText,
                    expColor = LocalCalcColors.current.mutedText
                )
                Text(
                    text = " × ",
                    color = LocalCalcColors.current.mutedText,
                    fontSize = 18.sp,
                    fontFamily = CalcDisplayFontFamily
                )
                StaticSuperscriptPower(
                    base = "2",
                    exponent = "4",
                    fontSize = 18.sp,
                    baseColor = LocalCalcColors.current.mutedText,
                    expColor = LocalCalcColors.current.mutedText
                )
                Spacer(Modifier.width(8.dp))
                PowerBlinkingCursor(alpha = cursorAlpha, heightDp = 28)
            }
        } else {
            PowerNodeSequenceRow(
                nodes = nodes,
                currentPath = emptyList(),
                cursor = cursor,
                cursorAlpha = cursorAlpha,
                decimalSeparator = decimalSeparator,
                fontSize = 30.sp,
                depth = 0,
                onFocusSlot = onFocusSlot
            )
        }
    }
}

@Composable
private fun PowerNodeSequenceRow(
    nodes: List<PowerNode>,
    currentPath: List<PowerSlotStep>,
    cursor: PowerCursor,
    cursorAlpha: Float,
    decimalSeparator: DecimalSeparator,
    fontSize: TextUnit,
    depth: Int,
    onFocusSlot: (List<PowerSlotStep>) -> Unit
) {
    val isCursorHere = cursor.path == currentPath
    val calcColors = LocalCalcColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (nodes.isEmpty()) {
            val slotBg = if (isCursorHere) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
            val slotBorder = if (isCursorHere) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            } else {
                calcColors.subtleBorder
            }
            Box(
                modifier = Modifier
                    .widthIn(min = if (depth == 0) 28.dp else 20.dp)
                    .height(if (depth == 0) 34.dp else 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(slotBg)
                    .border(1.dp, slotBorder, RoundedCornerShape(6.dp))
                    .clickable { onFocusSlot(currentPath) }
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCursorHere) {
                    PowerBlinkingCursor(alpha = cursorAlpha, heightDp = if (depth == 0) 24 else 15)
                }
            }
        } else {
            if (isCursorHere && cursor.index == 0) {
                PowerBlinkingCursor(alpha = cursorAlpha, heightDp = (fontSize.value * 1.0f).toInt())
            }
            nodes.forEachIndexed { idx, node ->
                when (node) {
                    is PowerNode.NumberToken -> {
                        val displayNum = node.text
                            .replace('.', decimalSeparator.symbol[0])
                            .replace('-', '−')
                        Text(
                            text = displayNum,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = fontSize,
                            fontFamily = CalcDisplayFontFamily,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    is PowerNode.OperatorToken -> {
                        Text(
                            text = " ${node.op.symbol} ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = fontSize,
                            fontFamily = CalcDisplayFontFamily,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    is PowerNode.PowerToken -> {
                        val basePath = currentPath + PowerSlotStep(idx, PowerSlotKind.BASE)
                        val expPath = currentPath + PowerSlotStep(idx, PowerSlotKind.EXPONENT)
                        val expFontSize = (fontSize.value * 0.62f).coerceAtLeast(13f).sp
                        val expOffsetY = -(fontSize.value * 0.38f).coerceAtLeast(8f)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier.clickable { onFocusSlot(basePath) }
                            ) {
                                PowerNodeSequenceRow(
                                    nodes = node.base,
                                    currentPath = basePath,
                                    cursor = cursor,
                                    cursorAlpha = cursorAlpha,
                                    decimalSeparator = decimalSeparator,
                                    fontSize = fontSize,
                                    depth = depth,
                                    onFocusSlot = onFocusSlot
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .offset(y = expOffsetY.dp)
                                    .clickable { onFocusSlot(expPath) }
                                    .padding(start = 1.dp, end = 2.dp)
                            ) {
                                PowerNodeSequenceRow(
                                    nodes = node.exponent,
                                    currentPath = expPath,
                                    cursor = cursor,
                                    cursorAlpha = cursorAlpha,
                                    decimalSeparator = decimalSeparator,
                                    fontSize = expFontSize,
                                    depth = depth + 1,
                                    onFocusSlot = onFocusSlot
                                )
                            }
                        }
                    }
                    is PowerNode.ParenToken -> {
                        val innerPath = currentPath + PowerSlotStep(idx, PowerSlotKind.PAREN_INNER)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "(",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = fontSize,
                                fontFamily = CalcDisplayFontFamily
                            )
                            Box(
                                modifier = Modifier
                                    .clickable { onFocusSlot(innerPath) }
                                    .padding(horizontal = 1.dp)
                            ) {
                                PowerNodeSequenceRow(
                                    nodes = node.inner,
                                    currentPath = innerPath,
                                    cursor = cursor,
                                    cursorAlpha = cursorAlpha,
                                    decimalSeparator = decimalSeparator,
                                    fontSize = fontSize,
                                    depth = depth,
                                    onFocusSlot = onFocusSlot
                                )
                            }
                            Text(
                                text = ")",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = fontSize,
                                fontFamily = CalcDisplayFontFamily
                            )
                        }
                    }
                }
                if (isCursorHere && cursor.index == idx + 1) {
                    PowerBlinkingCursor(alpha = cursorAlpha, heightDp = (fontSize.value * 1.0f).toInt())
                }
            }
        }
    }
}

@Composable
private fun PowerBlinkingCursor(alpha: Float, heightDp: Int) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(heightDp.coerceIn(13, 36).dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
    )
}

@Composable
fun StaticSuperscriptPower(
    base: String,
    exponent: String,
    fontSize: TextUnit,
    baseColor: Color,
    expColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = base,
            color = baseColor,
            fontSize = fontSize,
            fontFamily = CalcDisplayFontFamily,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = exponent,
            color = expColor,
            fontSize = (fontSize.value * 0.62f).coerceAtLeast(11f).sp,
            fontFamily = CalcDisplayFontFamily,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.offset(y = (-fontSize.value * 0.18f).dp)
        )
    }
}

@Composable
fun StaticPowerNodesView(
    nodes: List<PowerNode>,
    decimalSeparator: DecimalSeparator,
    fontSize: TextUnit,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (node in nodes) {
            when (node) {
                is PowerNode.NumberToken -> {
                    Text(
                        text = node.text.replace('.', decimalSeparator.symbol[0]).replace('-', '−'),
                        color = textColor,
                        fontSize = fontSize,
                        fontFamily = CalcDisplayFontFamily
                    )
                }
                is PowerNode.OperatorToken -> {
                    Text(
                        text = " ${node.op.symbol} ",
                        color = accentColor,
                        fontSize = fontSize,
                        fontFamily = CalcDisplayFontFamily
                    )
                }
                is PowerNode.PowerToken -> {
                    val expSize = (fontSize.value * 0.65f).coerceAtLeast(10f).sp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StaticPowerNodesView(
                            nodes = node.base,
                            decimalSeparator = decimalSeparator,
                            fontSize = fontSize,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                        Box(modifier = Modifier.offset(y = (-fontSize.value * 0.34f).dp)) {
                            StaticPowerNodesView(
                                nodes = node.exponent,
                                decimalSeparator = decimalSeparator,
                                fontSize = expSize,
                                textColor = textColor,
                                accentColor = accentColor
                            )
                        }
                    }
                }
                is PowerNode.ParenToken -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("(", color = accentColor, fontSize = fontSize, fontFamily = CalcDisplayFontFamily)
                        StaticPowerNodesView(
                            nodes = node.inner,
                            decimalSeparator = decimalSeparator,
                            fontSize = fontSize,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                        Text(")", color = accentColor, fontSize = fontSize, fontFamily = CalcDisplayFontFamily)
                    }
                }
            }
        }
    }
}
