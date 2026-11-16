package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = SoftSilverAccent,
    onPrimary = Color(0xFF000000),
    primaryContainer = DarkContainerPrimary,
    onPrimaryContainer = DarkOnContainerPrimary,
    secondary = SoftCyanAccent,
    onSecondary = Color(0xFF041E2B),
    secondaryContainer = DarkContainerSecondary,
    onSecondaryContainer = DarkOnContainerSecondary,
    tertiary = SoftAmberAccent,
    background = OledBlack,
    onBackground = OledTextPrimary,
    surface = OledSurface,
    onSurface = OledTextPrimary,
    surfaceVariant = OledSurfaceElevated,
    onSurfaceVariant = OledTextSecondary,
    outline = OledOutline,
    outlineVariant = OledDigitCircle,
    error = CoralErrorDark,
    onError = Color(0xFF2B0708),
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentPrimary,
    onPrimary = Color.White,
    primaryContainer = LightContainerPrimary,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightAccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightContainerSecondary,
    onSecondaryContainer = LightOnContainerSecondary,
    tertiary = SoftAmberAccent,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightFuncCircle,
    error = CoralErrorLight,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralErrorLight
)

@Immutable
data class CalcExtendedColors(
    val digitKeyBg: Color,
    val digitKeyContent: Color,
    val functionKeyBg: Color,
    val functionKeyContent: Color,
    val operatorKeyBg: Color,
    val operatorKeyContent: Color,
    val equalsKeyBg: Color,
    val equalsKeyContent: Color,
    val clearKeyBg: Color,
    val clearKeyContent: Color,
    val displayCardBg: Color,
    val subtleBorder: Color,
    val mutedText: Color,
    val fractionAccent: Color
)

private val DarkCalcExtended = CalcExtendedColors(
    digitKeyBg = OledDigitCircle,
    digitKeyContent = OledTextPrimary,
    functionKeyBg = OledFuncCircle,
    functionKeyContent = OledTextPrimary,
    operatorKeyBg = OledFuncCircle,
    operatorKeyContent = OledTextPrimary,
    equalsKeyBg = OledEqualsCircle,
    equalsKeyContent = Color(0xFFFFFFFF),
    clearKeyBg = OledFuncCircle,
    clearKeyContent = OledTextPrimary,
    displayCardBg = OledSurface,
    subtleBorder = OledOutline,
    mutedText = OledTextMuted,
    fractionAccent = SoftCyanAccent
)

private val LightCalcExtended = CalcExtendedColors(
    digitKeyBg = LightDigitCircle,
    digitKeyContent = LightTextPrimary,
    functionKeyBg = LightFuncCircle,
    functionKeyContent = LightTextPrimary,
    operatorKeyBg = LightFuncCircle,
    operatorKeyContent = LightTextPrimary,
    equalsKeyBg = LightEqualsCircle,
    equalsKeyContent = Color.White,
    clearKeyBg = LightFuncCircle,
    clearKeyContent = LightTextPrimary,
    displayCardBg = LightSurface,
    subtleBorder = LightOutline,
    mutedText = LightTextMuted,
    fractionAccent = LightAccentSecondary
)

val LocalCalcColors = staticCompositionLocalOf { DarkCalcExtended }

val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CalculatorTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (useDark) DarkCalcExtended else LightCalcExtended

    CompositionLocalProvider(LocalCalcColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
