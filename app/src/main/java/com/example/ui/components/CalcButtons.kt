package com.example.ui.components

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppSettings
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalCalcColors

enum class CalcKeyRole {
    DIGIT,
    FUNCTION,
    OPERATOR,
    EQUALS,
    CLEAR,
    ACCENT_SECONDARY
}

@Composable
fun rememberFeedbackTrigger(settings: AppSettings): () -> Unit {
    val view: View = LocalView.current
    return remember(view, settings.hapticEnabled, settings.soundEnabled) {
        {
            if (settings.hapticEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            if (settings.soundEnabled) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
    }
}

@Composable
fun CalcKeyButton(
    label: String,
    role: CalcKeyRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    contentDesc: String = label,
    fontSize: TextUnit = 26.sp,
    icon: ImageVector? = null,
    isActiveToggle: Boolean = false,
    shape: Shape = CircleShape,
    onFeedback: () -> Unit = {},
    customContent: (@Composable (Color) -> Unit)? = null
) {
    val calcColors = LocalCalcColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Instant press-down and snappy spring release for 120fps tactile feel
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 65)
        } else {
            spring(
                dampingRatio = 0.68f,
                stiffness = Spring.StiffnessHigh
            )
        },
        label = "key_scale"
    )

    val (bgColor, textColor) = when {
        isActiveToggle -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        role == CalcKeyRole.DIGIT -> calcColors.digitKeyBg to calcColors.digitKeyContent
        role == CalcKeyRole.FUNCTION -> calcColors.functionKeyBg to calcColors.functionKeyContent
        role == CalcKeyRole.OPERATOR -> calcColors.operatorKeyBg to calcColors.operatorKeyContent
        role == CalcKeyRole.EQUALS -> calcColors.equalsKeyBg to calcColors.equalsKeyContent
        role == CalcKeyRole.CLEAR -> calcColors.clearKeyBg to calcColors.clearKeyContent
        role == CalcKeyRole.ACCENT_SECONDARY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> calcColors.digitKeyBg to calcColors.digitKeyContent
    }

    val rippleColor = if (role == CalcKeyRole.EQUALS) {
        Color.White.copy(alpha = 0.3f)
    } else {
        textColor.copy(alpha = 0.2f)
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bgColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = rippleColor),
                role = Role.Button,
                onClick = {
                    onFeedback()
                    onClick()
                }
            )
            .let { if (testTag != null) it.testTag(testTag) else it }
            .semantics { contentDescription = contentDesc }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            customContent != null -> customContent(textColor)
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            else -> Text(
                text = label,
                color = textColor,
                fontSize = fontSize,
                fontFamily = if (role == CalcKeyRole.DIGIT) CalcDisplayFontFamily else InterFontFamily,
                fontWeight = when (role) {
                    CalcKeyRole.EQUALS -> FontWeight.Medium
                    CalcKeyRole.OPERATOR -> FontWeight.Normal
                    CalcKeyRole.CLEAR, CalcKeyRole.FUNCTION -> FontWeight.Medium
                    else -> FontWeight.Normal
                },
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
fun CompactPillKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    contentDesc: String = label,
    isActive: Boolean = false,
    fontSize: TextUnit = 15.sp,
    onFeedback: () -> Unit = {}
) {
    CalcKeyButton(
        label = label,
        role = CalcKeyRole.FUNCTION,
        onClick = onClick,
        modifier = modifier,
        testTag = testTag,
        contentDesc = contentDesc,
        fontSize = fontSize,
        isActiveToggle = isActive,
        shape = RoundedCornerShape(50),
        onFeedback = onFeedback
    )
}

/**
 * Custom minimalist √π / e= icon matching the top-right scientific toggle in the reference screenshot.
 */
@Composable
fun ScientificToggleIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.65.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Top-left: √
        val sqrtPath = Path().apply {
            moveTo(w * 0.06f, h * 0.30f)
            lineTo(w * 0.16f, h * 0.44f)
            lineTo(w * 0.28f, h * 0.14f)
            lineTo(w * 0.45f, h * 0.14f)
        }
        drawPath(sqrtPath, color = tint, style = stroke)

        // Top-right: π
        drawLine(tint, Offset(w * 0.56f, h * 0.17f), Offset(w * 0.92f, h * 0.17f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.65f, h * 0.17f), Offset(w * 0.62f, h * 0.43f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.82f, h * 0.17f), Offset(w * 0.84f, h * 0.43f), strokeWidth = stroke.width, cap = StrokeCap.Round)

        // Bottom-left: e (simplified curved loop)
        val ePath = Path().apply {
            moveTo(w * 0.15f, h * 0.72f)
            lineTo(w * 0.40f, h * 0.72f)
            cubicTo(w * 0.40f, h * 0.56f, w * 0.14f, h * 0.56f, w * 0.14f, h * 0.72f)
            cubicTo(w * 0.14f, h * 0.88f, w * 0.34f, h * 0.90f, w * 0.40f, h * 0.83f)
        }
        drawPath(ePath, color = tint, style = stroke)

        // Bottom-right: =
        drawLine(tint, Offset(w * 0.58f, h * 0.66f), Offset(w * 0.90f, h * 0.66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.58f, h * 0.82f), Offset(w * 0.90f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

/**
 * Custom minimalist horizontal fraction bar icon for 1-tap switching to Fractions mode.
 */
@Composable
fun FractionModeIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()

        // Horizontal vinculum bar
        drawLine(
            color = tint,
            start = Offset(w * 0.14f, h * 0.50f),
            end = Offset(w * 0.86f, h * 0.50f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        // Top numerator box/dot
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.32f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = sw * 0.9f)
        )
        // Bottom denominator box/dot
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.32f, h * 0.64f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = sw * 0.9f)
        )
    }
}
