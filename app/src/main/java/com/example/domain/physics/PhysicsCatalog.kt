package com.example.domain.physics

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class PhysicsEvalException(val messageUkr: String) : RuntimeException(messageUkr)

enum class PhysicsCategory(val labelUkr: String) {
    KINEMATICS("Кінематика"),
    DYNAMICS("Динаміка і статика"),
    ENERGY("Робота й енергія"),
    THERMO("Термодинаміка"),
    ELECTRICITY("Електрика і магнетизм"),
    WAVES_OPTICS("Хвилі й оптика")
}

data class PhysicsUnitOption(
    val label: String,
    val toSiFactor: Double,
    val offsetToSi: Double = 0.0
) {
    fun toSi(value: Double): Double = value * toSiFactor + offsetToSi
    fun fromSi(siValue: Double): Double = (siValue - offsetToSi) / toSiFactor
}

data class PhysicsVar(
    val id: String,
    val symbol: String,
    val nameUkr: String,
    val units: List<PhysicsUnitOption>,
    val hint: String = ""
)

data class PhysicsSolveTarget(
    val targetVarId: String,
    val targetLabelUkr: String,
    val equationDisplay: String,
    val requiredVarIds: List<String>,
    val computeSi: (Map<String, Double>) -> Double
)

data class PhysicsFormula(
    val id: String,
    val titleUkr: String,
    val formulaDisplay: String,
    val category: PhysicsCategory,
    val descriptionUkr: String,
    val searchKeywords: String,
    val variables: List<PhysicsVar>,
    val constantsNote: String? = null,
    val solveTargets: List<PhysicsSolveTarget>
) {
    fun varById(id: String): PhysicsVar = variables.first { it.id == id }
}

object PhysicsCatalog {

    private const val G_EARTH = 9.80665
    private const val G_UNIV = 6.67430e-11
    private const val R_GAS = 8.3144626
    private const val K_COULOMB = 8.98755179e9
    private const val H_PLANCK = 6.62607015e-34
    private const val C_LIGHT = 299792458.0
    private const val EV_TO_J = 1.602176634e-19

    private val lengthUnits = listOf(
        PhysicsUnitOption("м", 1.0),
        PhysicsUnitOption("км", 1000.0),
        PhysicsUnitOption("см", 0.01),
        PhysicsUnitOption("мм", 0.001)
    )
    private val timeUnits = listOf(
        PhysicsUnitOption("с", 1.0),
        PhysicsUnitOption("хв", 60.0),
        PhysicsUnitOption("год", 3600.0),
        PhysicsUnitOption("мс", 0.001)
    )
    private val speedUnits = listOf(
        PhysicsUnitOption("м/с", 1.0),
        PhysicsUnitOption("км/год", 1.0 / 3.6)
    )
    private val accelUnits = listOf(PhysicsUnitOption("м/с²", 1.0))
    private val massUnits = listOf(
        PhysicsUnitOption("кг", 1.0),
        PhysicsUnitOption("г", 0.001),
        PhysicsUnitOption("т", 1000.0)
    )
    private val forceUnits = listOf(
        PhysicsUnitOption("Н", 1.0),
        PhysicsUnitOption("кН", 1000.0),
        PhysicsUnitOption("мН", 0.001)
    )
    private val energyUnits = listOf(
        PhysicsUnitOption("Дж", 1.0),
        PhysicsUnitOption("кДж", 1000.0),
        PhysicsUnitOption("МДж", 1_000_000.0)
    )
    private val powerUnits = listOf(
        PhysicsUnitOption("Вт", 1.0),
        PhysicsUnitOption("кВт", 1000.0),
        PhysicsUnitOption("МВт", 1_000_000.0)
    )
    private val pressureUnits = listOf(
        PhysicsUnitOption("Па", 1.0),
        PhysicsUnitOption("кПа", 1000.0),
        PhysicsUnitOption("МПа", 1_000_000.0),
        PhysicsUnitOption("атм", 101325.0)
    )
    private val densityUnits = listOf(
        PhysicsUnitOption("кг/м³", 1.0),
        PhysicsUnitOption("г/см³", 1000.0)
    )
    private val volumeUnits = listOf(
        PhysicsUnitOption("м³", 1.0),
        PhysicsUnitOption("л", 0.001),
        PhysicsUnitOption("см³", 1e-6)
    )
    private val areaUnits = listOf(
        PhysicsUnitOption("м²", 1.0),
        PhysicsUnitOption("см²", 1e-4),
        PhysicsUnitOption("мм²", 1e-6)
    )
    private val currentUnits = listOf(
        PhysicsUnitOption("А", 1.0),
        PhysicsUnitOption("мА", 0.001),
        PhysicsUnitOption("кА", 1000.0)
    )
    private val voltageUnits = listOf(
        PhysicsUnitOption("В", 1.0),
        PhysicsUnitOption("мВ", 0.001),
        PhysicsUnitOption("кВ", 1000.0)
    )
    private val resistanceUnits = listOf(
        PhysicsUnitOption("Ом", 1.0),
        PhysicsUnitOption("кОм", 1000.0),
        PhysicsUnitOption("МОм", 1_000_000.0)
    )
    private val chargeUnits = listOf(
        PhysicsUnitOption("Кл", 1.0),
        PhysicsUnitOption("мКл", 0.001),
        PhysicsUnitOption("мкКл", 1e-6)
    )
    private val freqUnits = listOf(
        PhysicsUnitOption("Гц", 1.0),
        PhysicsUnitOption("кГц", 1000.0),
        PhysicsUnitOption("МГц", 1_000_000.0),
        PhysicsUnitOption("ТГц", 1e12)
    )

    private fun requireNonZero(v: Double, label: String): Double {
        if (abs(v) < 1e-300) throw PhysicsEvalException("Значення «$label» не може дорівнювати нулю")
        return v
    }

    private fun requireNonNegative(v: Double, label: String): Double {
        if (v < 0.0) throw PhysicsEvalException("Значення «$label» не може бути від’ємним")
        return v
    }

    val allFormulas: List<PhysicsFormula> = listOf(
        // ==================== КІНЕМАТИКА ====================
        PhysicsFormula(
            id = "speed_distance_time",
            titleUkr = "Швидкість, шлях і час",
            formulaDisplay = "v = s / t",
            category = PhysicsCategory.KINEMATICS,
            descriptionUkr = "Рівномірний прямолінійний рух: зв’язок між швидкістю, пройденим шляхом та часом руху.",
            searchKeywords = "швидкість час шлях відстань рух кілометри метри секунди години s v t",
            variables = listOf(
                PhysicsVar("v", "v", "Швидкість", speedUnits, "20"),
                PhysicsVar("s", "s", "Шлях (відстань)", lengthUnits, "100"),
                PhysicsVar("t", "t", "Час", timeUnits, "5")
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("v", "Швидкість (v)", "v = s ÷ t", listOf("s", "t")) {
                    it.getValue("s") / requireNonZero(it.getValue("t"), "Час")
                },
                PhysicsSolveTarget("s", "Шлях (s)", "s = v × t", listOf("v", "t")) {
                    it.getValue("v") * it.getValue("t")
                },
                PhysicsSolveTarget("t", "Час (t)", "t = s ÷ v", listOf("s", "v")) {
                    it.getValue("s") / requireNonZero(it.getValue("v"), "Швидкість")
                }
            )
        ),
        PhysicsFormula(
            id = "uniform_accel",
            titleUkr = "Прискорення при рівноприскореному русі",
            formulaDisplay = "a = (v − v₀) / t",
            category = PhysicsCategory.KINEMATICS,
            descriptionUkr = "Зміна швидкості за одиницю часу: знаходження прискорення, кінцевої або початкової швидкості чи часу.",
            searchKeywords = "прискорення швидкість початкова кінцева час рух розгін гальмування a v v0 t",
            variables = listOf(
                PhysicsVar("a", "a", "Прискорення", accelUnits, "2"),
                PhysicsVar("v", "v", "Кінцева швидкість", speedUnits, "20"),
                PhysicsVar("v0", "v₀", "Початкова швидкість", speedUnits, "0"),
                PhysicsVar("t", "t", "Час", timeUnits, "10")
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("a", "Прискорення (a)", "a = (v − v₀) ÷ t", listOf("v", "v0", "t")) {
                    (it.getValue("v") - it.getValue("v0")) / requireNonZero(it.getValue("t"), "Час")
                },
                PhysicsSolveTarget("v", "Кінцева швидкість (v)", "v = v₀ + a × t", listOf("v0", "a", "t")) {
                    it.getValue("v0") + it.getValue("a") * it.getValue("t")
                },
                PhysicsSolveTarget("v0", "Початкова швидкість (v₀)", "v₀ = v − a × t", listOf("v", "a", "t")) {
                    it.getValue("v") - it.getValue("a") * it.getValue("t")
                },
                PhysicsSolveTarget("t", "Час (t)", "t = (v − v₀) ÷ a", listOf("v", "v0", "a")) {
                    (it.getValue("v") - it.getValue("v0")) / requireNonZero(it.getValue("a"), "Прискорення")
                }
            )
        ),
        PhysicsFormula(
            id = "accel_distance",
            titleUkr = "Шлях при рівноприскореному русі",
            formulaDisplay = "s = v₀t + at² / 2",
            category = PhysicsCategory.KINEMATICS,
            descriptionUkr = "Переміщення або шлях тіла, що рухається з постійним прискоренням.",
            searchKeywords = "шлях відстань прискорення час початкова швидкість переміщення s v0 a t",
            variables = listOf(
                PhysicsVar("s", "s", "Шлях", lengthUnits),
                PhysicsVar("v0", "v₀", "Початкова швидкість", speedUnits),
                PhysicsVar("a", "a", "Прискорення", accelUnits),
                PhysicsVar("t", "t", "Час", timeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("s", "Шлях (s)", "s = v₀·t + a·t² ÷ 2", listOf("v0", "a", "t")) {
                    val t = it.getValue("t")
                    it.getValue("v0") * t + 0.5 * it.getValue("a") * t * t
                },
                PhysicsSolveTarget("a", "Прискорення (a)", "a = 2·(s − v₀·t) ÷ t²", listOf("s", "v0", "t")) {
                    val t = requireNonZero(it.getValue("t"), "Час")
                    2.0 * (it.getValue("s") - it.getValue("v0") * t) / (t * t)
                },
                PhysicsSolveTarget("v0", "Початкова швидкість (v₀)", "v₀ = (s − a·t²/2) ÷ t", listOf("s", "a", "t")) {
                    val t = requireNonZero(it.getValue("t"), "Час")
                    (it.getValue("s") - 0.5 * it.getValue("a") * t * t) / t
                }
            )
        ),
        PhysicsFormula(
            id = "free_fall_height",
            titleUkr = "Вільне падіння: висота і час",
            formulaDisplay = "h = gt² / 2",
            category = PhysicsCategory.KINEMATICS,
            descriptionUkr = "Висота падіння тіла без початкової швидкості або час падіння з висоти h.",
            searchKeywords = "вільне падіння висота час гравітація тяжіння h g t",
            constantsNote = "Прискорення вільного падіння g = 9,81 м/с²",
            variables = listOf(
                PhysicsVar("h", "h", "Висота", lengthUnits),
                PhysicsVar("t", "t", "Час падіння", timeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("h", "Висота (h)", "h = g · t² ÷ 2", listOf("t")) {
                    val t = it.getValue("t")
                    0.5 * G_EARTH * t * t
                },
                PhysicsSolveTarget("t", "Час падіння (t)", "t = √(2·h ÷ g)", listOf("h")) {
                    val h = requireNonNegative(it.getValue("h"), "Висота")
                    sqrt(2.0 * h / G_EARTH)
                }
            )
        ),
        PhysicsFormula(
            id = "free_fall_speed",
            titleUkr = "Швидкість при вільному падінні",
            formulaDisplay = "v = √(2gh)",
            category = PhysicsCategory.KINEMATICS,
            descriptionUkr = "Швидкість тіла наприкінці падіння з висоти h без опору повітря.",
            searchKeywords = "вільне падіння швидкість висота удар v g h",
            constantsNote = "g = 9,81 м/с²",
            variables = listOf(
                PhysicsVar("v", "v", "Швидкість", speedUnits),
                PhysicsVar("h", "h", "Висота", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("v", "Швидкість (v)", "v = √(2 · g · h)", listOf("h")) {
                    sqrt(2.0 * G_EARTH * requireNonNegative(it.getValue("h"), "Висота"))
                },
                PhysicsSolveTarget("h", "Висота (h)", "h = v² ÷ (2 · g)", listOf("v")) {
                    val v = it.getValue("v")
                    (v * v) / (2.0 * G_EARTH)
                }
            )
        ),
        PhysicsFormula(
            id = "circular_speed",
            titleUkr = "Лінійна швидкість руху по колу",
            formulaDisplay = "v = 2πR / T",
            category = PhysicsCategory.KINEMATICS,
            descriptionUkr = "Зв’язок лінійної швидкості, радіуса кола та періоду обертання.",
            searchKeywords = "коло обертання швидкість радіус період орбіта v R T",
            variables = listOf(
                PhysicsVar("v", "v", "Лінійна швидкість", speedUnits),
                PhysicsVar("R", "R", "Радіус кола", lengthUnits),
                PhysicsVar("T", "T", "Період обертання", timeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("v", "Швидкість (v)", "v = 2π·R ÷ T", listOf("R", "T")) {
                    2.0 * PI * it.getValue("R") / requireNonZero(it.getValue("T"), "Період")
                },
                PhysicsSolveTarget("R", "Радіус (R)", "R = v·T ÷ (2π)", listOf("v", "T")) {
                    it.getValue("v") * it.getValue("T") / (2.0 * PI)
                },
                PhysicsSolveTarget("T", "Період (T)", "T = 2π·R ÷ v", listOf("R", "v")) {
                    2.0 * PI * it.getValue("R") / requireNonZero(it.getValue("v"), "Швидкість")
                }
            )
        ),
        PhysicsFormula(
            id = "centripetal_accel",
            titleUkr = "Доцентрове прискорення",
            formulaDisplay = "a = v² / R",
            category = PhysicsCategory.KINEMATICS,
            descriptionUkr = "Прискорення тіла під час рівномірного руху по колу радіусом R.",
            searchKeywords = "доцентрове прискорення коло поворот швидкість радіус a v R",
            variables = listOf(
                PhysicsVar("a", "a", "Доцентрове прискорення", accelUnits),
                PhysicsVar("v", "v", "Швидкість", speedUnits),
                PhysicsVar("R", "R", "Радіус кола", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("a", "Прискорення (a)", "a = v² ÷ R", listOf("v", "R")) {
                    val v = it.getValue("v")
                    (v * v) / requireNonZero(it.getValue("R"), "Радіус")
                },
                PhysicsSolveTarget("v", "Швидкість (v)", "v = √(a · R)", listOf("a", "R")) {
                    sqrt(requireNonNegative(it.getValue("a") * it.getValue("R"), "a · R"))
                },
                PhysicsSolveTarget("R", "Радіус (R)", "R = v² ÷ a", listOf("v", "a")) {
                    val v = it.getValue("v")
                    (v * v) / requireNonZero(it.getValue("a"), "Прискорення")
                }
            )
        ),

        // ==================== ДИНАМІКА І СТАТИКА ====================
        PhysicsFormula(
            id = "newton_second",
            titleUkr = "Другий закон Ньютона",
            formulaDisplay = "F = m · a",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Основний закон динаміки: сила дорівнює добутку маси тіла на його прискорення.",
            searchKeywords = "сила ньютон маса прискорення динаміка F m a",
            variables = listOf(
                PhysicsVar("F", "F", "Сила", forceUnits),
                PhysicsVar("m", "m", "Маса", massUnits),
                PhysicsVar("a", "a", "Прискорення", accelUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Сила (F)", "F = m × a", listOf("m", "a")) {
                    it.getValue("m") * it.getValue("a")
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = F ÷ a", listOf("F", "a")) {
                    it.getValue("F") / requireNonZero(it.getValue("a"), "Прискорення")
                },
                PhysicsSolveTarget("a", "Прискорення (a)", "a = F ÷ m", listOf("F", "m")) {
                    it.getValue("F") / requireNonZero(it.getValue("m"), "Маса")
                }
            )
        ),
        PhysicsFormula(
            id = "gravity_force",
            titleUkr = "Сила тяжіння",
            formulaDisplay = "F = m · g",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Сила, з якою Земля притягує тіло масою m.",
            searchKeywords = "сила тяжіння вага маса гравітація F m g",
            constantsNote = "g = 9,81 м/с²",
            variables = listOf(
                PhysicsVar("F", "F", "Сила тяжіння", forceUnits),
                PhysicsVar("m", "m", "Маса тіла", massUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Сила тяжіння (F)", "F = m × g", listOf("m")) {
                    it.getValue("m") * G_EARTH
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = F ÷ g", listOf("F")) {
                    it.getValue("F") / G_EARTH
                }
            )
        ),
        PhysicsFormula(
            id = "friction_force",
            titleUkr = "Сила тертя ковзання",
            formulaDisplay = "F = μ · N",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Сила тертя залежить від коефіцієнта тертя та сили нормальної реакції опори.",
            searchKeywords = "тертя коефіцієнт реакція опори сила ковзання F mu N",
            variables = listOf(
                PhysicsVar("F", "F", "Сила тертя", forceUnits),
                PhysicsVar("mu", "μ", "Коефіцієнт тертя", listOf(PhysicsUnitOption("—", 1.0))),
                PhysicsVar("N", "N", "Сила реакції опори", forceUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Сила тертя (F)", "F = μ × N", listOf("mu", "N")) {
                    it.getValue("mu") * it.getValue("N")
                },
                PhysicsSolveTarget("mu", "Коефіцієнт тертя (μ)", "μ = F ÷ N", listOf("F", "N")) {
                    it.getValue("F") / requireNonZero(it.getValue("N"), "Сила опори")
                },
                PhysicsSolveTarget("N", "Сила реакції (N)", "N = F ÷ μ", listOf("F", "mu")) {
                    it.getValue("F") / requireNonZero(it.getValue("mu"), "Коефіцієнт тертя")
                }
            )
        ),
        PhysicsFormula(
            id = "hookes_law",
            titleUkr = "Закон Гука (сила пружності)",
            formulaDisplay = "F = k · x",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Сила пружності пружини пропорційна її жорсткості та абсолютному видовженню.",
            searchKeywords = "гук пружина пружність жорсткість деформація видовження F k x",
            variables = listOf(
                PhysicsVar("F", "F", "Сила пружності", forceUnits),
                PhysicsVar("k", "k", "Жорсткість", listOf(PhysicsUnitOption("Н/м", 1.0), PhysicsUnitOption("кН/м", 1000.0))),
                PhysicsVar("x", "x", "Видовження", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Сила (F)", "F = k × x", listOf("k", "x")) {
                    it.getValue("k") * it.getValue("x")
                },
                PhysicsSolveTarget("k", "Жорсткість (k)", "k = F ÷ x", listOf("F", "x")) {
                    it.getValue("F") / requireNonZero(it.getValue("x"), "Видовження")
                },
                PhysicsSolveTarget("x", "Видовження (x)", "x = F ÷ k", listOf("F", "k")) {
                    it.getValue("F") / requireNonZero(it.getValue("k"), "Жорсткість")
                }
            )
        ),
        PhysicsFormula(
            id = "momentum",
            titleUkr = "Імпульс тіла",
            formulaDisplay = "p = m · v",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Добуток маси тіла на його швидкість руху.",
            searchKeywords = "імпульс кількість руху маса швидкість удар p m v",
            variables = listOf(
                PhysicsVar("p", "p", "Імпульс", listOf(PhysicsUnitOption("кг·м/с", 1.0))),
                PhysicsVar("m", "m", "Маса", massUnits),
                PhysicsVar("v", "v", "Швидкість", speedUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("p", "Імпульс (p)", "p = m × v", listOf("m", "v")) {
                    it.getValue("m") * it.getValue("v")
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = p ÷ v", listOf("p", "v")) {
                    it.getValue("p") / requireNonZero(it.getValue("v"), "Швидкість")
                },
                PhysicsSolveTarget("v", "Швидкість (v)", "v = p ÷ m", listOf("p", "m")) {
                    it.getValue("p") / requireNonZero(it.getValue("m"), "Маса")
                }
            )
        ),
        PhysicsFormula(
            id = "density",
            titleUkr = "Густина речовини",
            formulaDisplay = "ρ = m / V",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Маса одиниці об’єму речовини.",
            searchKeywords = "густина маса об’єм речовина rho m V",
            variables = listOf(
                PhysicsVar("rho", "ρ", "Густина", densityUnits),
                PhysicsVar("m", "m", "Маса", massUnits),
                PhysicsVar("V", "V", "Об’єм", volumeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("rho", "Густина (ρ)", "ρ = m ÷ V", listOf("m", "V")) {
                    it.getValue("m") / requireNonZero(it.getValue("V"), "Об’єм")
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = ρ × V", listOf("rho", "V")) {
                    it.getValue("rho") * it.getValue("V")
                },
                PhysicsSolveTarget("V", "Об’єм (V)", "V = m ÷ ρ", listOf("m", "rho")) {
                    it.getValue("m") / requireNonZero(it.getValue("rho"), "Густина")
                }
            )
        ),
        PhysicsFormula(
            id = "pressure",
            titleUkr = "Механічний тиск",
            formulaDisplay = "p = F / S",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Тиск, який створює сила, що діє перпендикулярно на площу поверхні.",
            searchKeywords = "тиск сила площа паскаль атмосфера p F S",
            variables = listOf(
                PhysicsVar("p", "p", "Тиск", pressureUnits),
                PhysicsVar("F", "F", "Сила", forceUnits),
                PhysicsVar("S", "S", "Площа", areaUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("p", "Тиск (p)", "p = F ÷ S", listOf("F", "S")) {
                    it.getValue("F") / requireNonZero(it.getValue("S"), "Площа")
                },
                PhysicsSolveTarget("F", "Сила (F)", "F = p × S", listOf("p", "S")) {
                    it.getValue("p") * it.getValue("S")
                },
                PhysicsSolveTarget("S", "Площа (S)", "S = F ÷ p", listOf("F", "p")) {
                    it.getValue("F") / requireNonZero(it.getValue("p"), "Тиск")
                }
            )
        ),
        PhysicsFormula(
            id = "hydrostatic_pressure",
            titleUkr = "Гідростатичний тиск рідини",
            formulaDisplay = "p = ρ · g · h",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Тиск стовпа рідини густиною ρ на глибині h.",
            searchKeywords = "тиск рідини гідростатичний глибина вода густина p rho g h",
            constantsNote = "g = 9,81 м/с²",
            variables = listOf(
                PhysicsVar("p", "p", "Тиск", pressureUnits),
                PhysicsVar("rho", "ρ", "Густина рідини", densityUnits),
                PhysicsVar("h", "h", "Глибина (висота)", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("p", "Тиск (p)", "p = ρ × g × h", listOf("rho", "h")) {
                    it.getValue("rho") * G_EARTH * it.getValue("h")
                },
                PhysicsSolveTarget("h", "Глибина (h)", "h = p ÷ (ρ × g)", listOf("p", "rho")) {
                    it.getValue("p") / requireNonZero(it.getValue("rho") * G_EARTH, "Густина")
                },
                PhysicsSolveTarget("rho", "Густина (ρ)", "ρ = p ÷ (g × h)", listOf("p", "h")) {
                    it.getValue("p") / requireNonZero(G_EARTH * it.getValue("h"), "Глибина")
                }
            )
        ),
        PhysicsFormula(
            id = "archimedes",
            titleUkr = "Сила Архімеда",
            formulaDisplay = "Fₐ = ρ · g · V",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Виштовхувальна сила, що діє на тіло, занурене в рідину або газ.",
            searchKeywords = "архімед виштовхувальна сила плавання рідина об’єм густина F V rho",
            constantsNote = "g = 9,81 м/с²",
            variables = listOf(
                PhysicsVar("FA", "Fₐ", "Сила Архімеда", forceUnits),
                PhysicsVar("rho", "ρ", "Густина середовища", densityUnits),
                PhysicsVar("V", "V", "Занурений об’єм", volumeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("FA", "Сила Архімеда (Fₐ)", "Fₐ = ρ × g × V", listOf("rho", "V")) {
                    it.getValue("rho") * G_EARTH * it.getValue("V")
                },
                PhysicsSolveTarget("V", "Об’єм (V)", "V = Fₐ ÷ (ρ × g)", listOf("FA", "rho")) {
                    it.getValue("FA") / requireNonZero(it.getValue("rho") * G_EARTH, "Густина")
                },
                PhysicsSolveTarget("rho", "Густина (ρ)", "ρ = Fₐ ÷ (g × V)", listOf("FA", "V")) {
                    it.getValue("FA") / requireNonZero(G_EARTH * it.getValue("V"), "Об’єм")
                }
            )
        ),
        PhysicsFormula(
            id = "gravitation",
            titleUkr = "Закон всесвітнього тяжіння",
            formulaDisplay = "F = G · m₁m₂ / r²",
            category = PhysicsCategory.DYNAMICS,
            descriptionUkr = "Сила гравітаційного притягання між двома масами на відстані r.",
            searchKeywords = "гравітація всесвітнє тяжіння планети ньютон відстань маса G m1 m2 r",
            constantsNote = "G = 6,674×10⁻¹¹ Н·м²/кг²",
            variables = listOf(
                PhysicsVar("F", "F", "Сила тяжіння", forceUnits),
                PhysicsVar("m1", "m₁", "Маса 1", massUnits),
                PhysicsVar("m2", "m₂", "Маса 2", massUnits),
                PhysicsVar("r", "r", "Відстань", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Сила (F)", "F = G · m₁ · m₂ ÷ r²", listOf("m1", "m2", "r")) {
                    val r = requireNonZero(it.getValue("r"), "Відстань")
                    G_UNIV * it.getValue("m1") * it.getValue("m2") / (r * r)
                },
                PhysicsSolveTarget("r", "Відстань (r)", "r = √(G · m₁ · m₂ ÷ F)", listOf("m1", "m2", "F")) {
                    val f = requireNonZero(it.getValue("F"), "Сила")
                    sqrt(requireNonNegative(G_UNIV * it.getValue("m1") * it.getValue("m2") / f, "Підкореневий вираз"))
                }
            )
        ),

        // ==================== РОБОТА Й ЕНЕРГІЯ ====================
        PhysicsFormula(
            id = "mechanical_work",
            titleUkr = "Механічна робота",
            formulaDisplay = "A = F · s",
            category = PhysicsCategory.ENERGY,
            descriptionUkr = "Робота постійної сили на прямолінійній ділянці шляху.",
            searchKeywords = "робота сила шлях переміщення джоуль A F s",
            variables = listOf(
                PhysicsVar("A", "A", "Робота", energyUnits),
                PhysicsVar("F", "F", "Сила", forceUnits),
                PhysicsVar("s", "s", "Шлях", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("A", "Робота (A)", "A = F × s", listOf("F", "s")) {
                    it.getValue("F") * it.getValue("s")
                },
                PhysicsSolveTarget("F", "Сила (F)", "F = A ÷ s", listOf("A", "s")) {
                    it.getValue("A") / requireNonZero(it.getValue("s"), "Шлях")
                },
                PhysicsSolveTarget("s", "Шлях (s)", "s = A ÷ F", listOf("A", "F")) {
                    it.getValue("A") / requireNonZero(it.getValue("F"), "Сила")
                }
            )
        ),
        PhysicsFormula(
            id = "mechanical_power",
            titleUkr = "Потужність",
            formulaDisplay = "P = A / t",
            category = PhysicsCategory.ENERGY,
            descriptionUkr = "Швидкість виконання роботи: відношення роботи до часу її виконання.",
            searchKeywords = "потужність робота час ват кіловат швидкість виконання P A t N",
            variables = listOf(
                PhysicsVar("P", "P", "Потужність", powerUnits),
                PhysicsVar("A", "A", "Робота", energyUnits),
                PhysicsVar("t", "t", "Час", timeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("P", "Потужність (P)", "P = A ÷ t", listOf("A", "t")) {
                    it.getValue("A") / requireNonZero(it.getValue("t"), "Час")
                },
                PhysicsSolveTarget("A", "Робота (A)", "A = P × t", listOf("P", "t")) {
                    it.getValue("P") * it.getValue("t")
                },
                PhysicsSolveTarget("t", "Час (t)", "t = A ÷ P", listOf("A", "P")) {
                    it.getValue("A") / requireNonZero(it.getValue("P"), "Потужність")
                }
            )
        ),
        PhysicsFormula(
            id = "kinetic_energy",
            titleUkr = "Кінетична енергія",
            formulaDisplay = "Eₖ = mv² / 2",
            category = PhysicsCategory.ENERGY,
            descriptionUkr = "Енергія тіла, зумовлена його рухом зі швидкістю v.",
            searchKeywords = "кінетична енергія рух маса швидкість джоуль Ek m v",
            variables = listOf(
                PhysicsVar("Ek", "Eₖ", "Кінетична енергія", energyUnits),
                PhysicsVar("m", "m", "Маса", massUnits),
                PhysicsVar("v", "v", "Швидкість", speedUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("Ek", "Енергія (Eₖ)", "Eₖ = m · v² ÷ 2", listOf("m", "v")) {
                    val v = it.getValue("v")
                    0.5 * it.getValue("m") * v * v
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = 2·Eₖ ÷ v²", listOf("Ek", "v")) {
                    val v = requireNonZero(it.getValue("v"), "Швидкість")
                    2.0 * it.getValue("Ek") / (v * v)
                },
                PhysicsSolveTarget("v", "Швидкість (v)", "v = √(2·Eₖ ÷ m)", listOf("Ek", "m")) {
                    val m = requireNonZero(it.getValue("m"), "Маса")
                    sqrt(requireNonNegative(2.0 * it.getValue("Ek") / m, "2·Eₖ / m"))
                }
            )
        ),
        PhysicsFormula(
            id = "potential_energy",
            titleUkr = "Потенціальна енергія тяжіння",
            formulaDisplay = "Eₚ = m · g · h",
            category = PhysicsCategory.ENERGY,
            descriptionUkr = "Енергія піднятого над нульовим рівнем тіла масою m на висоті h.",
            searchKeywords = "потенціальна енергія висота маса тяжіння Ep m g h",
            constantsNote = "g = 9,81 м/с²",
            variables = listOf(
                PhysicsVar("Ep", "Eₚ", "Потенціальна енергія", energyUnits),
                PhysicsVar("m", "m", "Маса", massUnits),
                PhysicsVar("h", "h", "Висота", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("Ep", "Енергія (Eₚ)", "Eₚ = m × g × h", listOf("m", "h")) {
                    it.getValue("m") * G_EARTH * it.getValue("h")
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = Eₚ ÷ (g × h)", listOf("Ep", "h")) {
                    it.getValue("Ep") / requireNonZero(G_EARTH * it.getValue("h"), "Висота")
                },
                PhysicsSolveTarget("h", "Висота (h)", "h = Eₚ ÷ (m × g)", listOf("Ep", "m")) {
                    it.getValue("Ep") / requireNonZero(it.getValue("m") * G_EARTH, "Маса")
                }
            )
        ),
        PhysicsFormula(
            id = "spring_energy",
            titleUkr = "Потенціальна енергія пружини",
            formulaDisplay = "E = kx² / 2",
            category = PhysicsCategory.ENERGY,
            descriptionUkr = "Енергія пружно деформованої пружини жорсткістю k.",
            searchKeywords = "енергія пружини деформація жорсткість видовження E k x",
            variables = listOf(
                PhysicsVar("E", "E", "Енергія", energyUnits),
                PhysicsVar("k", "k", "Жорсткість", listOf(PhysicsUnitOption("Н/м", 1.0), PhysicsUnitOption("кН/м", 1000.0))),
                PhysicsVar("x", "x", "Видовження", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("E", "Енергія (E)", "E = k · x² ÷ 2", listOf("k", "x")) {
                    val x = it.getValue("x")
                    0.5 * it.getValue("k") * x * x
                },
                PhysicsSolveTarget("k", "Жорсткість (k)", "k = 2·E ÷ x²", listOf("E", "x")) {
                    val x = requireNonZero(it.getValue("x"), "Видовження")
                    2.0 * it.getValue("E") / (x * x)
                },
                PhysicsSolveTarget("x", "Видовження (x)", "x = √(2·E ÷ k)", listOf("E", "k")) {
                    val k = requireNonZero(it.getValue("k"), "Жорсткість")
                    sqrt(requireNonNegative(2.0 * it.getValue("E") / k, "2·E / k"))
                }
            )
        ),
        PhysicsFormula(
            id = "efficiency",
            titleUkr = "Коефіцієнт корисної дії (ККД)",
            formulaDisplay = "η = (Aₖ / Aₚ) · 100%",
            category = PhysicsCategory.ENERGY,
            descriptionUkr = "Відношення корисної роботи або енергії до повної витраченої роботи.",
            searchKeywords = "ккд коефіцієнт корисної дії відсотки робота енергія механізм eta",
            variables = listOf(
                PhysicsVar("eta", "η", "ККД", listOf(PhysicsUnitOption("%", 1.0))),
                PhysicsVar("Ak", "Aₖ", "Корисна робота", energyUnits),
                PhysicsVar("Ap", "Aₚ", "Повна (витрачена) робота", energyUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("eta", "ККД (η)", "η = (Aₖ ÷ Aₚ) × 100%", listOf("Ak", "Ap")) {
                    (it.getValue("Ak") / requireNonZero(it.getValue("Ap"), "Повна робота")) * 100.0
                },
                PhysicsSolveTarget("Ak", "Корисна робота (Aₖ)", "Aₖ = (η ÷ 100) × Aₚ", listOf("eta", "Ap")) {
                    (it.getValue("eta") / 100.0) * it.getValue("Ap")
                },
                PhysicsSolveTarget("Ap", "Повна робота (Aₚ)", "Aₚ = Aₖ ÷ (η ÷ 100)", listOf("Ak", "eta")) {
                    it.getValue("Ak") / requireNonZero(it.getValue("eta") / 100.0, "ККД")
                }
            )
        ),

        // ==================== ТЕРМОДИНАМІКА ====================
        PhysicsFormula(
            id = "heat_transfer",
            titleUkr = "Кількість теплоти при нагріванні",
            formulaDisplay = "Q = c · m · ΔT",
            category = PhysicsCategory.THERMO,
            descriptionUkr = "Теплота, необхідна для нагрівання або охолодження тіла на ΔT градусів.",
            searchKeywords = "теплота температура нагрівання охолодження теплоємність маса вода Q c m dT",
            constantsNote = "Для води c = 4200 Дж/(кг·К)",
            variables = listOf(
                PhysicsVar("Q", "Q", "Кількість теплоти", energyUnits),
                PhysicsVar("c", "c", "Питома теплоємність", listOf(PhysicsUnitOption("Дж/(кг·К)", 1.0), PhysicsUnitOption("кДж/(кг·К)", 1000.0))),
                PhysicsVar("m", "m", "Маса", massUnits),
                PhysicsVar("dT", "ΔT", "Зміна температури", listOf(PhysicsUnitOption("K / °C", 1.0)))
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("Q", "Теплота (Q)", "Q = c × m × ΔT", listOf("c", "m", "dT")) {
                    it.getValue("c") * it.getValue("m") * it.getValue("dT")
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = Q ÷ (c × ΔT)", listOf("Q", "c", "dT")) {
                    it.getValue("Q") / requireNonZero(it.getValue("c") * it.getValue("dT"), "c × ΔT")
                },
                PhysicsSolveTarget("dT", "Зміна температури (ΔT)", "ΔT = Q ÷ (c × m)", listOf("Q", "c", "m")) {
                    it.getValue("Q") / requireNonZero(it.getValue("c") * it.getValue("m"), "c × m")
                },
                PhysicsSolveTarget("c", "Теплоємність (c)", "c = Q ÷ (m × ΔT)", listOf("Q", "m", "dT")) {
                    it.getValue("Q") / requireNonZero(it.getValue("m") * it.getValue("dT"), "m × ΔT")
                }
            )
        ),
        PhysicsFormula(
            id = "heat_melting",
            titleUkr = "Кількість теплоти плавлення",
            formulaDisplay = "Q = λ · m",
            category = PhysicsCategory.THERMO,
            descriptionUkr = "Теплота, потрібна для розплавлення кристалічного тіла при температурі плавлення.",
            searchKeywords = "плавлення лід теплота кристалізація питома маса Q lambda m",
            constantsNote = "Для льоду λ = 332 000 Дж/кг",
            variables = listOf(
                PhysicsVar("Q", "Q", "Кількість теплоти", energyUnits),
                PhysicsVar("lambda", "λ", "Питома теплота плавлення", listOf(PhysicsUnitOption("Дж/кг", 1.0), PhysicsUnitOption("кДж/кг", 1000.0))),
                PhysicsVar("m", "m", "Маса", massUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("Q", "Теплота (Q)", "Q = λ × m", listOf("lambda", "m")) {
                    it.getValue("lambda") * it.getValue("m")
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = Q ÷ λ", listOf("Q", "lambda")) {
                    it.getValue("Q") / requireNonZero(it.getValue("lambda"), "Теплота плавлення")
                },
                PhysicsSolveTarget("lambda", "Питома теплота (λ)", "λ = Q ÷ m", listOf("Q", "m")) {
                    it.getValue("Q") / requireNonZero(it.getValue("m"), "Маса")
                }
            )
        ),
        PhysicsFormula(
            id = "heat_vaporization",
            titleUkr = "Теплота пароутворення (кипіння)",
            formulaDisplay = "Q = L · m",
            category = PhysicsCategory.THERMO,
            descriptionUkr = "Теплота, необхідна для перетворення рідини на пару при температурі кипіння.",
            searchKeywords = "пароутворення кипіння конденсація теплота пара Q L m",
            constantsNote = "Для води L = 2,26×10⁶ Дж/кг",
            variables = listOf(
                PhysicsVar("Q", "Q", "Кількість теплоти", energyUnits),
                PhysicsVar("L", "L", "Питома теплота пароутворення", listOf(PhysicsUnitOption("Дж/кг", 1.0), PhysicsUnitOption("кДж/кг", 1000.0), PhysicsUnitOption("МДж/кг", 1_000_000.0))),
                PhysicsVar("m", "m", "Маса", massUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("Q", "Теплота (Q)", "Q = L × m", listOf("L", "m")) {
                    it.getValue("L") * it.getValue("m")
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = Q ÷ L", listOf("Q", "L")) {
                    it.getValue("Q") / requireNonZero(it.getValue("L"), "Питома теплота")
                },
                PhysicsSolveTarget("L", "Питома теплота (L)", "L = Q ÷ m", listOf("Q", "m")) {
                    it.getValue("Q") / requireNonZero(it.getValue("m"), "Маса")
                }
            )
        ),
        PhysicsFormula(
            id = "ideal_gas",
            titleUkr = "Рівняння ідеального газу",
            formulaDisplay = "p · V = ν · R · T",
            category = PhysicsCategory.THERMO,
            descriptionUkr = "Рівняння Менделєєва–Клапейрона для стану ідеального газу.",
            searchKeywords = "ідеальний газ менделєєв клапейрон тиск об’єм температура моль p V R T",
            constantsNote = "Газова стала R = 8,314 Дж/(моль·К)",
            variables = listOf(
                PhysicsVar("p", "p", "Тиск газу", pressureUnits),
                PhysicsVar("V", "V", "Об’єм газу", volumeUnits),
                PhysicsVar("nu", "ν", "Кількість речовини", listOf(PhysicsUnitOption("моль", 1.0))),
                PhysicsVar("T", "T", "Температура", listOf(PhysicsUnitOption("K", 1.0, 0.0), PhysicsUnitOption("°C", 1.0, 273.15)))
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("p", "Тиск (p)", "p = ν · R · T ÷ V", listOf("nu", "T", "V")) {
                    it.getValue("nu") * R_GAS * it.getValue("T") / requireNonZero(it.getValue("V"), "Об’єм")
                },
                PhysicsSolveTarget("V", "Об’єм (V)", "V = ν · R · T ÷ p", listOf("nu", "T", "p")) {
                    it.getValue("nu") * R_GAS * it.getValue("T") / requireNonZero(it.getValue("p"), "Тиск")
                },
                PhysicsSolveTarget("nu", "Кількість речовини (ν)", "ν = (p · V) ÷ (R · T)", listOf("p", "V", "T")) {
                    it.getValue("p") * it.getValue("V") / requireNonZero(R_GAS * it.getValue("T"), "Температура (K)")
                },
                PhysicsSolveTarget("T", "Температура (T)", "T = (p · V) ÷ (ν · R)", listOf("p", "V", "nu")) {
                    it.getValue("p") * it.getValue("V") / requireNonZero(it.getValue("nu") * R_GAS, "Кількість речовини")
                }
            )
        ),

        // ==================== ЕЛЕКТРИКА І МАГНЕТИЗМ ====================
        PhysicsFormula(
            id = "ohms_law",
            titleUkr = "Закон Ома для ділянки кола",
            formulaDisplay = "I = U / R",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Зв’язок між силою струму, напругою на ділянці кола та її електричним опором.",
            searchKeywords = "ом струм напруга опір коло вольт ампер ом електрика I U R",
            variables = listOf(
                PhysicsVar("I", "I", "Сила струму", currentUnits),
                PhysicsVar("U", "U", "Напруга", voltageUnits),
                PhysicsVar("R", "R", "Опір", resistanceUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("I", "Сила струму (I)", "I = U ÷ R", listOf("U", "R")) {
                    it.getValue("U") / requireNonZero(it.getValue("R"), "Опір")
                },
                PhysicsSolveTarget("U", "Напруга (U)", "U = I × R", listOf("I", "R")) {
                    it.getValue("I") * it.getValue("R")
                },
                PhysicsSolveTarget("R", "Опір (R)", "R = U ÷ I", listOf("U", "I")) {
                    it.getValue("U") / requireNonZero(it.getValue("I"), "Сила струму")
                }
            )
        ),
        PhysicsFormula(
            id = "electric_current",
            titleUkr = "Сила електричного струму",
            formulaDisplay = "I = q / t",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Заряд, що проходить через поперечний переріз провідника за одиницю часу.",
            searchKeywords = "сила струму заряд час кулон ампер I q t",
            variables = listOf(
                PhysicsVar("I", "I", "Сила струму", currentUnits),
                PhysicsVar("q", "q", "Заряд", chargeUnits),
                PhysicsVar("t", "t", "Час", timeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("I", "Сила струму (I)", "I = q ÷ t", listOf("q", "t")) {
                    it.getValue("q") / requireNonZero(it.getValue("t"), "Час")
                },
                PhysicsSolveTarget("q", "Заряд (q)", "q = I × t", listOf("I", "t")) {
                    it.getValue("I") * it.getValue("t")
                },
                PhysicsSolveTarget("t", "Час (t)", "t = q ÷ I", listOf("q", "I")) {
                    it.getValue("q") / requireNonZero(it.getValue("I"), "Сила струму")
                }
            )
        ),
        PhysicsFormula(
            id = "wire_resistance",
            titleUkr = "Опір провідника",
            formulaDisplay = "R = ρ · l / S",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Залежність опору провідника від питомого опору матеріалу, довжини та площі перерізу.",
            searchKeywords = "опір провідник дріт довжина переріз питомий мідь R rho l S",
            constantsNote = "Для міді ρ = 1,68×10⁻⁸ Ом·м",
            variables = listOf(
                PhysicsVar("R", "R", "Опір", resistanceUnits),
                PhysicsVar("rho", "ρ", "Питомий опір", listOf(PhysicsUnitOption("Ом·м", 1.0), PhysicsUnitOption("Ом·мм²/м", 1e-6))),
                PhysicsVar("l", "l", "Довжина", lengthUnits),
                PhysicsVar("S", "S", "Площа перерізу", areaUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("R", "Опір (R)", "R = ρ · l ÷ S", listOf("rho", "l", "S")) {
                    it.getValue("rho") * it.getValue("l") / requireNonZero(it.getValue("S"), "Площа перерізу")
                },
                PhysicsSolveTarget("l", "Довжина (l)", "l = R · S ÷ ρ", listOf("R", "S", "rho")) {
                    it.getValue("R") * it.getValue("S") / requireNonZero(it.getValue("rho"), "Питомий опір")
                },
                PhysicsSolveTarget("S", "Площа перерізу (S)", "S = ρ · l ÷ R", listOf("rho", "l", "R")) {
                    it.getValue("rho") * it.getValue("l") / requireNonZero(it.getValue("R"), "Опір")
                }
            )
        ),
        PhysicsFormula(
            id = "electric_power",
            titleUkr = "Потужність електричного струму",
            formulaDisplay = "P = U · I",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Електрична потужність споживача при напрузі U і струмі I.",
            searchKeywords = "потужність струм напруга ват вольт ампер енергія P U I",
            variables = listOf(
                PhysicsVar("P", "P", "Потужність", powerUnits),
                PhysicsVar("U", "U", "Напруга", voltageUnits),
                PhysicsVar("I", "I", "Сила струму", currentUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("P", "Потужність (P)", "P = U × I", listOf("U", "I")) {
                    it.getValue("U") * it.getValue("I")
                },
                PhysicsSolveTarget("U", "Напруга (U)", "U = P ÷ I", listOf("P", "I")) {
                    it.getValue("P") / requireNonZero(it.getValue("I"), "Сила струму")
                },
                PhysicsSolveTarget("I", "Сила струму (I)", "I = P ÷ U", listOf("P", "U")) {
                    it.getValue("P") / requireNonZero(it.getValue("U"), "Напруга")
                }
            )
        ),
        PhysicsFormula(
            id = "joule_lenz",
            titleUkr = "Закон Джоуля–Ленца",
            formulaDisplay = "Q = I² · R · t",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Кількість теплоти, що виділяється у провіднику зі струмом за час t.",
            searchKeywords = "джоуль ленц теплота струм нагрівник опір час Q I R t",
            variables = listOf(
                PhysicsVar("Q", "Q", "Кількість теплоти", energyUnits),
                PhysicsVar("I", "I", "Сила струму", currentUnits),
                PhysicsVar("R", "R", "Опір", resistanceUnits),
                PhysicsVar("t", "t", "Час", timeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("Q", "Теплота (Q)", "Q = I² × R × t", listOf("I", "R", "t")) {
                    val i = it.getValue("I")
                    i * i * it.getValue("R") * it.getValue("t")
                },
                PhysicsSolveTarget("I", "Сила струму (I)", "I = √(Q ÷ (R · t))", listOf("Q", "R", "t")) {
                    val denom = requireNonZero(it.getValue("R") * it.getValue("t"), "R × t")
                    sqrt(requireNonNegative(it.getValue("Q") / denom, "Q / (R · t)"))
                },
                PhysicsSolveTarget("R", "Опір (R)", "R = Q ÷ (I² · t)", listOf("Q", "I", "t")) {
                    val i = it.getValue("I")
                    it.getValue("Q") / requireNonZero(i * i * it.getValue("t"), "I² × t")
                },
                PhysicsSolveTarget("t", "Час (t)", "t = Q ÷ (I² · R)", listOf("Q", "I", "R")) {
                    val i = it.getValue("I")
                    it.getValue("Q") / requireNonZero(i * i * it.getValue("R"), "I² × R")
                }
            )
        ),
        PhysicsFormula(
            id = "capacitor_energy",
            titleUkr = "Енергія конденсатора",
            formulaDisplay = "W = CU² / 2",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Енергія електричного поля зарядженого конденсатора ємністю C.",
            searchKeywords = "конденсатор ємність напруга енергія фарад W C U",
            variables = listOf(
                PhysicsVar("W", "W", "Енергія", energyUnits),
                PhysicsVar("C", "C", "Електроємність", listOf(PhysicsUnitOption("Ф", 1.0), PhysicsUnitOption("мФ", 0.001), PhysicsUnitOption("мкФ", 1e-6))),
                PhysicsVar("U", "U", "Напруга", voltageUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("W", "Енергія (W)", "W = C · U² ÷ 2", listOf("C", "U")) {
                    val u = it.getValue("U")
                    0.5 * it.getValue("C") * u * u
                },
                PhysicsSolveTarget("C", "Ємність (C)", "C = 2·W ÷ U²", listOf("W", "U")) {
                    val u = requireNonZero(it.getValue("U"), "Напруга")
                    2.0 * it.getValue("W") / (u * u)
                },
                PhysicsSolveTarget("U", "Напруга (U)", "U = √(2·W ÷ C)", listOf("W", "C")) {
                    val c = requireNonZero(it.getValue("C"), "Ємність")
                    sqrt(requireNonNegative(2.0 * it.getValue("W") / c, "2·W / C"))
                }
            )
        ),
        PhysicsFormula(
            id = "coulomb_law",
            titleUkr = "Закон Кулона",
            formulaDisplay = "F = k · |q₁q₂| / r²",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Сила електростатичної взаємодії між двома точковими зарядами.",
            searchKeywords = "кулон заряд сила взаємодія відстань електростатика F q r",
            constantsNote = "k = 8,99×10⁹ Н·м²/Кл²",
            variables = listOf(
                PhysicsVar("F", "F", "Сила взаємодії", forceUnits),
                PhysicsVar("q1", "q₁", "Заряд 1", chargeUnits),
                PhysicsVar("q2", "q₂", "Заряд 2", chargeUnits),
                PhysicsVar("r", "r", "Відстань", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Сила (F)", "F = k · |q₁·q₂| ÷ r²", listOf("q1", "q2", "r")) {
                    val r = requireNonZero(it.getValue("r"), "Відстань")
                    K_COULOMB * abs(it.getValue("q1") * it.getValue("q2")) / (r * r)
                },
                PhysicsSolveTarget("r", "Відстань (r)", "r = √(k · |q₁·q₂| ÷ F)", listOf("q1", "q2", "F")) {
                    val f = requireNonZero(it.getValue("F"), "Сила")
                    sqrt(requireNonNegative(K_COULOMB * abs(it.getValue("q1") * it.getValue("q2")) / f, "Підкореневий вираз"))
                }
            )
        ),
        PhysicsFormula(
            id = "ampere_force",
            titleUkr = "Сила Ампера (перпендикулярний струм)",
            formulaDisplay = "F = B · I · l",
            category = PhysicsCategory.ELECTRICITY,
            descriptionUkr = "Сила, що діє на провідник зі струмом у магнітному полі (при sin α = 1).",
            searchKeywords = "ампер магнітне поле індукція тесла струм провідник F B I l",
            variables = listOf(
                PhysicsVar("F", "F", "Сила Ампера", forceUnits),
                PhysicsVar("B", "B", "Магнітна індукція", listOf(PhysicsUnitOption("Тл", 1.0), PhysicsUnitOption("мТл", 0.001))),
                PhysicsVar("I", "I", "Сила струму", currentUnits),
                PhysicsVar("l", "l", "Довжина провідника", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Сила (F)", "F = B × I × l", listOf("B", "I", "l")) {
                    it.getValue("B") * it.getValue("I") * it.getValue("l")
                },
                PhysicsSolveTarget("B", "Магнітна індукція (B)", "B = F ÷ (I · l)", listOf("F", "I", "l")) {
                    it.getValue("F") / requireNonZero(it.getValue("I") * it.getValue("l"), "I × l")
                },
                PhysicsSolveTarget("I", "Сила струму (I)", "I = F ÷ (B · l)", listOf("F", "B", "l")) {
                    it.getValue("F") / requireNonZero(it.getValue("B") * it.getValue("l"), "B × l")
                }
            )
        ),

        // ==================== ХВИЛІ Й ОПТИКА ====================
        PhysicsFormula(
            id = "period_frequency",
            titleUkr = "Період і частота коливань",
            formulaDisplay = "T = 1 / ν",
            category = PhysicsCategory.WAVES_OPTICS,
            descriptionUkr = "Взаємний зв’язок між періодом коливань (часом одного циклу) та частотою.",
            searchKeywords = "період частота коливання герц час T nu f",
            variables = listOf(
                PhysicsVar("T", "T", "Період", timeUnits),
                PhysicsVar("nu", "ν", "Частота", freqUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("T", "Період (T)", "T = 1 ÷ ν", listOf("nu")) {
                    1.0 / requireNonZero(it.getValue("nu"), "Частота")
                },
                PhysicsSolveTarget("nu", "Частота (ν)", "ν = 1 ÷ T", listOf("T")) {
                    1.0 / requireNonZero(it.getValue("T"), "Період")
                }
            )
        ),
        PhysicsFormula(
            id = "wavelength",
            titleUkr = "Довжина хвилі",
            formulaDisplay = "λ = v · T",
            category = PhysicsCategory.WAVES_OPTICS,
            descriptionUkr = "Відстань, на яку поширюється хвиля за один період коливань.",
            searchKeywords = "довжина хвилі швидкість період звук світло lambda v T",
            variables = listOf(
                PhysicsVar("lambda", "λ", "Довжина хвилі", lengthUnits),
                PhysicsVar("v", "v", "Швидкість хвилі", speedUnits),
                PhysicsVar("T", "T", "Період", timeUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("lambda", "Довжина хвилі (λ)", "λ = v × T", listOf("v", "T")) {
                    it.getValue("v") * it.getValue("T")
                },
                PhysicsSolveTarget("v", "Швидкість (v)", "v = λ ÷ T", listOf("lambda", "T")) {
                    it.getValue("lambda") / requireNonZero(it.getValue("T"), "Період")
                },
                PhysicsSolveTarget("T", "Період (T)", "T = λ ÷ v", listOf("lambda", "v")) {
                    it.getValue("lambda") / requireNonZero(it.getValue("v"), "Швидкість")
                }
            )
        ),
        PhysicsFormula(
            id = "math_pendulum",
            titleUkr = "Період математичного маятника",
            formulaDisplay = "T = 2π · √(l / g)",
            category = PhysicsCategory.WAVES_OPTICS,
            descriptionUkr = "Період малих коливань нитяного маятника довжиною l.",
            searchKeywords = "маятник період час довжина нитка коливання T l g",
            constantsNote = "g = 9,81 м/с²",
            variables = listOf(
                PhysicsVar("T", "T", "Період", timeUnits),
                PhysicsVar("l", "l", "Довжина нитки", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("T", "Період (T)", "T = 2π · √(l ÷ g)", listOf("l")) {
                    2.0 * PI * sqrt(requireNonNegative(it.getValue("l"), "Довжина") / G_EARTH)
                },
                PhysicsSolveTarget("l", "Довжина (l)", "l = g · T² ÷ (4π²)", listOf("T")) {
                    val t = it.getValue("T")
                    G_EARTH * t * t / (4.0 * PI * PI)
                }
            )
        ),
        PhysicsFormula(
            id = "thin_lens",
            titleUkr = "Формула тонкої лінзи",
            formulaDisplay = "1/F = 1/d + 1/f",
            category = PhysicsCategory.WAVES_OPTICS,
            descriptionUkr = "Зв’язок фокусної відстані лінзи, відстані до предмета та відстані до зображення.",
            searchKeywords = "оптика лінза фокус зображення діоптрії відстань F d f",
            variables = listOf(
                PhysicsVar("F", "F", "Фокусна відстань", lengthUnits),
                PhysicsVar("d", "d", "Відстань до предмета", lengthUnits),
                PhysicsVar("f", "f", "Відстань до зображення", lengthUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("F", "Фокусна відстань (F)", "F = 1 ÷ (1/d + 1/f)", listOf("d", "f")) {
                    val d = requireNonZero(it.getValue("d"), "Відстань d")
                    val f = requireNonZero(it.getValue("f"), "Відстань f")
                    val inv = 1.0 / d + 1.0 / f
                    1.0 / requireNonZero(inv, "1/d + 1/f")
                },
                PhysicsSolveTarget("f", "Відстань до зображення (f)", "f = 1 ÷ (1/F − 1/d)", listOf("F", "d")) {
                    val bigF = requireNonZero(it.getValue("F"), "Фокус F")
                    val d = requireNonZero(it.getValue("d"), "Відстань d")
                    val inv = 1.0 / bigF - 1.0 / d
                    1.0 / requireNonZero(inv, "1/F − 1/d")
                },
                PhysicsSolveTarget("d", "Відстань до предмета (d)", "d = 1 ÷ (1/F − 1/f)", listOf("F", "f")) {
                    val bigF = requireNonZero(it.getValue("F"), "Фокус F")
                    val f = requireNonZero(it.getValue("f"), "Відстань f")
                    val inv = 1.0 / bigF - 1.0 / f
                    1.0 / requireNonZero(inv, "1/F − 1/f")
                }
            )
        ),
        PhysicsFormula(
            id = "photon_energy",
            titleUkr = "Енергія фотона (формула Планка)",
            formulaDisplay = "E = h · ν",
            category = PhysicsCategory.WAVES_OPTICS,
            descriptionUkr = "Енергія кванта світла з частотою випромінювання ν.",
            searchKeywords = "фотон квант планк світло частота енергія оптика E h nu",
            constantsNote = "Стала Планка h = 6,626×10⁻³⁴ Дж·с",
            variables = listOf(
                PhysicsVar("E", "E", "Енергія фотона", listOf(PhysicsUnitOption("Дж", 1.0), PhysicsUnitOption("еВ", EV_TO_J))),
                PhysicsVar("nu", "ν", "Частота", freqUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("E", "Енергія (E)", "E = h × ν", listOf("nu")) {
                    H_PLANCK * it.getValue("nu")
                },
                PhysicsSolveTarget("nu", "Частота (ν)", "ν = E ÷ h", listOf("E")) {
                    it.getValue("E") / H_PLANCK
                }
            )
        ),
        PhysicsFormula(
            id = "einstein_mass_energy",
            titleUkr = "Енергія спокою (формула Ейнштейна)",
            formulaDisplay = "E = m · c²",
            category = PhysicsCategory.WAVES_OPTICS,
            descriptionUkr = "Еквівалентність маси та енергії за спеціальною теорією відносності.",
            searchKeywords = "ейнштейн маса енергія світло швидкість теорія відносності E m c",
            constantsNote = "Швидкість світла c = 299 792 458 м/с",
            variables = listOf(
                PhysicsVar("E", "E", "Енергія", energyUnits),
                PhysicsVar("m", "m", "Маса", massUnits)
            ),
            solveTargets = listOf(
                PhysicsSolveTarget("E", "Енергія (E)", "E = m × c²", listOf("m")) {
                    it.getValue("m") * C_LIGHT * C_LIGHT
                },
                PhysicsSolveTarget("m", "Маса (m)", "m = E ÷ c²", listOf("E")) {
                    it.getValue("E") / (C_LIGHT * C_LIGHT)
                }
            )
        )
    )

    fun filter(
        query: String,
        category: PhysicsCategory?,
        favoritesOnly: Boolean,
        favoriteIds: Set<String>
    ): List<PhysicsFormula> {
        val trimmed = query.trim().lowercase()
        val tokens = if (trimmed.isEmpty()) emptyList() else trimmed.split(Regex("\\s+"))
        return allFormulas.filter { formula ->
            if (favoritesOnly && formula.id !in favoriteIds) return@filter false
            if (category != null && formula.category != category) return@filter false
            if (tokens.isEmpty()) return@filter true

            val haystack = buildString {
                append(formula.titleUkr.lowercase()).append(' ')
                append(formula.formulaDisplay.lowercase()).append(' ')
                append(formula.category.labelUkr.lowercase()).append(' ')
                append(formula.descriptionUkr.lowercase()).append(' ')
                append(formula.searchKeywords.lowercase()).append(' ')
                formula.variables.forEach {
                    append(it.symbol.lowercase()).append(' ')
                    append(it.nameUkr.lowercase()).append(' ')
                }
            }
            tokens.all { token -> haystack.contains(token) }
        }
    }
}
