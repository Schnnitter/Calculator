package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.math.ExactFraction
import com.example.domain.math.FractionCursor
import com.example.domain.math.FractionNode
import com.example.domain.math.MixedFraction
import com.example.domain.math.SlotKind
import com.example.domain.math.SlotStep
import com.example.domain.model.DecimalSeparator
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.LocalCalcColors
import java.math.BigInteger

@Composable
fun FractionFormulaEditorView(
    nodes: List<FractionNode>,
    cursor: FractionCursor,
    decimalSeparator: DecimalSeparator,
    onFocusSlot: (List<SlotStep>) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
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
                StaticTextbookFraction(
                    numerator = "1",
                    denominator = "2",
                    fontSize = 16.sp,
                    color = LocalCalcColors.current.mutedText
                )
                Text(
                    text = " + ",
                    color = LocalCalcColors.current.mutedText,
                    fontSize = 16.sp,
                    fontFamily = CalcDisplayFontFamily
                )
                StaticTextbookFraction(
                    numerator = "1",
                    denominator = "4",
                    fontSize = 16.sp,
                    color = LocalCalcColors.current.mutedText
                )
                Spacer(Modifier.width(8.dp))
                BlinkingCursor(alpha = cursorAlpha, heightDp = 28)
            }
        } else {
            NodeSequenceRow(
                nodes = nodes,
                currentPath = emptyList(),
                cursor = cursor,
                cursorAlpha = cursorAlpha,
                decimalSeparator = decimalSeparator,
                fontSize = 26.sp,
                depth = 0,
                onFocusSlot = onFocusSlot
            )
        }
    }
}

@Composable
private fun NodeSequenceRow(
    nodes: List<FractionNode>,
    currentPath: List<SlotStep>,
    cursor: FractionCursor,
    cursorAlpha: Float,
    decimalSeparator: DecimalSeparator,
    fontSize: TextUnit,
    depth: Int,
    onFocusSlot: (List<SlotStep>) -> Unit
) {
    val isCursorHere = cursor.path == currentPath
    val calcColors = LocalCalcColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (nodes.isEmpty()) {
            val slotBg = if (isCursorHere) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
            val slotBorder = if (isCursorHere) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            } else {
                calcColors.subtleBorder
            }
            Box(
                modifier = Modifier
                    .widthIn(min = if (depth == 0) 28.dp else 24.dp)
                    .height(if (depth == 0) 34.dp else 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(slotBg)
                    .border(1.dp, slotBorder, RoundedCornerShape(6.dp))
                    .clickable { onFocusSlot(currentPath) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCursorHere) {
                    BlinkingCursor(alpha = cursorAlpha, heightDp = if (depth == 0) 24 else 16)
                }
            }
        } else {
            if (isCursorHere && cursor.index == 0) {
                BlinkingCursor(alpha = cursorAlpha, heightDp = (fontSize.value * 1.05f).toInt())
            }
            nodes.forEachIndexed { idx, node ->
                when (node) {
                    is FractionNode.NumberToken -> {
                        val displayNum = node.text
                            .replace('.', decimalSeparator.symbol[0])
                            .replace('-', '−')
                        Text(
                            text = displayNum,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = fontSize,
                            fontFamily = CalcDisplayFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is FractionNode.OperatorToken -> {
                        Text(
                            text = " ${node.op.symbol} ",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = fontSize,
                            fontFamily = CalcDisplayFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    is FractionNode.FractionToken -> {
                        val childSize = (fontSize.value * 0.78f).coerceAtLeast(13f).sp
                        val numPath = currentPath + SlotStep(idx, SlotKind.FRAC_NUM)
                        val denPath = currentPath + SlotStep(idx, SlotKind.FRAC_DEN)
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .padding(horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFocusSlot(numPath) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NodeSequenceRow(
                                    nodes = node.numerator,
                                    currentPath = numPath,
                                    cursor = cursor,
                                    cursorAlpha = cursorAlpha,
                                    decimalSeparator = decimalSeparator,
                                    fontSize = childSize,
                                    depth = depth + 1,
                                    onFocusSlot = onFocusSlot
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(min = 26.dp)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFocusSlot(denPath) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NodeSequenceRow(
                                    nodes = node.denominator,
                                    currentPath = denPath,
                                    cursor = cursor,
                                    cursorAlpha = cursorAlpha,
                                    decimalSeparator = decimalSeparator,
                                    fontSize = childSize,
                                    depth = depth + 1,
                                    onFocusSlot = onFocusSlot
                                )
                            }
                        }
                    }
                    is FractionNode.MixedFractionToken -> {
                        val childSize = (fontSize.value * 0.76f).coerceAtLeast(13f).sp
                        val wholePath = currentPath + SlotStep(idx, SlotKind.MIXED_WHOLE)
                        val numPath = currentPath + SlotStep(idx, SlotKind.MIXED_NUM)
                        val denPath = currentPath + SlotStep(idx, SlotKind.MIXED_DEN)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable { onFocusSlot(wholePath) }
                                    .padding(horizontal = 2.dp)
                            ) {
                                NodeSequenceRow(
                                    nodes = node.whole,
                                    currentPath = wholePath,
                                    cursor = cursor,
                                    cursorAlpha = cursorAlpha,
                                    decimalSeparator = decimalSeparator,
                                    fontSize = fontSize,
                                    depth = depth + 1,
                                    onFocusSlot = onFocusSlot
                                )
                            }
                            Column(
                                modifier = Modifier.width(IntrinsicSize.Max),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFocusSlot(numPath) }
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    NodeSequenceRow(
                                        nodes = node.numerator,
                                        currentPath = numPath,
                                        cursor = cursor,
                                        cursorAlpha = cursorAlpha,
                                        decimalSeparator = decimalSeparator,
                                        fontSize = childSize,
                                        depth = depth + 1,
                                        onFocusSlot = onFocusSlot
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(min = 24.dp)
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFocusSlot(denPath) }
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    NodeSequenceRow(
                                        nodes = node.denominator,
                                        currentPath = denPath,
                                        cursor = cursor,
                                        cursorAlpha = cursorAlpha,
                                        decimalSeparator = decimalSeparator,
                                        fontSize = childSize,
                                        depth = depth + 1,
                                        onFocusSlot = onFocusSlot
                                    )
                                }
                            }
                        }
                    }
                    is FractionNode.ParenToken -> {
                        val innerPath = currentPath + SlotStep(idx, SlotKind.PAREN_INNER)
                        val parenColor = MaterialTheme.colorScheme.secondary
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BracketGlyph(isLeft = true, color = parenColor)
                            Box(
                                modifier = Modifier
                                    .clickable { onFocusSlot(innerPath) }
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NodeSequenceRow(
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
                            BracketGlyph(isLeft = false, color = parenColor)
                        }
                    }
                }
                if (isCursorHere && cursor.index == idx + 1) {
                    BlinkingCursor(alpha = cursorAlpha, heightDp = (fontSize.value * 1.05f).toInt())
                }
            }
        }
    }
}

@Composable
private fun BlinkingCursor(alpha: Float, heightDp: Int) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(heightDp.coerceIn(14, 34).dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

@Composable
private fun BracketGlyph(isLeft: Boolean, color: Color) {
    Canvas(
        modifier = Modifier
            .width(10.dp)
            .fillMaxHeight()
            .padding(vertical = 2.dp)
    ) {
        val w = size.width
        val h = size.height
        val path = Path()
        if (isLeft) {
            path.moveTo(w * 0.85f, 0f)
            path.quadraticTo(w * 0.1f, h * 0.5f, w * 0.85f, h)
        } else {
            path.moveTo(w * 0.15f, 0f)
            path.quadraticTo(w * 0.9f, h * 0.5f, w * 0.15f, h)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun StaticTextbookFraction(
    numerator: String,
    denominator: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = numerator,
            color = color,
            fontSize = fontSize,
            fontFamily = CalcDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 24.dp)
                .height(2.dp)
                .background(color)
        )
        Text(
            text = denominator,
            color = color,
            fontSize = fontSize,
            fontFamily = CalcDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
    }
}

@Composable
fun ExactFractionTextbookView(
    fraction: ExactFraction,
    showMixed: Boolean,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val reduced = fraction.reduced()
    if (reduced.isInteger) {
        val sign = if (reduced.numerator < BigInteger.ZERO) "−" else ""
        Text(
            text = "$sign${reduced.numerator.abs()}",
            color = color,
            fontSize = (fontSize.value * 1.25f).sp,
            fontFamily = CalcDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = modifier
        )
        return
    }

    if (showMixed) {
        val mixed: MixedFraction = reduced.toMixed()
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (mixed.isNegative) {
                Text(
                    text = "−",
                    color = color,
                    fontSize = (fontSize.value * 1.2f).sp,
                    fontFamily = CalcDisplayFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            if (mixed.whole > BigInteger.ZERO) {
                Text(
                    text = mixed.whole.toString(),
                    color = color,
                    fontSize = (fontSize.value * 1.25f).sp,
                    fontFamily = CalcDisplayFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            if (mixed.numerator > BigInteger.ZERO) {
                StaticTextbookFraction(
                    numerator = mixed.numerator.toString(),
                    denominator = mixed.denominator.toString(),
                    fontSize = fontSize,
                    color = color
                )
            }
        }
    } else {
        val isNeg = reduced.numerator < BigInteger.ZERO
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isNeg) {
                Text(
                    text = "−",
                    color = color,
                    fontSize = (fontSize.value * 1.2f).sp,
                    fontFamily = CalcDisplayFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            StaticTextbookFraction(
                numerator = reduced.numerator.abs().toString(),
                denominator = reduced.denominator.toString(),
                fontSize = fontSize,
                color = color
            )
        }
    }
}

@Composable
fun StaticFractionNodesView(
    nodes: List<FractionNode>,
    decimalSeparator: DecimalSeparator,
    fontSize: TextUnit,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (node in nodes) {
            when (node) {
                is FractionNode.NumberToken -> {
                    Text(
                        text = node.text.replace('.', decimalSeparator.symbol[0]).replace('-', '−'),
                        color = textColor,
                        fontSize = fontSize,
                        fontFamily = CalcDisplayFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
                is FractionNode.OperatorToken -> {
                    Text(
                        text = " ${node.op.symbol} ",
                        color = accentColor,
                        fontSize = fontSize,
                        fontFamily = CalcDisplayFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                is FractionNode.FractionToken -> {
                    val childSize = (fontSize.value * 0.8f).coerceAtLeast(11f).sp
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.padding(horizontal = 3.dp)) {
                            StaticFractionNodesView(
                                nodes = node.numerator,
                                decimalSeparator = decimalSeparator,
                                fontSize = childSize,
                                textColor = textColor,
                                accentColor = accentColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(min = 16.dp)
                                .height(1.5.dp)
                                .background(textColor)
                        )
                        Box(Modifier.padding(horizontal = 3.dp)) {
                            StaticFractionNodesView(
                                nodes = node.denominator,
                                decimalSeparator = decimalSeparator,
                                fontSize = childSize,
                                textColor = textColor,
                                accentColor = accentColor
                            )
                        }
                    }
                }
                is FractionNode.MixedFractionToken -> {
                    val childSize = (fontSize.value * 0.8f).coerceAtLeast(11f).sp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        StaticFractionNodesView(
                            nodes = node.whole,
                            decimalSeparator = decimalSeparator,
                            fontSize = fontSize,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                        Column(
                            modifier = Modifier.width(IntrinsicSize.Max),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StaticFractionNodesView(
                                nodes = node.numerator,
                                decimalSeparator = decimalSeparator,
                                fontSize = childSize,
                                textColor = textColor,
                                accentColor = accentColor
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(min = 14.dp)
                                    .height(1.5.dp)
                                    .background(textColor)
                            )
                            StaticFractionNodesView(
                                nodes = node.denominator,
                                decimalSeparator = decimalSeparator,
                                fontSize = childSize,
                                textColor = textColor,
                                accentColor = accentColor
                            )
                        }
                    }
                }
                is FractionNode.ParenToken -> {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketGlyph(isLeft = true, color = accentColor)
                        StaticFractionNodesView(
                            nodes = node.inner,
                            decimalSeparator = decimalSeparator,
                            fontSize = fontSize,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                        BracketGlyph(isLeft = false, color = accentColor)
                    }
                }
            }
        }
    }
}
