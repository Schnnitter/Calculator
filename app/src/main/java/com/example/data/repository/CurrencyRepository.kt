package com.example.data.repository

import com.example.data.local.CurrencyDao
import com.example.data.local.CurrencyRateEntity
import com.example.data.remote.CurrencyApiService
import com.example.domain.model.CurrencyRate
import com.example.domain.model.CurrencyRatesResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class CurrencyRepository(
    private val apiService: CurrencyApiService,
    private val currencyDao: CurrencyDao
) {
    suspend fun getCachedRates(): CurrencyRatesResult? = withContext(Dispatchers.IO) {
        val entities = currencyDao.getAllRatesOnce()
        if (entities.isEmpty()) return@withContext null
        val map = entities.associate { entity ->
            entity.code to CurrencyRate(
                code = entity.code,
                ratePerUsd = entity.ratePerUsd,
                updatedAtMillis = entity.updatedAtMillis,
                sourceLabel = entity.sourceLabel
            )
        }
        val latestUpdate = entities.maxOfOrNull { it.updatedAtMillis } ?: System.currentTimeMillis()
        val source = entities.firstOrNull()?.sourceLabel ?: "Кеш"
        CurrencyRatesResult(
            rates = map,
            updatedAtMillis = latestUpdate,
            isFromCache = true,
            sourceLabel = source
        )
    }

    suspend fun fetchLiveRates(): Result<CurrencyRatesResult> = withContext(Dispatchers.IO) {
        // Primary: Open ExchangeRate-API (160+ world currencies)
        try {
            val response = apiService.getOpenExchangeRates()
            val rawRates = response.rates
            if (response.result == "success" && !rawRates.isNullOrEmpty()) {
                val updatedMillis = (response.timeLastUpdateUnix ?: (System.currentTimeMillis() / 1000L)) * 1000L
                val source = "ExchangeRate-API"
                val entities = rawRates.mapNotNull { (code, value) ->
                    if (value <= 0.0 || value.isNaN() || value.isInfinite()) null
                    else CurrencyRateEntity(
                        code = code.uppercase(),
                        ratePerUsd = BigDecimal.valueOf(value).toPlainString(),
                        updatedAtMillis = updatedMillis,
                        sourceLabel = source
                    )
                }
                if (entities.isNotEmpty()) {
                    currencyDao.upsertRates(entities)
                    val rateMap = entities.associate {
                        it.code to CurrencyRate(it.code, it.ratePerUsd, it.updatedAtMillis, it.sourceLabel)
                    }
                    return@withContext Result.success(
                        CurrencyRatesResult(
                            rates = rateMap,
                            updatedAtMillis = updatedMillis,
                            isFromCache = false,
                            sourceLabel = source
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to NBU fallback
        }

        // Secondary fallback: National Bank of Ukraine official API
        try {
            val nbuList = apiService.getNbuRates()
            val usdEntry = nbuList.firstOrNull { it.cc.equals("USD", ignoreCase = true) && (it.rate ?: 0.0) > 0.0 }
            if (usdEntry != null && usdEntry.rate != null) {
                val usdInUah = BigDecimal.valueOf(usdEntry.rate)
                val now = System.currentTimeMillis()
                val source = "НБУ (bank.gov.ua)"
                val mc = MathContext(12, RoundingMode.HALF_UP)

                val entities = mutableListOf(
                    CurrencyRateEntity("USD", "1", now, source),
                    CurrencyRateEntity("UAH", usdInUah.toPlainString(), now, source)
                )
                for (item in nbuList) {
                    val code = item.cc?.uppercase() ?: continue
                    val rateInUah = item.rate ?: continue
                    if (code == "USD" || rateInUah <= 0.0) continue
                    val ratePerUsd = usdInUah.divide(BigDecimal.valueOf(rateInUah), mc)
                    entities.add(CurrencyRateEntity(code, ratePerUsd.toPlainString(), now, source))
                }
                currencyDao.upsertRates(entities)
                val rateMap = entities.associate {
                    it.code to CurrencyRate(it.code, it.ratePerUsd, it.updatedAtMillis, it.sourceLabel)
                }
                return@withContext Result.success(
                    CurrencyRatesResult(
                        rates = rateMap,
                        updatedAtMillis = now,
                        isFromCache = false,
                        sourceLabel = source
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        Result.failure(IllegalStateException("Не вдалося отримати курси валют"))
    }
}
