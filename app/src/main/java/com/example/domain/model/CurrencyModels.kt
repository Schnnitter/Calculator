package com.example.domain.model

data class CurrencyInfo(
    val code: String,
    val nameUkr: String,
    val symbol: String,
    val isCore: Boolean = false
)

data class CurrencyRate(
    val code: String,
    val ratePerUsd: String,
    val updatedAtMillis: Long,
    val sourceLabel: String
)

data class CurrencyRatesResult(
    val rates: Map<String, CurrencyRate>,
    val updatedAtMillis: Long,
    val isFromCache: Boolean,
    val sourceLabel: String
)

object CurrencyCatalog {
    val coreCodes = listOf("UAH", "USD", "EUR", "GBP", "PLN", "CZK", "CHF", "JPY", "CAD", "AUD")

    private val knownCurrencies: Map<String, CurrencyInfo> = listOf(
        CurrencyInfo("UAH", "Українська гривня", "₴", isCore = true),
        CurrencyInfo("USD", "Долар США", "$", isCore = true),
        CurrencyInfo("EUR", "Євро", "€", isCore = true),
        CurrencyInfo("GBP", "Британський фунт", "£", isCore = true),
        CurrencyInfo("PLN", "Польський злотий", "zł", isCore = true),
        CurrencyInfo("CZK", "Чеська крона", "Kč", isCore = true),
        CurrencyInfo("CHF", "Швейцарський франк", "Fr", isCore = true),
        CurrencyInfo("JPY", "Японська єна", "¥", isCore = true),
        CurrencyInfo("CAD", "Канадський долар", "C$", isCore = true),
        CurrencyInfo("AUD", "Австралійський долар", "A$", isCore = true),
        CurrencyInfo("SEK", "Шведська крона", "kr"),
        CurrencyInfo("NOK", "Норвезька крона", "kr"),
        CurrencyInfo("DKK", "Данська крона", "kr"),
        CurrencyInfo("HUF", "Угорський форинт", "Ft"),
        CurrencyInfo("RON", "Румунський лей", "lei"),
        CurrencyInfo("BGN", "Болгарський лев", "лв"),
        CurrencyInfo("MDL", "Молдовський лей", "L"),
        CurrencyInfo("GEL", "Грузинський ларі", "₾"),
        CurrencyInfo("TRY", "Турецька ліра", "₺"),
        CurrencyInfo("CNY", "Китайський юань", "¥"),
        CurrencyInfo("KRW", "Південнокорейська вона", "₩"),
        CurrencyInfo("INR", "Індійська рупія", "₹"),
        CurrencyInfo("ILS", "Ізраїльський шекель", "₪"),
        CurrencyInfo("AED", "Дирхам ОАЕ", "د.إ"),
        CurrencyInfo("SAR", "Саудівський ріал", "﷼"),
        CurrencyInfo("BRL", "Бразильський реал", "R$"),
        CurrencyInfo("MXN", "Мексиканське песо", "$"),
        CurrencyInfo("SGD", "Сінгапурський долар", "S$"),
        CurrencyInfo("HKD", "Гонконгський долар", "HK$"),
        CurrencyInfo("NZD", "Новозеландський долар", "NZ$"),
        CurrencyInfo("ZAR", "Південноафриканський ранд", "R"),
        CurrencyInfo("THB", "Таїландський бат", "฿"),
        CurrencyInfo("IDR", "Індонезійська рупія", "Rp"),
        CurrencyInfo("MYR", "Малайзійський ринггіт", "RM"),
        CurrencyInfo("PHP", "Філіппінське песо", "₱"),
        CurrencyInfo("VND", "В’єтнамський донг", "₫"),
        CurrencyInfo("EGP", "Єгипетський фунт", "E£"),
        CurrencyInfo("KZT", "Казахстанський тенге", "₸"),
        CurrencyInfo("AZN", "Азербайджанський манат", "₼"),
        CurrencyInfo("UZS", "Узбецький сум", "soʻm"),
        CurrencyInfo("ISK", "Ісландська крона", "kr"),
        CurrencyInfo("RSD", "Сербський динар", "дин"),
        CurrencyInfo("ALL", "Албанський лек", "L"),
        CurrencyInfo("BAM", "Боснійська марка", "KM"),
        CurrencyInfo("MKD", "Македонський денар", "ден")
    ).associateBy { it.code }

    fun getInfo(code: String): CurrencyInfo {
        val upper = code.uppercase()
        knownCurrencies[upper]?.let { return it }
        val fallbackName = try {
            java.util.Currency.getInstance(upper)
                .getDisplayName(java.util.Locale.forLanguageTag("uk-UA"))
                .replaceFirstChar { it.titlecase(java.util.Locale.forLanguageTag("uk-UA")) }
        } catch (_: Exception) {
            upper
        }
        val fallbackSymbol = try {
            java.util.Currency.getInstance(upper).symbol
        } catch (_: Exception) {
            upper
        }
        return CurrencyInfo(
            code = upper,
            nameUkr = fallbackName,
            symbol = fallbackSymbol,
            isCore = false
        )
    }

    fun getCoreList(): List<CurrencyInfo> = coreCodes.mapNotNull { knownCurrencies[it] }
}
