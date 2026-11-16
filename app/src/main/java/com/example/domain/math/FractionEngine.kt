package com.example.domain.math

import com.example.domain.model.AppSettings
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

data class MixedFraction(
    val isNegative: Boolean,
    val whole: BigInteger,
    val numerator: BigInteger,
    val denominator: BigInteger
)

data class ExactFraction(
    val numerator: BigInteger,
    val denominator: BigInteger
) {
    init {
        require(denominator != BigInteger.ZERO) { "Ділення на нуль неможливе" }
    }

    val isZero: Boolean get() = numerator == BigInteger.ZERO
    val isInteger: Boolean get() = denominator == BigInteger.ONE
    val isImproper: Boolean get() = denominator > BigInteger.ONE && numerator.abs() > denominator

    fun reduced(): ExactFraction {
        if (numerator == BigInteger.ZERO) return ExactFraction(BigInteger.ZERO, BigInteger.ONE)
        var num = numerator
        var den = denominator
        if (den < BigInteger.ZERO) {
            num = num.negate()
            den = den.negate()
        }
        val gcd = num.abs().gcd(den)
        return ExactFraction(num.divide(gcd), den.divide(gcd))
    }

    operator fun plus(other: ExactFraction): ExactFraction {
        val num = numerator.multiply(other.denominator).add(other.numerator.multiply(denominator))
        val den = denominator.multiply(other.denominator)
        return ExactFraction(num, den).reduced()
    }

    operator fun minus(other: ExactFraction): ExactFraction {
        val num = numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator))
        val den = denominator.multiply(other.denominator)
        return ExactFraction(num, den).reduced()
    }

    operator fun times(other: ExactFraction): ExactFraction {
        val num = numerator.multiply(other.numerator)
        val den = denominator.multiply(other.denominator)
        return ExactFraction(num, den).reduced()
    }

    operator fun div(other: ExactFraction): ExactFraction {
        if (other.numerator == BigInteger.ZERO) {
            throw FractionEvalException("Ділення на нуль неможливе")
        }
        val num = numerator.multiply(other.denominator)
        val den = denominator.multiply(other.numerator)
        return ExactFraction(num, den).reduced()
    }

    operator fun unaryMinus(): ExactFraction = ExactFraction(numerator.negate(), denominator).reduced()

    fun toMixed(): MixedFraction {
        val r = reduced()
        val neg = r.numerator < BigInteger.ZERO
        val absNum = r.numerator.abs()
        val whole = absNum.divide(r.denominator)
        val rem = absNum.remainder(r.denominator)
        return MixedFraction(
            isNegative = neg,
            whole = whole,
            numerator = rem,
            denominator = r.denominator
        )
    }

    fun toDecimalString(settings: AppSettings): String {
        val r = reduced()
        val mc = MathContext(settings.decimalPrecision + 6, RoundingMode.HALF_UP)
        val bd = BigDecimal(r.numerator).divide(BigDecimal(r.denominator), mc)
        return NumberFormatter.formatBigDecimal(bd, settings)
    }

    fun toPlainCanonical(): String {
        val r = reduced()
        return if (r.denominator == BigInteger.ONE) {
            r.numerator.toString()
        } else {
            "${r.numerator}/${r.denominator}"
        }
    }

    fun toMixedPlainCanonical(): String {
        val m = toMixed()
        val sign = if (m.isNegative) "−" else ""
        return when {
            m.numerator == BigInteger.ZERO -> "$sign${m.whole}"
            m.whole == BigInteger.ZERO -> "$sign${m.numerator}/${m.denominator}"
            else -> "$sign${m.whole} ${m.numerator}/${m.denominator}"
        }
    }

    companion object {
        val ZERO = ExactFraction(BigInteger.ZERO, BigInteger.ONE)
        val ONE = ExactFraction(BigInteger.ONE, BigInteger.ONE)

        fun of(num: Long, den: Long = 1L): ExactFraction {
            if (den == 0L) throw FractionEvalException("Ділення на нуль неможливе")
            return ExactFraction(BigInteger.valueOf(num), BigInteger.valueOf(den)).reduced()
        }

        fun fromMixed(
            isNegative: Boolean,
            whole: BigInteger,
            numerator: BigInteger,
            denominator: BigInteger
        ): ExactFraction {
            if (denominator == BigInteger.ZERO) throw FractionEvalException("Ділення на нуль неможливе")
            val absFrac = ExactFraction(
                whole.abs().multiply(denominator.abs()).add(numerator.abs()),
                denominator.abs()
            ).reduced()
            return if (isNegative) -absFrac else absFrac
        }

        fun fromDecimalString(text: String): ExactFraction {
            val normalized = text.trim().replace(',', '.').replace('−', '-')
            if (normalized.isEmpty() || normalized == "-" || normalized == ".") {
                throw FractionEvalException("Некоректне число")
            }
            val bd = try {
                BigDecimal(normalized)
            } catch (_: NumberFormatException) {
                throw FractionEvalException("Некоректне число")
            }
            val stripped = bd.stripTrailingZeros()
            return if (stripped.scale() <= 0) {
                ExactFraction(stripped.toBigIntegerExact(), BigInteger.ONE).reduced()
            } else {
                val den = BigInteger.TEN.pow(stripped.scale())
                ExactFraction(stripped.unscaledValue(), den).reduced()
            }
        }
    }
}

class FractionEvalException(val messageUkr: String) : RuntimeException(messageUkr)

enum class FractionOp(val symbol: String) {
    ADD("+"),
    SUB("−"),
    MUL("×"),
    DIV("÷")
}

sealed class FractionNode {
    data class NumberToken(val text: String) : FractionNode()
    data class OperatorToken(val op: FractionOp) : FractionNode()
    data class FractionToken(
        val numerator: MutableList<FractionNode> = mutableListOf(),
        val denominator: MutableList<FractionNode> = mutableListOf()
    ) : FractionNode()
    data class MixedFractionToken(
        val whole: MutableList<FractionNode> = mutableListOf(),
        val numerator: MutableList<FractionNode> = mutableListOf(),
        val denominator: MutableList<FractionNode> = mutableListOf()
    ) : FractionNode()
    data class ParenToken(
        val inner: MutableList<FractionNode> = mutableListOf()
    ) : FractionNode()
}

enum class SlotKind {
    ROOT,
    FRAC_NUM,
    FRAC_DEN,
    MIXED_WHOLE,
    MIXED_NUM,
    MIXED_DEN,
    PAREN_INNER
}

data class SlotStep(
    val nodeIndex: Int,
    val slotKind: SlotKind
)

data class FractionCursor(
    val path: List<SlotStep> = emptyList(),
    val index: Int = 0
)

data class FractionEvaluation(
    val reduced: ExactFraction,
    val mixed: MixedFraction
)

object FractionEvaluator {

    fun evaluate(nodes: List<FractionNode>): FractionEvaluation {
        if (nodes.isEmpty()) throw FractionEvalException("Введіть дробовий вираз")
        val value = evalSequence(nodes)
        val reduced = value.reduced()
        return FractionEvaluation(reduced = reduced, mixed = reduced.toMixed())
    }

    private fun evalSequence(nodes: List<FractionNode>): ExactFraction {
        if (nodes.isEmpty()) throw FractionEvalException("Порожній блок у дробі")
        val parser = SeqParser(nodes)
        val res = parser.parseAddSub()
        if (!parser.isAtEnd()) throw FractionEvalException("Некоректний дробовий вираз")
        return res
    }

    private class SeqParser(private val nodes: List<FractionNode>) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= nodes.size
        private fun peek(): FractionNode? = nodes.getOrNull(pos)
        private fun consume(): FractionNode = nodes[pos++]

        fun parseAddSub(): ExactFraction {
            var left = parseMulDiv()
            while (true) {
                val next = peek()
                if (next is FractionNode.OperatorToken &&
                    (next.op == FractionOp.ADD || next.op == FractionOp.SUB)
                ) {
                    consume()
                    val right = parseMulDiv()
                    left = if (next.op == FractionOp.ADD) left + right else left - right
                } else {
                    break
                }
            }
            return left
        }

        private fun parseMulDiv(): ExactFraction {
            var left = parseUnary()
            while (true) {
                val next = peek()
                if (next is FractionNode.OperatorToken &&
                    (next.op == FractionOp.MUL || next.op == FractionOp.DIV)
                ) {
                    consume()
                    val right = parseUnary()
                    left = if (next.op == FractionOp.MUL) left * right else left / right
                } else if (next is FractionNode.ParenToken ||
                    next is FractionNode.FractionToken ||
                    next is FractionNode.MixedFractionToken
                ) {
                    // Implicit multiplication e.g. 2(1/2) or (1/2)(3/4)
                    val right = parseUnary()
                    left *= right
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): ExactFraction {
            val next = peek()
            if (next is FractionNode.OperatorToken &&
                (next.op == FractionOp.SUB || next.op == FractionOp.ADD)
            ) {
                consume()
                val inner = parseUnary()
                return if (next.op == FractionOp.SUB) -inner else inner
            }
            return parsePrimary()
        }

        private fun parsePrimary(): ExactFraction {
            val n = peek() ?: throw FractionEvalException("Незавершений вираз")
            consume()
            return when (n) {
                is FractionNode.NumberToken -> ExactFraction.fromDecimalString(n.text)
                is FractionNode.FractionToken -> {
                    val numVal = evalSequence(n.numerator)
                    val denVal = evalSequence(n.denominator)
                    numVal / denVal
                }
                is FractionNode.MixedFractionToken -> {
                    val wholeVal = evalSequence(n.whole)
                    val numVal = evalSequence(n.numerator)
                    val denVal = evalSequence(n.denominator)
                    if (!wholeVal.isInteger || !numVal.isInteger || !denVal.isInteger) {
                        // Even if a user enters nested values inside mixed parts: whole + num/den
                        val fracPart = numVal / denVal
                        if (wholeVal.numerator < BigInteger.ZERO) wholeVal - fracPart else wholeVal + fracPart
                    } else {
                        if (denVal.numerator == BigInteger.ZERO) {
                            throw FractionEvalException("Ділення на нуль неможливе")
                        }
                        val isNeg = wholeVal.numerator < BigInteger.ZERO
                        ExactFraction.fromMixed(
                            isNegative = isNeg,
                            whole = wholeVal.numerator.abs(),
                            numerator = numVal.numerator,
                            denominator = denVal.numerator
                        )
                    }
                }
                is FractionNode.ParenToken -> evalSequence(n.inner)
                is FractionNode.OperatorToken -> throw FractionEvalException("Некоректний оператор")
            }
        }
    }

    fun toPlainText(nodes: List<FractionNode>): String {
        return nodes.joinToString("") { node ->
            when (node) {
                is FractionNode.NumberToken -> node.text
                is FractionNode.OperatorToken -> " ${node.op.symbol} "
                is FractionNode.FractionToken -> {
                    val numStr = toPlainText(node.numerator)
                    val denStr = toPlainText(node.denominator)
                    val wrapNum = if (node.numerator.size > 1) "($numStr)" else numStr
                    val wrapDen = if (node.denominator.size > 1) "($denStr)" else denStr
                    "$wrapNum/$wrapDen"
                }
                is FractionNode.MixedFractionToken -> {
                    "${toPlainText(node.whole)} ${toPlainText(node.numerator)}/${toPlainText(node.denominator)}"
                }
                is FractionNode.ParenToken -> "(${toPlainText(node.inner)})"
            }
        }.trim()
    }

    fun cloneNodes(nodes: List<FractionNode>): MutableList<FractionNode> {
        return nodes.map { node ->
            when (node) {
                is FractionNode.NumberToken -> node.copy()
                is FractionNode.OperatorToken -> node.copy()
                is FractionNode.FractionToken -> FractionNode.FractionToken(
                    numerator = cloneNodes(node.numerator),
                    denominator = cloneNodes(node.denominator)
                )
                is FractionNode.MixedFractionToken -> FractionNode.MixedFractionToken(
                    whole = cloneNodes(node.whole),
                    numerator = cloneNodes(node.numerator),
                    denominator = cloneNodes(node.denominator)
                )
                is FractionNode.ParenToken -> FractionNode.ParenToken(
                    inner = cloneNodes(node.inner)
                )
            }
        }.toMutableList()
    }
}

@JsonClass(generateAdapter = true)
data class FractionNodeDto(
    val type: String,
    val text: String? = null,
    val op: String? = null,
    val whole: List<FractionNodeDto>? = null,
    val numerator: List<FractionNodeDto>? = null,
    val denominator: List<FractionNodeDto>? = null,
    val inner: List<FractionNodeDto>? = null
)

object FractionSerializer {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, FractionNodeDto::class.java)
    private val adapter = moshi.adapter<List<FractionNodeDto>>(listType)

    fun toJson(nodes: List<FractionNode>): String = adapter.toJson(nodes.map { it.toDto() })

    fun fromJson(json: String?): MutableList<FractionNode>? {
        if (json.isNullOrBlank()) return null
        return try {
            adapter.fromJson(json)?.map { it.toDomain() }?.toMutableList()
        } catch (_: Exception) {
            null
        }
    }

    private fun FractionNode.toDto(): FractionNodeDto = when (this) {
        is FractionNode.NumberToken -> FractionNodeDto(type = "num", text = text)
        is FractionNode.OperatorToken -> FractionNodeDto(type = "op", op = op.name)
        is FractionNode.FractionToken -> FractionNodeDto(
            type = "frac",
            numerator = numerator.map { it.toDto() },
            denominator = denominator.map { it.toDto() }
        )
        is FractionNode.MixedFractionToken -> FractionNodeDto(
            type = "mixed",
            whole = whole.map { it.toDto() },
            numerator = numerator.map { it.toDto() },
            denominator = denominator.map { it.toDto() }
        )
        is FractionNode.ParenToken -> FractionNodeDto(
            type = "paren",
            inner = inner.map { it.toDto() }
        )
    }

    private fun FractionNodeDto.toDomain(): FractionNode = when (type) {
        "num" -> FractionNode.NumberToken(text ?: "0")
        "op" -> FractionNode.OperatorToken(FractionOp.valueOf(op ?: "ADD"))
        "frac" -> FractionNode.FractionToken(
            numerator = numerator?.map { it.toDomain() }?.toMutableList() ?: mutableListOf(),
            denominator = denominator?.map { it.toDomain() }?.toMutableList() ?: mutableListOf()
        )
        "mixed" -> FractionNode.MixedFractionToken(
            whole = whole?.map { it.toDomain() }?.toMutableList() ?: mutableListOf(),
            numerator = numerator?.map { it.toDomain() }?.toMutableList() ?: mutableListOf(),
            denominator = denominator?.map { it.toDomain() }?.toMutableList() ?: mutableListOf()
        )
        "paren" -> FractionNode.ParenToken(
            inner = inner?.map { it.toDomain() }?.toMutableList() ?: mutableListOf()
        )
        else -> FractionNode.NumberToken("0")
    }
}
