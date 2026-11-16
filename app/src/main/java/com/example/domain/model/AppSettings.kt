package com.example.domain.model

enum class ThemeMode(val labelUkr: String) {
    DARK("Темна"),
    LIGHT("Світла"),
    SYSTEM("Системна")
}

enum class DecimalSeparator(val symbol: String, val labelUkr: String) {
    COMMA(",", "Кома (3,14)"),
    DOT(".", "Крапка (3.14)")
}

enum class GroupingStyle(val labelUkr: String) {
    SPACE("Пробіл (1 234)"),
    NONE("Без роздільника (1234)")
}

enum class FractionDisplayPreference(val labelUkr: String) {
    MIXED_FIRST("Мішане число (1 ½)"),
    IMPROPER_FIRST("Неправильний дріб (3/2)")
}

enum class AngleUnit(val labelUkr: String, val shortLabel: String) {
    DEG("Градуси", "DEG"),
    RAD("Радіани", "RAD")
}

enum class CalculatorMode(val titleUkr: String) {
    STANDARD("Звичайний"),
    FRACTIONS("Дроби"),
    POWERS("Степені"),
    SCIENTIFIC("Науковий"),
    PHYSICS("Фізика"),
    CURRENCY("Валюти"),
    HISTORY("Історія")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val decimalPrecision: Int = 8,
    val decimalSeparator: DecimalSeparator = DecimalSeparator.COMMA,
    val groupingStyle: GroupingStyle = GroupingStyle.SPACE,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val fractionDisplayPreference: FractionDisplayPreference = FractionDisplayPreference.MIXED_FIRST
)
