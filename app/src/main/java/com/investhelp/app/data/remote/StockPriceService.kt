package com.investhelp.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class StockQuote(
    val price: Double,
    val previousClose: Double
)

data class HistoricalPrice(
    val timestamp: Long, // epoch seconds
    val close: Double
)

data class AnalysisInfo(
    val shortName: String?,
    val sector: String?,
    val industry: String?,
    val marketCap: Long?,
    val trailingPE: Double?,
    val forwardPE: Double?,
    val eps: Double?,
    val dividendYield: Double?,
    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val fiftyDayAverage: Double?,
    val twoHundredDayAverage: Double?,
    val targetMeanPrice: Double?,
    val revenuePerShare: Double?,
    val profitMargins: Double?,
    val returnOnEquity: Double?,
    val longBusinessSummary: String?
)

@Singleton
class StockPriceService @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchPrice(ticker: String): Double = fetchQuote(ticker).price

    suspend fun fetchHistoricalPrices(ticker: String, rangeDays: Int = 14): List<HistoricalPrice> =
        withContext(Dispatchers.IO) {
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=${rangeDays}d&interval=1d")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "InvestHelp/1.0")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            try {
                if (connection.responseCode != 200) {
                    throw Exception("Yahoo Finance returned ${connection.responseCode}")
                }

                val body = connection.inputStream.bufferedReader().readText()
                val root = json.parseToJsonElement(body) as JsonObject
                val result = root["chart"]!!.jsonObject["result"]!!.jsonArray[0].jsonObject
                val timestamps = result["timestamp"]!!.jsonArray.map { it.jsonPrimitive.long }
                val closes = result["indicators"]!!.jsonObject["quote"]!!.jsonArray[0]
                    .jsonObject["close"]!!.jsonArray

                timestamps.mapIndexedNotNull { i, ts ->
                    val close = closes[i].jsonPrimitive.doubleOrNull ?: return@mapIndexedNotNull null
                    HistoricalPrice(timestamp = ts, close = close)
                }
            } finally {
                connection.disconnect()
            }
        }

    suspend fun fetchAnalysisInfo(ticker: String): AnalysisInfo = withContext(Dispatchers.IO) {
        val modules = "summaryProfile,summaryDetail,financialData,defaultKeyStatistics"
        val url = URL("https://query1.finance.yahoo.com/v10/finance/quoteSummary/${ticker}?modules=$modules")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "InvestHelp/1.0")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {
            if (connection.responseCode != 200) {
                throw Exception("Yahoo Finance returned ${connection.responseCode}")
            }

            val body = connection.inputStream.bufferedReader().readText()
            val root = json.parseToJsonElement(body) as JsonObject
            val result = root["quoteSummary"]!!.jsonObject["result"]!!.jsonArray[0].jsonObject

            val profile = result["summaryProfile"]?.jsonObject
            val detail = result["summaryDetail"]?.jsonObject
            val financial = result["financialData"]?.jsonObject
            val keyStats = result["defaultKeyStatistics"]?.jsonObject

            AnalysisInfo(
                shortName = detail?.rawString("shortName")
                    ?: financial?.rawString("shortName"),
                sector = profile?.rawString("sector"),
                industry = profile?.rawString("industry"),
                marketCap = detail?.rawLong("marketCap") ?: keyStats?.rawLong("marketCap"),
                trailingPE = detail?.rawDouble("trailingPE") ?: keyStats?.rawDouble("trailingPE"),
                forwardPE = detail?.rawDouble("forwardPE") ?: keyStats?.rawDouble("forwardPE"),
                eps = keyStats?.rawDouble("trailingEps"),
                dividendYield = detail?.rawDouble("dividendYield"),
                fiftyTwoWeekHigh = detail?.rawDouble("fiftyTwoWeekHigh"),
                fiftyTwoWeekLow = detail?.rawDouble("fiftyTwoWeekLow"),
                fiftyDayAverage = detail?.rawDouble("fiftyDayAverage"),
                twoHundredDayAverage = detail?.rawDouble("twoHundredDayAverage"),
                targetMeanPrice = financial?.rawDouble("targetMeanPrice"),
                revenuePerShare = financial?.rawDouble("revenuePerShare"),
                profitMargins = financial?.rawDouble("profitMargins"),
                returnOnEquity = financial?.rawDouble("returnOnEquity"),
                longBusinessSummary = profile?.rawString("longBusinessSummary")
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonObject.rawDouble(key: String): Double? =
        this[key]?.jsonObject?.get("raw")?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.rawLong(key: String): Long? =
        this[key]?.jsonObject?.get("raw")?.jsonPrimitive?.longOrNull

    private fun JsonObject.rawString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    suspend fun fetchQuote(ticker: String): StockQuote = withContext(Dispatchers.IO) {
        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=1d&interval=1d")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "InvestHelp/1.0")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        try {
            if (connection.responseCode != 200) {
                throw Exception("Yahoo Finance returned ${connection.responseCode}")
            }

            val body = connection.inputStream.bufferedReader().readText()
            val root = json.parseToJsonElement(body) as JsonObject
            val result = root["chart"]!!.jsonObject["result"]!!.jsonArray[0].jsonObject
            val meta = result["meta"]!!.jsonObject
            StockQuote(
                price = meta["regularMarketPrice"]!!.jsonPrimitive.double,
                previousClose = meta["chartPreviousClose"]?.jsonPrimitive?.double
                    ?: meta["previousClose"]?.jsonPrimitive?.double
                    ?: meta["regularMarketPrice"]!!.jsonPrimitive.double
            )
        } finally {
            connection.disconnect()
        }
    }
}
