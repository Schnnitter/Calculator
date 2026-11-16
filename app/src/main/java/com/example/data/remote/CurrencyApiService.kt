package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenExchangeRatesResponse(
    val result: String? = null,
    @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @Json(name = "base_code") val baseCode: String? = null,
    val rates: Map<String, Double>? = null
)

@JsonClass(generateAdapter = true)
data class NbuCurrencyItem(
    val r030: Int? = null,
    val txt: String? = null,
    val rate: Double? = null,
    val cc: String? = null,
    @Json(name = "exchangedate") val exchangeDate: String? = null
)

interface CurrencyApiService {
    @GET("v6/latest/USD")
    suspend fun getOpenExchangeRates(): OpenExchangeRatesResponse

    @GET
    suspend fun getNbuRates(@Url url: String = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json"): List<NbuCurrencyItem>

    companion object {
        private const val BASE_URL = "https://open.er-api.com/"

        fun create(): CurrencyApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(CurrencyApiService::class.java)
        }
    }
}
