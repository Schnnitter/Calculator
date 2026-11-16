package com.example.domain.math

import com.example.domain.math.NumberFormatter.stripTrailingZerosSafe
import com.example.domain.model.AngleUnit
import com.example.domain.model.AppSettings
import com.example.domain.model.EvalResult
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

class StandardScientificEngine {

    private val mathContext = MathContext(34, RoundingMode.HALF_UP)

    private sealed class CalcVal {
        data class Exact(val bd: BigDecimal) : CalcVal()
        data class Approx(val d: Double) : CalcVal()

        fun asDouble(): Double = when (this) {
            is Exact -> bd.toDouble()
            is Approx -> d
        }

        fun asBigDecimalOrNull(): BigDecimal? = when (this) {
            is Exact -> bd
            is Approx -> if (d.isFinite()) BigDecimal.valueOf(d) else null
        }
    }

    private data class TermResult(
        val value: CalcVal,
        val isPercentageTerm: Boolean = false
    )

    private class MathEvalException(val messageUkr: String) : RuntimeException(messageUkr)

    fun evaluate(
        expression: String,
        angleUnit: AngleUnit,
        settings: AppSettings
    ): EvalResult {
        val cleaned = normalizeExpression(expression)
        if (cleaned.isBlank()) {
            return EvalResult.Success("0", "0")
        }

        return try {
            val tokens = tokenize(cleaned)
            if (tokens.isEmpty()) return EvalResult.Success("0", "0")
            val parser = Parser(tokens, angleUnit)
            val result = parser.parseExpression()
            if (!parser.isAtEnd()) {
                return EvalResult.Error("Некоректний вираз")
            }
            formatCalcVal(result, settings)
        } catch (e: MathEvalException) {
            EvalResult.Error(e.messageUkr)
        } catch (_: ArithmeticException) {
            EvalResult.Error("Ділення на нуль неможливе")
        } catch (_: Exception) {
            EvalResult.Error("Некоректний вираз")
        }
    }

    private fun formatCalcVal(value: CalcVal, settings: AppSettings): EvalResult {
        return when (value) {
            is CalcVal.Exact -> {
                val stripped = value.bd.stripTrailingZerosSafe()
                val raw = stripped.toPlainString()
                val formatted = NumberFormatter.formatBigDecimal(stripped, settings)
                EvalResult.Success(numericString = raw, formattedDisplay = formatted)
            }
            is CalcVal.Approx -> {
                val d = value.d
                if (d.isNaN() || d.isInfinite()) {
                    EvalResult.Error("Значення поза областю визначення")
                } else {
                    val formatted = NumberFormatter.formatDouble(d, settings)
                    val bd = BigDecimal.valueOf(d).setScale(settings.decimalPrecision, RoundingMode.HALF_UP)
                        .stripTrailingZerosSafe()
                    EvalResult.Success(
                        numericString = bd.toPlainString(),
                        formattedDisplay = formatted
                    )
                }
            }
        }
    }

    private fun normalizeExpression(input: String): String {
        var s = input
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(',', '.')
            .replace('−', '-')
            .replace('×', '*')
            .replace('÷', '/')
            .replace("sin⁻¹", "asin")
            .replace("cos⁻¹", "acos")
            .replace("tan⁻¹", "atan")
            .replace("ʸ√", " root ")
            .replace("ⁿ√", " root ")
            .trim()

        // Auto-close unmatched open parentheses at the end for smooth calculator UX
        var openCount = 0
        for (ch in s) {
            if (ch == '(') openCount++
            else if (ch == ')') {
                if (openCount == 0) {
                    throw MathEvalException("Некоректні дужки")
                }
                openCount--
            }
        }
        if (openCount > 0) {
            s += ")".repeat(openCount)
        }
        return s
    }

    private sealed class Token {
        data class Num(val text: String) : Token()
        data class Ident(val name: String) : Token()
        data class Op(val symbol: String) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object Bar : Token()
    }

    private fun tokenize(input: String): List<Token> {
        val raw = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var hasDot = ch == '.'
                    i++
                    while (i < input.length) {
                        val c = input[i]
                        if (c.isDigit()) {
                            i++
                        } else if (c == '.' && !hasDot) {
                            hasDot = true
                            i++
                        } else if ((c == 'E' || c == 'e') && i + 1 < input.length &&
                            (input[i + 1].isDigit() || input[i + 1] == '+' || input[i + 1] == '-')
                        ) {
                            // Scientific notation number e.g. 1.5E-4 (but not Euler constant e)
                            i++
                            if (input[i] == '+' || input[i] == '-') i++
                        } else {
                            break
                        }
                    }
                    val numStr = input.substring(start, i)
                    if (numStr == ".") throw MathEvalException("Некоректне число")
                    raw.add(Token.Num(numStr))
                }
                ch == 'π' -> {
                    raw.add(Token.Ident("pi"))
                    i++
                }
                ch == '√' -> {
                    raw.add(Token.Ident("sqrt"))
                    i++
                }
                ch == '²' -> {
                    raw.add(Token.Op("^"))
                    raw.add(Token.Num("2"))
                    i++
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < input.length && input[i].isLetter()) i++
                    val word = input.substring(start, i).lowercase()
                    raw.add(Token.Ident(word))
                }
                ch == '(' -> {
                    raw.add(Token.LParen)
                    i++
                }
                ch == ')' -> {
                    raw.add(Token.RParen)
                    i++
                }
                ch == '|' -> {
                    raw.add(Token.Bar)
                    i++
                }
                ch in charArrayOf('+', '-', '*', '/', '^', '%', '!') -> {
                    raw.add(Token.Op(ch.toString()))
                    i++
                }
                else -> throw MathEvalException("Невідомий символ")
            }
        }

        // Convert |expr| pairs to abs(expr)
        val withAbs = resolveAbsoluteBars(raw)
        // Insert implicit multiplication where mathematically natural: 2π, 2(3), (1)(2), 3sin(30), π(2), 5!2
        return insertImplicitMultiplication(withAbs)
    }

    private fun resolveAbsoluteBars(tokens: List<Token>): List<Token> {
        val out = mutableListOf<Token>()
        var openAbs = false
        for (idx in tokens.indices) {
            val t = tokens[idx]
            if (t is Token.Bar) {
                val prev = tokens.getOrNull(idx - 1)
                val shouldClose = openAbs && (prev is Token.Num || prev is Token.RParen ||
                    (prev is Token.Ident && (prev.name == "pi" || prev.name == "e")) ||
                    (prev is Token.Op && (prev.symbol == "%" || prev.symbol == "!")))
                if (shouldClose) {
                    out.add(Token.RParen)
                    openAbs = false
                } else {
                    out.add(Token.Ident("abs"))
                    out.add(Token.LParen)
                    openAbs = true
                }
            } else {
                out.add(t)
            }
        }
        if (openAbs) {
            out.add(Token.RParen)
        }
        return out
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val out = mutableListOf<Token>()
        for (i in tokens.indices) {
            val curr = tokens[i]
            if (i > 0) {
                val prev = tokens[i - 1]
                val prevEndsValue = prev is Token.Num ||
                    prev is Token.RParen ||
                    (prev is Token.Ident && (prev.name == "pi" || prev.name == "e")) ||
                    (prev is Token.Op && (prev.symbol == "%" || prev.symbol == "!"))
                val currStartsValue = curr is Token.Num ||
                    curr is Token.LParen ||
                    (curr is Token.Ident && curr.name != "root")
                if (prevEndsValue && currStartsValue) {
                    out.add(Token.Op("*"))
                }
            }
            out.add(curr)
        }
        return out
    }

    private inner class Parser(
        private val tokens: List<Token>,
        private val angleUnit: AngleUnit
    ) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): CalcVal {
            var left = parseTermWithFlag().value
            while (true) {
                val next = peek()
                if (next is Token.Op && (next.symbol == "+" || next.symbol == "-")) {
                    consume()
                    val rightTerm = parseTermWithFlag()
                    left = if (rightTerm.isPercentageTerm) {
                        // Contextual percentage: rightTerm.value is already (pct / 100)
                        val delta = multiplyValues(left, rightTerm.value)
                        if (next.symbol == "+") addValues(left, delta) else subtractValues(left, delta)
                    } else {
                        if (next.symbol == "+") addValues(left, rightTerm.value) else subtractValues(left, rightTerm.value)
                    }
                } else {
                    break
                }
            }
            return left
        }

        private fun parseTermWithFlag(): TermResult {
            var left = parseUnary()
            var isBarePercent = false
            // Check if the single factor itself ended with %
            if (left.second) {
                isBarePercent = true
            }
            var result = left.first
            while (true) {
                val next = peek()
                if (next is Token.Op && (next.symbol == "*" || next.symbol == "/")) {
                    isBarePercent = false
                    consume()
                    val right = parseUnary().first
                    result = if (next.symbol == "*") {
                        multiplyValues(result, right)
                    } else {
                        divideExactOrDouble(result, right)
                    }
                } else {
                    break
                }
            }
            return TermResult(result, isBarePercent)
        }

        // Returns Pair(value, endedWithBarePercent)
        private fun parseUnary(): Pair<CalcVal, Boolean> {
            val next = peek()
            if (next is Token.Op && (next.symbol == "+" || next.symbol == "-")) {
                consume()
                val inner = parseUnary()
                val value = if (next.symbol == "-") negateValue(inner.first) else inner.first
                return value to inner.second
            }
            return parsePower()
        }

        private fun parsePower(): Pair<CalcVal, Boolean> {
            val basePair = parsePostfix()
            val next = peek()
            if (next is Token.Op && next.symbol == "^") {
                consume()
                val exp = parseUnary().first
                return powerValues(basePair.first, exp) to false
            }
            if (next is Token.Ident && next.name == "root") {
                consume()
                val radicand = parseUnary().first
                return nthRootValues(basePair.first, radicand) to false
            }
            return basePair
        }

        private fun parsePostfix(): Pair<CalcVal, Boolean> {
            var value = parsePrimary()
            var endedWithPercent = false
            while (true) {
                val next = peek()
                if (next is Token.Op && next.symbol == "!") {
                    consume()
                    value = factorialValue(value)
                    endedWithPercent = false
                } else if (next is Token.Op && next.symbol == "%") {
                    consume()
                    // Note: value stays un-divided if it's a bare percent? No, divide by 100 immediately,
                    // wait: if we divide by 100 here, then parseExpression would divide by 100 again!
                    // Instead: return raw value if bare %? No, nested (200 * 10%) wouldn't know!
                    // Cleaner: store value / 100 here, and in parseExpression multiply left * (right * 100 / 100) = left * right!
                    // Wait: 10% -> value becomes 0.10, isPercentageTerm = true.
                    // In 100 + 10%: delta = 100 * 0.10 = 10 -> 100 + 10 = 110!
                    // In 200 * 10%: 200 * 0.10 = 20!
                    // What about 100 + 10% * 2? isBarePercent became false in parseTermWithFlag -> 100 + 0.2 = 100.2!
                    value = divideExactOrDouble(value, CalcVal.Exact(BigDecimal(100)))
                    endedWithPercent = true
                } else {
                    break
                }
            }
            return value to endedWithPercent
        }

        private fun parsePrimary(): CalcVal {
            val t = peek() ?: throw MathEvalException("Незавершений вираз")
            return when (t) {
                is Token.Num -> {
                    consume()
                    try {
                        CalcVal.Exact(BigDecimal(t.text))
                    } catch (_: NumberFormatException) {
                        throw MathEvalException("Некоректне число")
                    }
                }
                is Token.LParen -> {
                    consume()
                    val v = parseExpression()
                    val closing = peek()
                    if (closing !is Token.RParen) {
                        throw MathEvalException("Очікувалась закриваюча дужка")
                    }
                    consume()
                    v
                }
                is Token.Ident -> {
                    consume()
                    when (t.name) {
                        "pi" -> CalcVal.Approx(PI)
                        "e" -> CalcVal.Approx(E)
                        "sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "abs" -> {
                            val arg = if (peek() is Token.LParen) {
                                consume()
                                val inner = parseExpression()
                                if (peek() !is Token.RParen) {
                                    throw MathEvalException("Очікувалась закриваюча дужка")
                                }
                                consume()
                                inner
                            } else {
                                parseUnary().first
                            }
                            applyFunction(t.name, arg)
                        }
                        else -> throw MathEvalException("Невідома функція: ${t.name}")
                    }
                }
                else -> throw MathEvalException("Некоректний вираз")
            }
        }

        private fun applyFunction(name: String, arg: CalcVal): CalcVal {
            val d = arg.asDouble()
            return when (name) {
                "abs" -> when (arg) {
                    is CalcVal.Exact -> CalcVal.Exact(arg.bd.abs())
                    is CalcVal.Approx -> CalcVal.Approx(abs(d))
                }
                "sqrt" -> {
                    if (d < 0.0) throw MathEvalException("Корінь із від’ємного числа")
                    if (arg is CalcVal.Exact) {
                        val bdSqrt = bigDecimalSqrt(arg.bd)
                        CalcVal.Exact(bdSqrt)
                    } else {
                        CalcVal.Approx(sqrt(d))
                    }
                }
                "log" -> {
                    if (d <= 0.0) throw MathEvalException("Значення поза областю визначення")
                    CalcVal.Approx(log10(d))
                }
                "ln" -> {
                    if (d <= 0.0) throw MathEvalException("Значення поза областю визначення")
                    CalcVal.Approx(ln(d))
                }
                "sin" -> {
                    val rad = toRadiansIfNeeded(d)
                    CalcVal.Approx(sin(rad))
                }
                "cos" -> {
                    val rad = toRadiansIfNeeded(d)
                    CalcVal.Approx(cos(rad))
                }
                "tan" -> {
                    val rad = toRadiansIfNeeded(d)
                    if (abs(cos(rad)) < 1e-12) {
                        throw MathEvalException("Значення поза областю визначення")
                    }
                    CalcVal.Approx(tan(rad))
                }
                "asin" -> {
                    if (d < -1.0 || d > 1.0) throw MathEvalException("Значення поза областю визначення")
                    CalcVal.Approx(fromRadiansIfNeeded(asin(d)))
                }
                "acos" -> {
                    if (d < -1.0 || d > 1.0) throw MathEvalException("Значення поза областю визначення")
                    CalcVal.Approx(fromRadiansIfNeeded(acos(d)))
                }
                "atan" -> CalcVal.Approx(fromRadiansIfNeeded(atan(d)))
                else -> throw MathEvalException("Невідома функція")
            }
        }

        private fun toRadiansIfNeeded(v: Double): Double =
            if (angleUnit == AngleUnit.DEG) Math.toRadians(v) else v

        private fun fromRadiansIfNeeded(rad: Double): Double =
            if (angleUnit == AngleUnit.DEG) Math.toDegrees(rad) else rad

        private fun addValues(a: CalcVal, b: CalcVal): CalcVal =
            if (a is CalcVal.Exact && b is CalcVal.Exact) {
                CalcVal.Exact(a.bd.add(b.bd, mathContext))
            } else {
                CalcVal.Approx(a.asDouble() + b.asDouble())
            }

        private fun subtractValues(a: CalcVal, b: CalcVal): CalcVal =
            if (a is CalcVal.Exact && b is CalcVal.Exact) {
                CalcVal.Exact(a.bd.subtract(b.bd, mathContext))
            } else {
                CalcVal.Approx(a.asDouble() - b.asDouble())
            }

        private fun multiplyValues(a: CalcVal, b: CalcVal): CalcVal =
            if (a is CalcVal.Exact && b is CalcVal.Exact) {
                CalcVal.Exact(a.bd.multiply(b.bd, mathContext))
            } else {
                CalcVal.Approx(a.asDouble() * b.asDouble())
            }

        private fun divideExactOrDouble(a: CalcVal, b: CalcVal): CalcVal {
            if (b is CalcVal.Exact && b.bd.compareTo(BigDecimal.ZERO) == 0) {
                throw MathEvalException("Ділення на нуль неможливе")
            }
            if (b is CalcVal.Approx && abs(b.d) == 0.0) {
                throw MathEvalException("Ділення на нуль неможливе")
            }
            return if (a is CalcVal.Exact && b is CalcVal.Exact) {
                CalcVal.Exact(a.bd.divide(b.bd, mathContext))
            } else {
                val res = a.asDouble() / b.asDouble()
                if (res.isInfinite() || res.isNaN()) throw MathEvalException("Ділення на нуль неможливе")
                CalcVal.Approx(res)
            }
        }

        private fun negateValue(a: CalcVal): CalcVal = when (a) {
            is CalcVal.Exact -> CalcVal.Exact(a.bd.negate())
            is CalcVal.Approx -> CalcVal.Approx(-a.d)
        }

        private fun powerValues(base: CalcVal, exp: CalcVal): CalcVal {
            if (base is CalcVal.Exact && exp is CalcVal.Exact) {
                val strippedExp = exp.bd.stripTrailingZerosSafe()
                if (strippedExp.scale() <= 0) {
                    val intExp = try {
                        strippedExp.intValueExact()
                    } catch (_: ArithmeticException) {
                        null
                    }
                    if (intExp != null && intExp in -999..999) {
                        if (base.bd.compareTo(BigDecimal.ZERO) == 0 && intExp < 0) {
                            throw MathEvalException("Ділення на нуль неможливе")
                        }
                        return try {
                            CalcVal.Exact(base.bd.pow(intExp, mathContext))
                        } catch (_: ArithmeticException) {
                            throw MathEvalException("Число занадто велике")
                        }
                    }
                }
            }
            val b = base.asDouble()
            val e = exp.asDouble()
            if (b == 0.0 && e < 0.0) throw MathEvalException("Ділення на нуль неможливе")
            val res = b.pow(e)
            if (res.isNaN()) throw MathEvalException("Значення поза областю визначення")
            if (res.isInfinite()) throw MathEvalException("Число занадто велике")
            return CalcVal.Approx(res)
        }

        private fun nthRootValues(degree: CalcVal, radicand: CalcVal): CalcVal {
            val n = degree.asDouble()
            val x = radicand.asDouble()
            if (n == 0.0) throw MathEvalException("Ділення на нуль неможливе")
            // Check odd integer root of negative number
            val strippedN = degree.asBigDecimalOrNull()?.stripTrailingZerosSafe()
            val isOddInteger = strippedN != null && strippedN.scale() <= 0 &&
                (strippedN.toBigInteger().mod(BigInteger.valueOf(2)) == BigInteger.ONE)
            if (x < 0.0) {
                if (!isOddInteger) {
                    throw MathEvalException("Корінь парного степеня з від’ємного числа")
                }
                val res = -(-x).pow(1.0 / n)
                return CalcVal.Approx(res)
            }
            if (x == 0.0 && n < 0.0) throw MathEvalException("Ділення на нуль неможливе")
            val res = x.pow(1.0 / n)
            if (res.isNaN() || res.isInfinite()) throw MathEvalException("Значення поза областю визначення")
            return CalcVal.Approx(res)
        }

        private fun factorialValue(v: CalcVal): CalcVal {
            val bd = v.asBigDecimalOrNull()?.stripTrailingZerosSafe()
                ?: throw MathEvalException("Факторіал визначений лише для цілих невід’ємних чисел")
            if (bd.scale() > 0 || bd.compareTo(BigDecimal.ZERO) < 0) {
                throw MathEvalException("Факторіал визначений лише для цілих невід’ємних чисел")
            }
            val n = try {
                bd.intValueExact()
            } catch (_: ArithmeticException) {
                throw MathEvalException("Число занадто велике")
            }
            if (n > 170) throw MathEvalException("Число занадто велике")
            var acc = BigInteger.ONE
            for (i in 2..n) {
                acc = acc.multiply(BigInteger.valueOf(i.toLong()))
            }
            return CalcVal.Exact(BigDecimal(acc))
        }

        private fun bigDecimalSqrt(value: BigDecimal): BigDecimal {
            if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
            var x = BigDecimal.valueOf(sqrt(value.toDouble()))
            if (x.compareTo(BigDecimal.ZERO) == 0) {
                x = value.divide(BigDecimal.valueOf(2), mathContext)
            }
            val two = BigDecimal.valueOf(2)
            repeat(12) {
                x = x.add(value.divide(x, mathContext), mathContext).divide(two, mathContext)
            }
            return x.stripTrailingZerosSafe()
        }
    }
}
