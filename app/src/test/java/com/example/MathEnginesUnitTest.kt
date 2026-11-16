package com.example

import com.example.domain.math.ExactFraction
import com.example.domain.math.FractionEvalException
import com.example.domain.math.FractionEvaluator
import com.example.domain.math.FractionNode
import com.example.domain.math.FractionOp
import com.example.domain.math.FractionSerializer
import com.example.domain.math.NumberFormatter
import com.example.domain.math.StandardScientificEngine
import com.example.domain.model.AngleUnit
import com.example.domain.model.AppSettings
import com.example.domain.model.DecimalSeparator
import com.example.domain.model.EvalResult
import com.example.domain.model.GroupingStyle
import com.example.domain.physics.PhysicsCatalog
import com.example.domain.physics.PhysicsCategory
import com.example.domain.physics.PhysicsEvalException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class MathEnginesUnitTest {

    private val engine = StandardScientificEngine()
    private val settings = AppSettings(
        decimalPrecision = 8,
        decimalSeparator = DecimalSeparator.DOT,
        groupingStyle = GroupingStyle.NONE
    )

    @Test
    fun `standard decimal addition avoids floating point drift`() {
        val res = engine.evaluate("0.1+0.2", AngleUnit.DEG, settings)
        assertTrue(res is EvalResult.Success)
        assertEquals("0.3", (res as EvalResult.Success).numericString)
    }

    @Test
    fun `standard precedence parentheses and percentages`() {
        val r1 = engine.evaluate("2+3×4", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("14", r1.numericString)

        val r2 = engine.evaluate("(2+3)×4^2", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("80", r2.numericString)

        val r3 = engine.evaluate("100+10%", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("110", r3.numericString)

        val r4 = engine.evaluate("100−20%", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("80", r4.numericString)

        val r5 = engine.evaluate("200×15%", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("30", r5.numericString)
    }

    @Test
    fun `scientific functions trig logs roots factorials`() {
        val sin30 = engine.evaluate("sin(30)", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("0.5", sin30.numericString)

        val asinHalf = engine.evaluate("sin⁻¹(0.5)", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("30", asinHalf.numericString)

        val sinPiHalf = engine.evaluate("sin(π÷2)", AngleUnit.RAD, settings) as EvalResult.Success
        assertEquals("1", sinPiHalf.numericString)

        val logRes = engine.evaluate("log(1000)", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("3", logRes.numericString)

        val lnRes = engine.evaluate("ln(e)", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("1", lnRes.numericString)

        val factRes = engine.evaluate("5!", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("120", factRes.numericString)

        val absRes = engine.evaluate("abs(−42)", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("42", absRes.numericString)

        val rootRes = engine.evaluate("3ʸ√(27)", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("3", rootRes.numericString)

        val tenPow = engine.evaluate("10^(3)", AngleUnit.DEG, settings) as EvalResult.Success
        assertEquals("1000", tenPow.numericString)
    }

    @Test
    fun `standard and scientific errors are reported in Ukrainian`() {
        val divZero = engine.evaluate("5÷0", AngleUnit.DEG, settings)
        assertTrue(divZero is EvalResult.Error)

        val negSqrt = engine.evaluate("√(−9)", AngleUnit.DEG, settings)
        assertTrue(negSqrt is EvalResult.Error)

        val tan90 = engine.evaluate("tan(90)", AngleUnit.DEG, settings)
        assertTrue(tan90 is EvalResult.Error)
    }

    @Test
    fun `exact fraction arithmetic matches prompt examples`() {
        // 1/2 + 1/4 = 3/4
        val f1 = ExactFraction.of(1, 2) + ExactFraction.of(1, 4)
        assertEquals(ExactFraction.of(3, 4), f1)

        // 2/3 * 3/5 = 2/5
        val f2 = ExactFraction.of(2, 3) * ExactFraction.of(3, 5)
        assertEquals(ExactFraction.of(2, 5), f2)

        // 3/4 / 1/2 = 3/2 = 1 1/2
        val f3 = ExactFraction.of(3, 4) / ExactFraction.of(1, 2)
        assertEquals(ExactFraction.of(3, 2), f3)
        val mixed = f3.toMixed()
        assertEquals(BigInteger.ONE, mixed.whole)
        assertEquals(BigInteger.ONE, mixed.numerator)
        assertEquals(BigInteger.valueOf(2), mixed.denominator)
    }

    @Test
    fun `decimal to fraction and mixed conversions are exact`() {
        assertEquals(ExactFraction.of(3, 4), ExactFraction.fromDecimalString("0.75"))
        assertEquals(ExactFraction.of(5, 4), ExactFraction.fromDecimalString("1.25"))
        val fromMixed = ExactFraction.fromMixed(false, BigInteger.valueOf(2), BigInteger.ONE, BigInteger.valueOf(3))
        assertEquals(ExactFraction.of(7, 3), fromMixed)
    }

    @Test
    fun `nested fraction AST evaluation and serialization`() {
        // (1/2 + 1/4) / (3/8) = (3/4) / (3/8) = 2
        val innerSum = FractionNode.ParenToken(
            mutableListOf(
                FractionNode.FractionToken(
                    mutableListOf(FractionNode.NumberToken("1")),
                    mutableListOf(FractionNode.NumberToken("2"))
                ),
                FractionNode.OperatorToken(FractionOp.ADD),
                FractionNode.FractionToken(
                    mutableListOf(FractionNode.NumberToken("1")),
                    mutableListOf(FractionNode.NumberToken("4"))
                )
            )
        )
        val nested = listOf(
            FractionNode.FractionToken(
                numerator = mutableListOf(innerSum),
                denominator = mutableListOf(
                    FractionNode.FractionToken(
                        mutableListOf(FractionNode.NumberToken("3")),
                        mutableListOf(FractionNode.NumberToken("8"))
                    )
                )
            )
        )
        val eval = FractionEvaluator.evaluate(nested)
        assertEquals(ExactFraction.of(2, 1), eval.reduced)

        val json = FractionSerializer.toJson(nested)
        val restored = FractionSerializer.fromJson(json)
        assertNotNull(restored)
        assertEquals(ExactFraction.of(2, 1), FractionEvaluator.evaluate(restored!!).reduced)
    }

    @Test(expected = FractionEvalException::class)
    fun `fraction division by zero throws Ukrainian exception`() {
        val expr = listOf(
            FractionNode.FractionToken(
                mutableListOf(FractionNode.NumberToken("3")),
                mutableListOf(FractionNode.NumberToken("0"))
            )
        )
        FractionEvaluator.evaluate(expr)
    }

    @Test
    fun `power multiplication division and power of power simplify and evaluate accurately`() {
        // 2^3 * 2^4 = 2^7 = 128
        val mulSameBase = listOf(
            com.example.domain.math.PowerNode.PowerToken(
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("2")),
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("3"))
            ),
            com.example.domain.math.PowerNode.OperatorToken(com.example.domain.math.PowerOp.MUL),
            com.example.domain.math.PowerNode.PowerToken(
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("2")),
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("4"))
            )
        )
        val mulEval = com.example.domain.math.PowerEvaluator.evaluate(mulSameBase, settings)
        assertEquals("128", mulEval.rawNumeric)
        assertEquals("2", mulEval.simplifiedPower?.baseText)
        assertEquals("7", mulEval.simplifiedPower?.exponentText)

        // 5^6 / 5^2 = 5^4 = 625
        val divSameBase = listOf(
            com.example.domain.math.PowerNode.PowerToken(
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("5")),
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("6"))
            ),
            com.example.domain.math.PowerNode.OperatorToken(com.example.domain.math.PowerOp.DIV),
            com.example.domain.math.PowerNode.PowerToken(
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("5")),
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("2"))
            )
        )
        val divEval = com.example.domain.math.PowerEvaluator.evaluate(divSameBase, settings)
        assertEquals("625", divEval.rawNumeric)
        assertEquals("5", divEval.simplifiedPower?.baseText)
        assertEquals("4", divEval.simplifiedPower?.exponentText)

        // (3^2)^3 = 3^6 = 729
        val powOfPow = listOf(
            com.example.domain.math.PowerNode.PowerToken(
                base = mutableListOf(
                    com.example.domain.math.PowerNode.ParenToken(
                        mutableListOf(
                            com.example.domain.math.PowerNode.PowerToken(
                                mutableListOf(com.example.domain.math.PowerNode.NumberToken("3")),
                                mutableListOf(com.example.domain.math.PowerNode.NumberToken("2"))
                            )
                        )
                    )
                ),
                exponent = mutableListOf(com.example.domain.math.PowerNode.NumberToken("3"))
            )
        )
        val powPowEval = com.example.domain.math.PowerEvaluator.evaluate(powOfPow, settings)
        assertEquals("729", powPowEval.rawNumeric)
        assertEquals("3", powPowEval.simplifiedPower?.baseText)
        assertEquals("6", powPowEval.simplifiedPower?.exponentText)

        // 2^-3 = 0.125 (1/8)
        val negExp = listOf(
            com.example.domain.math.PowerNode.PowerToken(
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("2")),
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("-3"))
            )
        )
        val negEval = com.example.domain.math.PowerEvaluator.evaluate(negExp, settings)
        assertEquals("0.125", negEval.rawNumeric)
        assertEquals("1/8", negEval.fractionHint)
    }

    @Test(expected = com.example.domain.math.PowerEvalException::class)
    fun `power zero to negative exponent throws error`() {
        val expr = listOf(
            com.example.domain.math.PowerNode.PowerToken(
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("0")),
                mutableListOf(com.example.domain.math.PowerNode.NumberToken("-2"))
            )
        )
        com.example.domain.math.PowerEvaluator.evaluate(expr, settings)
    }

    @Test
    fun `physics catalog search and bidirectional speed time distance solver`() {
        val searchSpeed = PhysicsCatalog.filter("швидкість час", null, false, emptySet())
        assertTrue(searchSpeed.any { it.id == "speed_distance_time" })

        val searchOhm = PhysicsCatalog.filter("закон ома", PhysicsCategory.ELECTRICITY, false, emptySet())
        assertTrue(searchOhm.any { it.id == "ohms_law" })

        val searchOhmFormula = PhysicsCatalog.filter("i = u / r", PhysicsCategory.ELECTRICITY, false, emptySet())
        assertEquals(1, searchOhmFormula.size)
        assertEquals("ohms_law", searchOhmFormula.first().id)

        val speedFormula = PhysicsCatalog.allFormulas.first { it.id == "speed_distance_time" }
        val sVar = speedFormula.varById("s")
        val tVar = speedFormula.varById("t")
        val vVar = speedFormula.varById("v")

        // 180 km in 2 hours -> 25 m/s -> 90 km/h
        val kmUnit = sVar.units.first { it.label == "км" }
        val hrUnit = tVar.units.first { it.label == "год" }
        val kmhUnit = vVar.units.first { it.label == "км/год" }

        val targetV = speedFormula.solveTargets.first { it.targetVarId == "v" }
        val vSi = targetV.computeSi(mapOf("s" to kmUnit.toSi(180.0), "t" to hrUnit.toSi(2.0)))
        assertEquals(25.0, vSi, 1e-6)
        assertEquals(90.0, kmhUnit.fromSi(vSi), 1e-6)

        // Solve for time: t = s / v -> 100 m / 20 m/s = 5 s
        val targetT = speedFormula.solveTargets.first { it.targetVarId == "t" }
        val tSi = targetT.computeSi(mapOf("s" to 100.0, "v" to 20.0))
        assertEquals(5.0, tSi, 1e-9)

        // Planck photon energy formatting preserves tiny scientific quantities
        val photon = PhysicsCatalog.allFormulas.first { it.id == "photon_energy" }
        val eSi = photon.solveTargets.first { it.targetVarId == "E" }.computeSi(mapOf("nu" to 5e14))
        val formatted = NumberFormatter.formatPhysicsDouble(eSi, settings)
        assertTrue(formatted.contains("×10^-19"))
    }

    @Test(expected = PhysicsEvalException::class)
    fun `physics solver rejects zero time when computing speed`() {
        val speedFormula = PhysicsCatalog.allFormulas.first { it.id == "speed_distance_time" }
        val targetV = speedFormula.solveTargets.first { it.targetVarId == "v" }
        targetV.computeSi(mapOf("s" to 100.0, "t" to 0.0))
    }
}
