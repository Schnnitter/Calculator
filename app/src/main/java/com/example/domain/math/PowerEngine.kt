package com.example.domain.math

import com.example.domain.math.NumberFormatter.stripTrailingZerosSafe
import com.example.domain.model.AppSettings
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.pow

class PowerEvalException(val messageUkr: String) : RuntimeException(messageUkr)

enum class PowerOp(val symbol: String) {
    ADD("+"),
    SUB("−"),
    MUL("×"),
    DIV("÷")
}

sealed class PowerNode {
    data class NumberToken(val text: String) : PowerNode()
    data class OperatorToken(val op: PowerOp) : PowerNode()
    data class PowerToken(
        val base: MutableList<PowerNode> = mutableListOf(),
        val exponent: MutableList<PowerNode> = mutableListOf()
    ) : PowerNode()
    data class ParenToken(
        val inner: MutableList<PowerNode> = mutableListOf()
    ) : PowerNode()
}

enum class PowerSlotKind {
    ROOT,
    BASE,
    EXPONENT,
    PAREN_INNER
}

data class PowerSlotStep(
    val nodeIndex: Int,
    val slotKind: PowerSlotKind
)

data class PowerCursor(
    val path: List<PowerSlotStep> = emptyList(),
    val index: Int = 0
)

data class SimplifiedPowerForm(
    val baseText: String,
    val exponentText: String,
    val lawHintUkr: String
)

data class PowerEvaluation(
    val numericValue: BigDecimal?,
    val formattedResult: String,
    val rawNumeric: String,
    val fractionHint: String? = null,
    val simplifiedPower: SimplifiedPowerForm? = null
)

object PowerEvaluator {

    private val mc = MathContext(34, RoundingMode.HALF_UP)

    fun evaluate(nodes: List<PowerNode>, settings: AppSettings): PowerEvaluation {
        if (nodes.isEmpty()) throw PowerEvalException("Введіть вираз зі степенями")
        val parser = SeqParser(nodes)
        val resultVal = parser.parseAddSub()
        if (!parser.isAtEnd()) throw PowerEvalException("Некоректний вираз")

        val stripped = resultVal.bd?.stripTrailingZerosSafe()
        val formatted = if (stripped != null) {
            NumberFormatter.formatBigDecimal(stripped, settings)
        } else {
            NumberFormatter.formatDouble(resultVal.approx, settings)
        }
        val raw = stripped?.toPlainString() ?: BigDecimal.valueOf(resultVal.approx)
            .setScale(settings.decimalPrecision, RoundingMode.HALF_UP)
            .stripTrailingZerosSafe()
            .toPlainString()

        val fractionHint = computeExactFractionHint(nodes, settings)
        val simplified = trySimplifySameBase(nodes, settings)

        return PowerEvaluation(
            numericValue = stripped,
            formattedResult = formatted,
            rawNumeric = raw,
            fractionHint = fractionHint,
            simplifiedPower = simplified
        )
    }

    private data class NumVal(val bd: BigDecimal?, val approx: Double) {
        fun asDouble(): Double = bd?.toDouble() ?: approx
    }

    private class SeqParser(private val nodes: List<PowerNode>) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= nodes.size
        private fun peek(): PowerNode? = nodes.getOrNull(pos)
        private fun consume(): PowerNode = nodes[pos++]

        fun parseAddSub(): NumVal {
            var left = parseMulDiv()
            while (true) {
                val next = peek()
                if (next is PowerNode.OperatorToken &&
                    (next.op == PowerOp.ADD || next.op == PowerOp.SUB)
                ) {
                    consume()
                    val right = parseMulDiv()
                    left = if (next.op == PowerOp.ADD) add(left, right) else sub(left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseMulDiv(): NumVal {
            var left = parseUnary()
            while (true) {
                val next = peek()
                if (next is PowerNode.OperatorToken &&
                    (next.op == PowerOp.MUL || next.op == PowerOp.DIV)
                ) {
                    consume()
                    val right = parseUnary()
                    left = if (next.op == PowerOp.MUL) mul(left, right) else div(left, right)
                } else if (next is PowerNode.ParenToken || next is PowerNode.PowerToken) {
                    val right = parseUnary()
                    left = mul(left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): NumVal {
            val next = peek()
            if (next is PowerNode.OperatorToken &&
                (next.op == PowerOp.SUB || next.op == PowerOp.ADD)
            ) {
                consume()
                val inner = parseUnary()
                return if (next.op == PowerOp.SUB) negate(inner) else inner
            }
            return parsePrimary()
        }

        private fun parsePrimary(): NumVal {
            val node = peek() ?: throw PowerEvalException("Незавершений вираз")
            consume()
            return when (node) {
                is PowerNode.NumberToken -> {
                    val bd = parseNumber(node.text)
                    NumVal(bd, bd.toDouble())
                }
                is PowerNode.ParenToken -> {
                    if (node.inner.isEmpty()) throw PowerEvalException("Порожні дужки")
                    val p = SeqParser(node.inner)
                    val res = p.parseAddSub()
                    if (!p.isAtEnd()) throw PowerEvalException("Некоректний вираз у дужках")
                    res
                }
                is PowerNode.PowerToken -> {
                    if (node.base.isEmpty()) throw PowerEvalException("Вкажіть основу степеня")
                    if (node.exponent.isEmpty()) throw PowerEvalException("Вкажіть показник степеня")
                    val baseParser = SeqParser(node.base)
                    val baseVal = baseParser.parseAddSub()
                    if (!baseParser.isAtEnd()) throw PowerEvalException("Некоректна основа степеня")

                    val expParser = SeqParser(node.exponent)
                    val expVal = expParser.parseAddSub()
                    if (!expParser.isAtEnd()) throw PowerEvalException("Некоректний показник степеня")

                    pow(baseVal, expVal)
                }
                is PowerNode.OperatorToken -> throw PowerEvalException("Некоректний оператор")
            }
        }

        private fun parseNumber(text: String): BigDecimal {
            val cleaned = text.trim().replace(',', '.').replace('−', '-')
            if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") {
                throw PowerEvalException("Некоректне число")
            }
            return try {
                BigDecimal(cleaned)
            } catch (_: NumberFormatException) {
                throw PowerEvalException("Некоректне число")
            }
        }

        private fun add(a: NumVal, b: NumVal): NumVal =
            if (a.bd != null && b.bd != null) {
                val r = a.bd.add(b.bd, mc)
                NumVal(r, r.toDouble())
            } else {
                NumVal(null, a.asDouble() + b.asDouble())
            }

        private fun sub(a: NumVal, b: NumVal): NumVal =
            if (a.bd != null && b.bd != null) {
                val r = a.bd.subtract(b.bd, mc)
                NumVal(r, r.toDouble())
            } else {
                NumVal(null, a.asDouble() - b.asDouble())
            }

        private fun mul(a: NumVal, b: NumVal): NumVal =
            if (a.bd != null && b.bd != null) {
                val r = a.bd.multiply(b.bd, mc)
                NumVal(r, r.toDouble())
            } else {
                NumVal(null, a.asDouble() * b.asDouble())
            }

        private fun div(a: NumVal, b: NumVal): NumVal {
            if (b.bd != null && b.bd.compareTo(BigDecimal.ZERO) == 0) {
                throw PowerEvalException("Ділення на нуль неможливе")
            }
            if (b.bd == null && abs(b.approx) == 0.0) {
                throw PowerEvalException("Ділення на нуль неможливе")
            }
            return if (a.bd != null && b.bd != null) {
                val r = a.bd.divide(b.bd, mc)
                NumVal(r, r.toDouble())
            } else {
                val res = a.asDouble() / b.asDouble()
                if (!res.isFinite()) throw PowerEvalException("Ділення на нуль неможливе")
                NumVal(null, res)
            }
        }

        private fun negate(a: NumVal): NumVal =
            if (a.bd != null) NumVal(a.bd.negate(), -a.bd.toDouble())
            else NumVal(null, -a.approx)

        private fun pow(base: NumVal, exp: NumVal): NumVal {
            if (base.bd != null && exp.bd != null) {
                val strippedExp = exp.bd.stripTrailingZerosSafe()
                if (strippedExp.scale() <= 0) {
                    val intExp = try {
                        strippedExp.intValueExact()
                    } catch (_: ArithmeticException) {
                        null
                    }
                    if (intExp != null && intExp in -999..999) {
                        if (base.bd.compareTo(BigDecimal.ZERO) == 0 && intExp < 0) {
                            throw PowerEvalException("Ділення на нуль неможливе")
                        }
                        if (base.bd.compareTo(BigDecimal.ZERO) == 0 && intExp == 0) {
                            return NumVal(BigDecimal.ONE, 1.0)
                        }
                        return try {
                            val r = base.bd.pow(intExp, mc)
                            NumVal(r, r.toDouble())
                        } catch (_: ArithmeticException) {
                            throw PowerEvalException("Число занадто велике")
                        }
                    }
                }
            }

            val b = base.asDouble()
            val e = exp.asDouble()
            if (b == 0.0 && e < 0.0) throw PowerEvalException("Ділення на нуль неможливе")
            if (b < 0.0) {
                throw PowerEvalException("Дробовий степінь від’ємного числа не визначений")
            }
            val res = b.pow(e)
            if (res.isNaN()) throw PowerEvalException("Значення поза областю визначення")
            if (res.isInfinite()) throw PowerEvalException("Число занадто велике")
            return NumVal(null, res)
        }
    }

    private fun computeExactFractionHint(nodes: List<PowerNode>, settings: AppSettings): String? {
        if (nodes.size != 1) return null
        val p = nodes.first() as? PowerNode.PowerToken ?: return null
        val baseVal = runCatching {
            val parser = SeqParser(p.base)
            val v = parser.parseAddSub()
            if (parser.isAtEnd()) v.bd else null
        }.getOrNull() ?: return null
        val expVal = runCatching {
            val parser = SeqParser(p.exponent)
            val v = parser.parseAddSub()
            if (parser.isAtEnd()) v.bd else null
        }.getOrNull() ?: return null

        val strippedBase = baseVal.stripTrailingZerosSafe()
        val strippedExp = expVal.stripTrailingZerosSafe()
        if (strippedBase.scale() <= 0 && strippedExp.scale() <= 0) {
            val intExp = runCatching { strippedExp.intValueExact() }.getOrNull() ?: return null
            if (intExp in -20..-1 && strippedBase.compareTo(BigDecimal.ZERO) != 0) {
                val denom = strippedBase.toBigIntegerExact().pow(-intExp)
                val sign = if (denom < BigInteger.ZERO) "−" else ""
                val absDen = denom.abs()
                return if (absDen == BigInteger.ONE) null else "${sign}1/$absDen"
            }
        }
        return null
    }

    private data class BaseExpPair(
        val base: BigDecimal,
        val exp: BigDecimal,
        val hadPower: Boolean,
        val usedPowerOfPower: Boolean
    )

    private fun trySimplifySameBase(nodes: List<PowerNode>, settings: AppSettings): SimplifiedPowerForm? {
        // Only simplify pure multiplication/division chains (or a single power-of-power)
        if (nodes.isEmpty()) return null
        val items = mutableListOf<Pair<PowerOp?, BaseExpPair>>()
        var expectOp = false
        var pendingOp: PowerOp? = null

        for (node in nodes) {
            if (!expectOp) {
                val factor = extractBaseExp(node) ?: return null
                items.add(pendingOp to factor)
                pendingOp = null
                expectOp = true
            } else {
                val opNode = node as? PowerNode.OperatorToken ?: return null
                if (opNode.op != PowerOp.MUL && opNode.op != PowerOp.DIV) return null
                pendingOp = opNode.op
                expectOp = false
            }
        }
        if (pendingOp != null || items.isEmpty()) return null

        val firstBase = items.first().second.base.stripTrailingZerosSafe()
        if (items.any { it.second.base.stripTrailingZerosSafe().compareTo(firstBase) != 0 }) {
            return null
        }

        val hasAnyPower = items.any { it.second.hadPower }
        if (!hasAnyPower) return null
        if (items.size == 1 && !items.first().second.usedPowerOfPower) return null

        var totalExp = BigDecimal.ZERO
        var usedMul = false
        var usedDiv = false
        var usedPowPow = false

        for ((op, factor) in items) {
            if (factor.usedPowerOfPower) usedPowPow = true
            when (op) {
                null, PowerOp.MUL -> {
                    if (op == PowerOp.MUL) usedMul = true
                    totalExp = totalExp.add(factor.exp, mc)
                }
                PowerOp.DIV -> {
                    usedDiv = true
                    totalExp = totalExp.subtract(factor.exp, mc)
                }
                else -> return null
            }
        }

        val lawHint = when {
            usedMul && usedDiv -> "aᵐ × aⁿ ÷ aᵏ = aᵐ⁺ⁿ⁻ᵏ"
            usedMul && usedPowPow -> "(aᵐ)ⁿ = aᵐˣⁿ, aᵐ × aⁿ = aᵐ⁺ⁿ"
            usedDiv && usedPowPow -> "(aᵐ)ⁿ = aᵐˣⁿ, aᵐ ÷ aⁿ = aᵐ⁻ⁿ"
            usedMul -> "aᵐ × aⁿ = aᵐ⁺ⁿ"
            usedDiv -> "aᵐ ÷ aⁿ = aᵐ⁻ⁿ"
            usedPowPow -> "(aᵐ)ⁿ = aᵐˣⁿ"
            else -> ""
        }

        val baseStr = NumberFormatter.formatBigDecimal(firstBase, settings)
        val expStr = NumberFormatter.formatBigDecimal(totalExp.stripTrailingZerosSafe(), settings)
        return SimplifiedPowerForm(
            baseText = if (firstBase < BigDecimal.ZERO) "($baseStr)" else baseStr,
            exponentText = expStr,
            lawHintUkr = lawHint
        )
    }

    private fun extractBaseExp(node: PowerNode): BaseExpPair? {
        return when (node) {
            is PowerNode.NumberToken -> {
                val bd = runCatching {
                    BigDecimal(node.text.trim().replace(',', '.').replace('−', '-'))
                }.getOrNull() ?: return null
                BaseExpPair(base = bd, exp = BigDecimal.ONE, hadPower = false, usedPowerOfPower = false)
            }
            is PowerNode.ParenToken -> {
                if (node.inner.size == 1) extractBaseExp(node.inner.first()) else null
            }
            is PowerNode.PowerToken -> {
                val expBd = evalSimpleBd(node.exponent) ?: return null
                // Check for power of a power: (a^m)^n -> a^(m*n)
                if (node.base.size == 1 && node.base.first() is PowerNode.ParenToken) {
                    val paren = node.base.first() as PowerNode.ParenToken
                    if (paren.inner.size == 1 && paren.inner.first() is PowerNode.PowerToken) {
                        val innerPair = extractBaseExp(paren.inner.first()) ?: return null
                        return BaseExpPair(
                            base = innerPair.base,
                            exp = innerPair.exp.multiply(expBd, mc),
                            hadPower = true,
                            usedPowerOfPower = true
                        )
                    }
                }
                val baseBd = evalSimpleBd(node.base) ?: return null
                BaseExpPair(base = baseBd, exp = expBd, hadPower = true, usedPowerOfPower = false)
            }
            is PowerNode.OperatorToken -> null
        }
    }

    private fun evalSimpleBd(nodes: List<PowerNode>): BigDecimal? {
        if (nodes.isEmpty()) return null
        return runCatching {
            val parser = SeqParser(nodes)
            val v = parser.parseAddSub()
            if (parser.isAtEnd()) v.bd else null
        }.getOrNull()
    }

    fun toPlainText(nodes: List<PowerNode>): String {
        return nodes.joinToString("") { node ->
            when (node) {
                is PowerNode.NumberToken -> node.text
                is PowerNode.OperatorToken -> " ${node.op.symbol} "
                is PowerNode.PowerToken -> {
                    val b = toPlainText(node.base)
                    val e = toPlainText(node.exponent)
                    val wrapB = if (node.base.size > 1) "($b)" else b
                    val wrapE = if (node.exponent.size > 1) "($e)" else e
                    "$wrapB^$wrapE"
                }
                is PowerNode.ParenToken -> "(${toPlainText(node.inner)})"
            }
        }.trim()
    }

    fun cloneNodes(nodes: List<PowerNode>): MutableList<PowerNode> {
        return nodes.map { node ->
            when (node) {
                is PowerNode.NumberToken -> node.copy()
                is PowerNode.OperatorToken -> node.copy()
                is PowerNode.PowerToken -> PowerNode.PowerToken(
                    base = cloneNodes(node.base),
                    exponent = cloneNodes(node.exponent)
                )
                is PowerNode.ParenToken -> PowerNode.ParenToken(
                    inner = cloneNodes(node.inner)
                )
            }
        }.toMutableList()
    }
}

@JsonClass(generateAdapter = true)
data class PowerNodeDto(
    val type: String,
    val text: String? = null,
    val op: String? = null,
    val base: List<PowerNodeDto>? = null,
    val exponent: List<PowerNodeDto>? = null,
    val inner: List<PowerNodeDto>? = null
)

object PowerSerializer {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, PowerNodeDto::class.java)
    private val adapter = moshi.adapter<List<PowerNodeDto>>(listType)

    fun toJson(nodes: List<PowerNode>): String = adapter.toJson(nodes.map { it.toDto() })

    fun fromJson(json: String?): MutableList<PowerNode>? {
        if (json.isNullOrBlank()) return null
        return try {
            adapter.fromJson(json)?.map { it.toDomain() }?.toMutableList()
        } catch (_: Exception) {
            null
        }
    }

    private fun PowerNode.toDto(): PowerNodeDto = when (this) {
        is PowerNode.NumberToken -> PowerNodeDto(type = "num", text = text)
        is PowerNode.OperatorToken -> PowerNodeDto(type = "op", op = op.name)
        is PowerNode.PowerToken -> PowerNodeDto(
            type = "pow",
            base = base.map { it.toDto() },
            exponent = exponent.map { it.toDto() }
        )
        is PowerNode.ParenToken -> PowerNodeDto(
            type = "paren",
            inner = inner.map { it.toDto() }
        )
    }

    private fun PowerNodeDto.toDomain(): PowerNode = when (type) {
        "num" -> PowerNode.NumberToken(text ?: "0")
        "op" -> PowerNode.OperatorToken(PowerOp.valueOf(op ?: "MUL"))
        "pow" -> PowerNode.PowerToken(
            base = base?.map { it.toDomain() }?.toMutableList() ?: mutableListOf(),
            exponent = exponent?.map { it.toDomain() }?.toMutableList() ?: mutableListOf()
        )
        "paren" -> PowerNode.ParenToken(
            inner = inner?.map { it.toDomain() }?.toMutableList() ?: mutableListOf()
        )
        else -> PowerNode.NumberToken("0")
    }
}
