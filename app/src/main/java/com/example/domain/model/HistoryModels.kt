package com.example.domain.model

enum class HistoryMode(val labelUkr: String) {
    STANDARD("Звичайний"),
    FRACTIONS("Дроби"),
    POWERS("Степені"),
    SCIENTIFIC("Науковий"),
    PHYSICS("Фізика")
}

data class HistoryItem(
    val id: Long = 0L,
    val mode: HistoryMode,
    val expressionDisplay: String,
    val resultDisplay: String,
    val rawResult: String,
    val fractionJson: String? = null,
    val powerJson: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

sealed class EvalResult {
    data class Success(
        val numericString: String,
        val formattedDisplay: String
    ) : EvalResult()

    data class Error(val messageUkr: String) : EvalResult()
}
