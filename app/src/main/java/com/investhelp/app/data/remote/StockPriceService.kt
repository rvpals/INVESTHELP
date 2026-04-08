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

    // Cached crumb + cookie for authenticated Yahoo Finance endpoints
    private var cachedCrumb: String? = null
    private var cachedCookie: String? = null

    private suspend fun ensureCrumb() {
        if (cachedCrumb != null && cachedCookie != null) return

        // Step 1: Hit Yahoo Finance to get a session cookie
        val consentUrl = URL("https://fc.yahoo.com/")
        val consentConn = consentUrl.openConnection() as HttpURLConnection
        consentConn.instanceFollowRedirects = true
        consentConn.setRequestProperty("User-Agent", "Mozilla/5.0")
        consentConn.connectTimeout = 10_000
        consentConn.readTimeout = 10_000
        try {
            consentConn.responseCode // trigger the request
        } catch (_: Exception) {
            // fc.yahoo.com may return 404 but still sets cookies
        }
        val cookies = consentConn.headerFields["Set-Cookie"]
            ?.joinToString("; ") { it.substringBefore(";") }
        consentConn.disconnect()

        if (cookies.isNullOrBlank()) throw Exception("Failed to obtain Yahoo session cookie")

        // Step 2: Fetch crumb using the session cookie
        val crumbUrl = URL("https://query2.finance.yahoo.com/v1/test/getcrumb")
        val crumbConn = crumbUrl.openConnection() as HttpURLConnection
        crumbConn.instanceFollowRedirects = true
        crumbConn.setRequestProperty("User-Agent", "Mozilla/5.0")
        crumbConn.setRequestProperty("Cookie", cookies)
        crumbConn.connectTimeout = 10_000
        crumbConn.readTimeout = 10_000
        try {
            if (crumbConn.responseCode != 200) {
                throw Exception("Failed to fetch crumb: ${crumbConn.responseCode}")
            }
            val crumb = crumbConn.inputStream.bufferedReader().readText().trim()
            if (crumb.isBlank()) throw Exception("Empty crumb returned")
            cachedCrumb = crumb
            cachedCookie = cookies
        } finally {
            crumbConn.disconnect()
        }
    }

    /** Invalidate crumb so the next call re-fetches it */
    private fun invalidateCrumb() {
        cachedCrumb = null
        cachedCookie = null
    }

    suspend fun fetchPrice(ticker: String): Double = fetchQuote(ticker).price

    suspend fun fetchHistoricalPrices(ticker: String, rangeDays: Int = 14): List<HistoricalPrice> =
        withContext(Dispatchers.IO) {
            val rangeParam = if (rangeDays == Int.MAX_VALUE) "max" else "${rangeDays}d"
            val intervalParam = if (rangeDays > 1825) "1wk" else if (rangeDays > 180) "1d" else "1d"
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=${rangeParam}&interval=${intervalParam}")
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000

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
        ensureCrumb()
        val modules = "summaryProfile,summaryDetail,financialData,defaultKeyStatistics"
        val url = URL("https://query2.finance.yahoo.com/v10/finance/quoteSummary/${ticker}?modules=$modules&crumb=${java.net.URLEncoder.encode(cachedCrumb!!, "UTF-8")}")
        val connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.setRequestProperty("Cookie", cachedCookie!!)
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {
            if (connection.responseCode == 401 || connection.responseCode == 403) {
                // Crumb expired, retry once
                connection.disconnect()
                invalidateCrumb()
                ensureCrumb()
                val retryUrl = URL("https://query2.finance.yahoo.com/v10/finance/quoteSummary/${ticker}?modules=$modules&crumb=${java.net.URLEncoder.encode(cachedCrumb!!, "UTF-8")}")
                val retryConn = retryUrl.openConnection() as HttpURLConnection
                retryConn.instanceFollowRedirects = true
                retryConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                retryConn.setRequestProperty("Cookie", cachedCookie!!)
                retryConn.connectTimeout = 15_000
                retryConn.readTimeout = 15_000
                try {
                    if (retryConn.responseCode != 200) {
                        throw Exception("Yahoo Finance returned ${retryConn.responseCode}")
                    }
                    return@withContext parseAnalysisResponse(retryConn.inputStream.bufferedReader().readText())
                } finally {
                    retryConn.disconnect()
                }
            }

            if (connection.responseCode != 200) {
                throw Exception("Yahoo Finance returned ${connection.responseCode}")
            }

            parseAnalysisResponse(connection.inputStream.bufferedReader().readText())
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAnalysisResponse(body: String): AnalysisInfo {
        val root = json.parseToJsonElement(body) as JsonObject
        val result = root["quoteSummary"]!!.jsonObject["result"]!!.jsonArray[0].jsonObject

        val profile = result["summaryProfile"]?.jsonObject
        val detail = result["summaryDetail"]?.jsonObject
        val financial = result["financialData"]?.jsonObject
        val keyStats = result["defaultKeyStatistics"]?.jsonObject

        return AnalysisInfo(
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
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
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
