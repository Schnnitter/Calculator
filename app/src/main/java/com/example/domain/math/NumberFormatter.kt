package com.example.domain.math

import com.example.domain.model.AppSettings
import com.example.domain.model.DecimalSeparator
import com.example.domain.model.GroupingStyle
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

object NumberFormatter {

    fun formatBigDecimal(
        value: BigDecimal,
        settings: AppSettings,
        forceFixedScale: Int? = null
    ): String {
        val rounded = if (forceFixedScale != null) {
            value.setScale(forceFixedScale, RoundingMode.HALF_UP)
        } else {
            val absVal = value.abs()
            if (absVal != BigDecimal.ZERO &&
                (absVal >= BigDecimal("1E15") || absVal < BigDecimal("1E-10"))
            ) {
                return formatScientific(value, settings)
            }
            val scaled = value.setScale(settings.decimalPrecision, RoundingMode.HALF_UP)
            scaled.stripTrailingZerosSafe()
        }

        val plain = rounded.toPlainString()
        return applySeparators(plain, settings)
    }

    fun formatDouble(value: Double, settings: AppSettings): String {
        if (value.isNaN() || value.isInfinite()) return "Помилка"
        val snapped = snapFloatingNoise(value)
        if (snapped == 0.0) return "0"
        val absVal = abs(snapped)
        if (absVal >= 1e15 || absVal < 1e-10) {
            val bd = BigDecimal.valueOf(snapped)
            return formatScientific(bd, settings)
        }
        val bd = BigDecimal.valueOf(snapped)
        return formatBigDecimal(bd, settings)
    }

    fun formatPhysicsDouble(value: Double, settings: AppSettings): String {
        if (value.isNaN() || value.isInfinite()) return "Помилка"
        if (value == 0.0) return "0"
        val absVal = abs(value)
        if (absVal >= 1e12 || absVal < 1e-6) {
            return formatScientific(BigDecimal.valueOf(value), settings)
        }
        val rounded = BigDecimal.valueOf(value)
            .setScale(settings.decimalPrecision, RoundingMode.HALF_UP)
            .stripTrailingZerosSafe()
        if (rounded.compareTo(BigDecimal.ZERO) == 0) {
            return formatScientific(BigDecimal.valueOf(value), settings)
        }
        return applySeparators(rounded.toPlainString(), settings)
    }

    fun formatCurrencyAmount(value: BigDecimal, settings: AppSettings): String {
        val absVal = value.abs()
        val scale = when {
            absVal == BigDecimal.ZERO -> 2
            absVal < BigDecimal("0.01") -> 6
            absVal < BigDecimal("1") -> 4
            else -> 2
        }
        val rounded = value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        val plain = rounded.toPlainString()
        return applySeparators(plain, settings)
    }

    fun formatRate(value: BigDecimal, settings: AppSettings): String {
        val absVal = value.abs()
        val scale = when {
            absVal < BigDecimal("0.01") -> 6
            absVal < BigDecimal("1") -> 4
            else -> 4
        }
        val rounded = value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        return applySeparators(rounded.toPlainString(), settings)
    }

    fun formatEditableExpression(expr: String, settings: AppSettings): String {
        val sep = settings.decimalSeparator.symbol
        val normalized = expr.replace('.', sep[0]).replace(',', sep[0])
        if (settings.groupingStyle == GroupingStyle.NONE) return normalized
        // Group integer parts of numbers in the expression without breaking decimals or operators
        val sb = StringBuilder()
        var i = 0
        while (i < normalized.length) {
            val ch = normalized[i]
            if (ch.isDigit()) {
                val start = i
                while (i < normalized.length && normalized[i].isDigit()) {
                    i++
                }
                val digits = normalized.substring(start, i)
                val isAfterDecimal = start > 0 && normalized[start - 1] == sep[0]
                if (isAfterDecimal || digits.length <= 3) {
                    sb.append(digits)
                } else {
                    sb.append(groupIntegerDigits(digits))
                }
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }

    private fun formatScientific(value: BigDecimal, settings: AppSettings): String {
        val rounded = value.round(MathContext(settings.decimalPrecision.coerceIn(4, 10), RoundingMode.HALF_UP))
        val str = rounded.toString()
        val parts = str.split('E', 'e')
        val mantissa = BigDecimal(parts[0]).stripTrailingZerosSafe().toPlainString()
            .replace(".", settings.decimalSeparator.symbol)
        return if (parts.size == 2) {
            val exp = parts[1].toIntOrNull() ?: 0
            "${mantissa}×10^$exp"
        } else {
            mantissa
        }
    }

    private fun applySeparators(plain: String, settings: AppSettings): String {
        val negative = plain.startsWith("-")
        val unsigned = if (negative) plain.substring(1) else plain
        val dotIndex = unsigned.indexOf('.')
        val intPart = if (dotIndex >= 0) unsigned.substring(0, dotIndex) else unsigned
        val fracPart = if (dotIndex >= 0) unsigned.substring(dotIndex + 1) else ""

        val groupedInt = if (settings.groupingStyle == GroupingStyle.SPACE) {
            groupIntegerDigits(intPart)
        } else {
            intPart
        }

        val prefix = if (negative) "−" else ""
        return if (fracPart.isNotEmpty()) {
            "$prefix$groupedInt${settings.decimalSeparator.symbol}$fracPart"
        } else {
            "$prefix$groupedInt"
        }
    }

    private fun groupIntegerDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        val firstGroup = digits.length % 3
        var idx = 0
        if (firstGroup > 0) {
            sb.append(digits.substring(0, firstGroup))
            idx = firstGroup
            if (idx < digits.length) sb.append('\u00A0')
        }
        while (idx < digits.length) {
            sb.append(digits.substring(idx, idx + 3))
            idx += 3
            if (idx < digits.length) sb.append('\u00A0')
        }
        return sb.toString()
    }

    private fun snapFloatingNoise(value: Double): Double {
        if (abs(value) < 1e-13) return 0.0
        val nearestHalf = kotlin.math.round(value * 2.0) / 2.0
        if (abs(value - nearestHalf) < 1e-12) return nearestHalf
        return value
    }

    fun BigDecimal.stripTrailingZerosSafe(): BigDecimal {
        return if (this.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            this.stripTrailingZeros()
        }
    }
}
