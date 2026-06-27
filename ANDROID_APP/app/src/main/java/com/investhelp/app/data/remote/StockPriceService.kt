package com.investhelp.app.data.remote

import com.investhelp.app.AppLog
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
    val previousClose: Double,
    val shortName: String? = null,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val quoteType: String? = null,
    val dividendRate: Double = 0.0
)

data class NewsArticle(
    val title: String,
    val link: String,
    val publisher: String,
    val publishedAt: Long // epoch seconds
)

data class TickerScanData(
    val twentyDaySma: Double,
    val avgVolume20Day: Long,
    val closingVolume: Long,
    val dayHigh: Double,
    val dayLow: Double,
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
    val trailingAnnualDividendRate: Double?,
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

data class YahooReportSection(
    val title: String,
    val fields: List<YahooReportField>
)

data class YahooReportField(
    val name: String,
    val value: String,
    val description: String
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

    suspend fun fetchPriceHistory(ticker: String, range: String, interval: String): List<HistoricalPrice> =
        withContext(Dispatchers.IO) {
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=${range}&interval=${interval}")
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

    suspend fun fetchHistoricalPrices(ticker: String, rangeDays: Int = 14, interval: String? = null): List<HistoricalPrice> =
        withContext(Dispatchers.IO) {
            val rangeParam = if (rangeDays == Int.MAX_VALUE) "max" else "${rangeDays}d"
            val intervalParam = interval ?: (if (rangeDays > 1825) "1wk" else "1d")
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

    suspend fun fetchPriceHistoryByPeriod(ticker: String, period1: Long, period2: Long, interval: String = "1d"): List<HistoricalPrice> =
        withContext(Dispatchers.IO) {
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?period1=${period1}&period2=${period2}&interval=${interval}")
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
                val timestamps = result["timestamp"]?.jsonArray?.map { it.jsonPrimitive.long } ?: emptyList()
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
            trailingAnnualDividendRate = detail?.rawDouble("trailingAnnualDividendRate"),
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

    suspend fun fetchLogo(ticker: String): ByteArray? = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://companiesmarketcap.com/img/company-logos/64/${ticker.lowercase()}.webp",
            "https://assets.parqet.com/logos/symbol/${ticker.uppercase()}?format=jpg",
            "https://storage.googleapis.com/iexcloud-hl37opg/api/logos/${ticker.uppercase()}.png"
        )
        for (logoUrl in urls) {
            try {
                val url = URL(logoUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                try {
                    if (connection.responseCode != 200) continue
                    val bytes = connection.inputStream.readBytes()
                    if (bytes.size > 100) {
                        AppLog.log("Logo fetched $ticker: ${bytes.size} bytes from $logoUrl")
                        return@withContext bytes
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) { }
        }
        AppLog.log("Logo fetch $ticker: no source available")
        null
    }

    suspend fun fetchQuote(ticker: String): StockQuote = withContext(Dispatchers.IO) {
        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=1d&interval=1m")
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
                    ?: meta["regularMarketPrice"]!!.jsonPrimitive.double,
                shortName = meta["shortName"]?.jsonPrimitive?.contentOrNull
                    ?: meta["longName"]?.jsonPrimitive?.contentOrNull,
                dayHigh = meta["regularMarketDayHigh"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                dayLow = meta["regularMarketDayLow"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                quoteType = meta["instrumentType"]?.jsonPrimitive?.contentOrNull
                    ?: meta["quoteType"]?.jsonPrimitive?.contentOrNull,
                dividendRate = meta["trailingAnnualDividendRate"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun fetchNews(ticker: String, count: Int = 5): List<NewsArticle> = withContext(Dispatchers.IO) {
        val url = URL("https://query2.finance.yahoo.com/v1/finance/search?q=${ticker}&newsCount=${count}&quotesCount=0&listsCount=0")
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
            val newsArray = root["news"]?.jsonArray ?: return@withContext emptyList()

            newsArray.mapNotNull { element ->
                val obj = element.jsonObject
                val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val link = obj["link"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val publisher = obj["publisher"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                val publishedAt = obj["providerPublishTime"]?.jsonPrimitive?.longOrNull ?: 0L
                NewsArticle(title = title, link = link, publisher = publisher, publishedAt = publishedAt)
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun fetchScanData(ticker: String): TickerScanData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=1mo&interval=1d")
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            try {
                if (connection.responseCode != 200) return@withContext null

                val body = connection.inputStream.bufferedReader().readText()
                val root = json.parseToJsonElement(body) as JsonObject
                val result = root["chart"]!!.jsonObject["result"]!!.jsonArray[0].jsonObject
                val meta = result["meta"]!!.jsonObject
                val indicators = result["indicators"]!!.jsonObject["quote"]!!.jsonArray[0].jsonObject
                val closes = indicators["close"]!!.jsonArray
                val volumes = indicators["volume"]!!.jsonArray

                val closePrices = closes.mapNotNull { it.jsonPrimitive.doubleOrNull }
                val volumeList = volumes.mapNotNull { it.jsonPrimitive.longOrNull }

                if (closePrices.isEmpty() || volumeList.isEmpty()) return@withContext null

                val last20Closes = closePrices.takeLast(20)
                val last20Volumes = volumeList.takeLast(20)
                val sma20 = last20Closes.average()
                val avgVolume = last20Volumes.average().toLong()
                val closingVolume = volumeList.last()

                val dayHigh = meta["regularMarketDayHigh"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val dayLow = meta["regularMarketDayLow"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val previousClose = meta["chartPreviousClose"]?.jsonPrimitive?.doubleOrNull
                    ?: meta["previousClose"]?.jsonPrimitive?.doubleOrNull ?: 0.0

                TickerScanData(
                    twentyDaySma = sma20,
                    avgVolume20Day = avgVolume,
                    closingVolume = closingVolume,
                    dayHigh = dayHigh,
                    dayLow = dayLow,
                    previousClose = previousClose
                )
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchFullReport(ticker: String): List<YahooReportSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<YahooReportSection>()

        // 1. Chart meta data
        try {
            val chartUrl = URL("https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=1d&interval=1d")
            val chartConn = chartUrl.openConnection() as HttpURLConnection
            chartConn.instanceFollowRedirects = true
            chartConn.setRequestProperty("User-Agent", "Mozilla/5.0")
            chartConn.connectTimeout = 10_000
            chartConn.readTimeout = 10_000
            try {
                if (chartConn.responseCode == 200) {
                    val body = chartConn.inputStream.bufferedReader().readText()
                    val root = json.parseToJsonElement(body) as JsonObject
                    val meta = root["chart"]!!.jsonObject["result"]!!.jsonArray[0].jsonObject["meta"]!!.jsonObject
                    val fields = mutableListOf<YahooReportField>()
                    meta["symbol"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Symbol", it, "Ticker symbol")) }
                    meta["shortName"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Short Name", it, "Company short name")) }
                    meta["longName"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Long Name", it, "Company full name")) }
                    meta["instrumentType"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Instrument Type", it, "Security type (EQUITY, ETF, MUTUALFUND, etc.)")) }
                    meta["quoteType"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Quote Type", it, "Quote classification")) }
                    meta["currency"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Currency", it, "Trading currency")) }
                    meta["exchangeName"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Exchange", it, "Exchange code")) }
                    meta["fullExchangeName"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Full Exchange Name", it, "Full name of the exchange")) }
                    meta["regularMarketPrice"]?.jsonPrimitive?.doubleOrNull?.let { fields.add(YahooReportField("Market Price", "%.2f".format(it), "Current regular market price")) }
                    meta["chartPreviousClose"]?.jsonPrimitive?.doubleOrNull?.let { fields.add(YahooReportField("Previous Close", "%.2f".format(it), "Previous trading day closing price")) }
                    meta["regularMarketDayHigh"]?.jsonPrimitive?.doubleOrNull?.let { fields.add(YahooReportField("Day High", "%.2f".format(it), "Highest price during current trading day")) }
                    meta["regularMarketDayLow"]?.jsonPrimitive?.doubleOrNull?.let { fields.add(YahooReportField("Day Low", "%.2f".format(it), "Lowest price during current trading day")) }
                    meta["regularMarketVolume"]?.jsonPrimitive?.longOrNull?.let { fields.add(YahooReportField("Volume", "%,d".format(it), "Number of shares traded today")) }
                    meta["fiftyTwoWeekHigh"]?.jsonPrimitive?.doubleOrNull?.let { fields.add(YahooReportField("52-Week High", "%.2f".format(it), "Highest price in the last 52 weeks")) }
                    meta["fiftyTwoWeekLow"]?.jsonPrimitive?.doubleOrNull?.let { fields.add(YahooReportField("52-Week Low", "%.2f".format(it), "Lowest price in the last 52 weeks")) }
                    meta["timezone"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Timezone", it, "Market timezone abbreviation")) }
                    meta["exchangeTimezoneName"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Timezone Name", it, "IANA timezone of the exchange")) }
                    if (fields.isNotEmpty()) sections.add(YahooReportSection("Market Data", fields))
                }
            } finally {
                chartConn.disconnect()
            }
        } catch (_: Exception) { }

        // 2. Quote Summary modules
        try {
            ensureCrumb()
            val modules = "summaryProfile,summaryDetail,financialData,defaultKeyStatistics,calendarEvents,recommendationTrend,earningsTrend,majorHoldersBreakdown,fundProfile,topHoldings"
            val summaryUrl = URL("https://query2.finance.yahoo.com/v10/finance/quoteSummary/${ticker}?modules=$modules&crumb=${java.net.URLEncoder.encode(cachedCrumb!!, "UTF-8")}")
            val summaryConn = summaryUrl.openConnection() as HttpURLConnection
            summaryConn.instanceFollowRedirects = true
            summaryConn.setRequestProperty("User-Agent", "Mozilla/5.0")
            summaryConn.setRequestProperty("Cookie", cachedCookie!!)
            summaryConn.connectTimeout = 15_000
            summaryConn.readTimeout = 15_000
            try {
                if (summaryConn.responseCode == 200) {
                    val body = summaryConn.inputStream.bufferedReader().readText()
                    val root = json.parseToJsonElement(body) as JsonObject
                    val result = root["quoteSummary"]!!.jsonObject["result"]!!.jsonArray[0].jsonObject

                    // Summary Detail
                    result["summaryDetail"]?.jsonObject?.let { detail ->
                        val fields = mutableListOf<YahooReportField>()
                        detail.rawDouble("marketCap")?.let { fields.add(YahooReportField("Market Cap", formatLargeNumber(it), "Total market value of outstanding shares")) }
                        detail.rawDouble("trailingPE")?.let { fields.add(YahooReportField("Trailing P/E", "%.2f".format(it), "Price-to-earnings ratio based on last 12 months earnings")) }
                        detail.rawDouble("forwardPE")?.let { fields.add(YahooReportField("Forward P/E", "%.2f".format(it), "Price-to-earnings ratio based on estimated future earnings")) }
                        detail.rawDouble("priceToSalesTrailing12Months")?.let { fields.add(YahooReportField("Price/Sales", "%.2f".format(it), "Price-to-sales ratio over trailing 12 months")) }
                        detail.rawDouble("priceToBook")?.let { fields.add(YahooReportField("Price/Book", "%.2f".format(it), "Market price per share divided by book value per share")) }
                        detail.rawDouble("dividendRate")?.let { fields.add(YahooReportField("Dividend Rate", "%.2f".format(it), "Annual dividend payment per share in dollars")) }
                        detail.rawDouble("dividendYield")?.let { fields.add(YahooReportField("Dividend Yield", "%.2f%%".format(it * 100), "Annual dividend as a percentage of current price")) }
                        detail.rawDouble("trailingAnnualDividendRate")?.let { fields.add(YahooReportField("Trailing Annual Dividend", "%.2f".format(it), "Total dividends paid per share over the last year")) }
                        detail.rawDouble("payoutRatio")?.let { fields.add(YahooReportField("Payout Ratio", "%.1f%%".format(it * 100), "Percentage of earnings paid as dividends")) }
                        detail.rawDouble("beta")?.let { fields.add(YahooReportField("Beta", "%.2f".format(it), "Volatility relative to the market (1.0 = same as market)")) }
                        detail.rawDouble("fiftyTwoWeekHigh")?.let { fields.add(YahooReportField("52-Week High", "%.2f".format(it), "Highest price in the last 52 weeks")) }
                        detail.rawDouble("fiftyTwoWeekLow")?.let { fields.add(YahooReportField("52-Week Low", "%.2f".format(it), "Lowest price in the last 52 weeks")) }
                        detail.rawDouble("fiftyDayAverage")?.let { fields.add(YahooReportField("50-Day Average", "%.2f".format(it), "Average closing price over the last 50 trading days")) }
                        detail.rawDouble("twoHundredDayAverage")?.let { fields.add(YahooReportField("200-Day Average", "%.2f".format(it), "Average closing price over the last 200 trading days")) }
                        detail.rawLong("averageVolume")?.let { fields.add(YahooReportField("Avg Volume", "%,d".format(it), "Average daily trading volume")) }
                        detail.rawLong("averageVolume10days")?.let { fields.add(YahooReportField("Avg Volume (10d)", "%,d".format(it), "Average volume over the last 10 days")) }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Valuation & Trading", fields))
                    }

                    // Financial Data
                    result["financialData"]?.jsonObject?.let { fin ->
                        val fields = mutableListOf<YahooReportField>()
                        fin.rawDouble("currentPrice")?.let { fields.add(YahooReportField("Current Price", "%.2f".format(it), "Most recent trading price")) }
                        fin.rawDouble("targetHighPrice")?.let { fields.add(YahooReportField("Analyst Target High", "%.2f".format(it), "Highest analyst price target")) }
                        fin.rawDouble("targetLowPrice")?.let { fields.add(YahooReportField("Analyst Target Low", "%.2f".format(it), "Lowest analyst price target")) }
                        fin.rawDouble("targetMeanPrice")?.let { fields.add(YahooReportField("Analyst Target Mean", "%.2f".format(it), "Average analyst price target")) }
                        fin.rawDouble("targetMedianPrice")?.let { fields.add(YahooReportField("Analyst Target Median", "%.2f".format(it), "Median analyst price target")) }
                        fin["recommendationKey"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Recommendation", it.uppercase(), "Consensus analyst recommendation (buy/hold/sell)")) }
                        fin.rawLong("numberOfAnalystOpinions")?.let { fields.add(YahooReportField("# of Analysts", it.toString(), "Number of analysts providing estimates")) }
                        fin.rawDouble("totalRevenue")?.let { fields.add(YahooReportField("Total Revenue", formatLargeNumber(it), "Total revenue over the last 12 months")) }
                        fin.rawDouble("revenuePerShare")?.let { fields.add(YahooReportField("Revenue/Share", "%.2f".format(it), "Revenue divided by outstanding shares")) }
                        fin.rawDouble("revenueGrowth")?.let { fields.add(YahooReportField("Revenue Growth", "%.1f%%".format(it * 100), "Year-over-year revenue growth rate")) }
                        fin.rawDouble("grossProfits")?.let { fields.add(YahooReportField("Gross Profits", formatLargeNumber(it), "Revenue minus cost of goods sold")) }
                        fin.rawDouble("grossMargins")?.let { fields.add(YahooReportField("Gross Margin", "%.1f%%".format(it * 100), "Gross profit as percentage of revenue")) }
                        fin.rawDouble("operatingMargins")?.let { fields.add(YahooReportField("Operating Margin", "%.1f%%".format(it * 100), "Operating income as percentage of revenue")) }
                        fin.rawDouble("profitMargins")?.let { fields.add(YahooReportField("Profit Margin", "%.1f%%".format(it * 100), "Net income as percentage of revenue")) }
                        fin.rawDouble("ebitda")?.let { fields.add(YahooReportField("EBITDA", formatLargeNumber(it), "Earnings before interest, taxes, depreciation, and amortization")) }
                        fin.rawDouble("operatingCashflow")?.let { fields.add(YahooReportField("Operating Cash Flow", formatLargeNumber(it), "Cash generated from core business operations")) }
                        fin.rawDouble("freeCashflow")?.let { fields.add(YahooReportField("Free Cash Flow", formatLargeNumber(it), "Operating cash flow minus capital expenditures")) }
                        fin.rawDouble("totalCash")?.let { fields.add(YahooReportField("Total Cash", formatLargeNumber(it), "Cash and cash equivalents on balance sheet")) }
                        fin.rawDouble("totalDebt")?.let { fields.add(YahooReportField("Total Debt", formatLargeNumber(it), "Sum of short-term and long-term debt")) }
                        fin.rawDouble("debtToEquity")?.let { fields.add(YahooReportField("Debt/Equity", "%.2f".format(it), "Total debt divided by shareholder equity")) }
                        fin.rawDouble("returnOnAssets")?.let { fields.add(YahooReportField("Return on Assets", "%.1f%%".format(it * 100), "Net income as percentage of total assets")) }
                        fin.rawDouble("returnOnEquity")?.let { fields.add(YahooReportField("Return on Equity", "%.1f%%".format(it * 100), "Net income as percentage of shareholder equity")) }
                        fin.rawDouble("earningsGrowth")?.let { fields.add(YahooReportField("Earnings Growth", "%.1f%%".format(it * 100), "Year-over-year earnings growth rate")) }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Financials", fields))
                    }

                    // Key Statistics
                    result["defaultKeyStatistics"]?.jsonObject?.let { stats ->
                        val fields = mutableListOf<YahooReportField>()
                        stats.rawDouble("trailingEps")?.let { fields.add(YahooReportField("Trailing EPS", "%.2f".format(it), "Earnings per share over the last 12 months")) }
                        stats.rawDouble("forwardEps")?.let { fields.add(YahooReportField("Forward EPS", "%.2f".format(it), "Estimated earnings per share for next 12 months")) }
                        stats.rawDouble("pegRatio")?.let { fields.add(YahooReportField("PEG Ratio", "%.2f".format(it), "P/E ratio divided by earnings growth rate; <1 may indicate undervaluation")) }
                        stats.rawDouble("bookValue")?.let { fields.add(YahooReportField("Book Value", "%.2f".format(it), "Net asset value per share")) }
                        stats.rawDouble("enterpriseValue")?.let { fields.add(YahooReportField("Enterprise Value", formatLargeNumber(it), "Market cap + debt - cash; total cost to acquire the company")) }
                        stats.rawDouble("enterpriseToRevenue")?.let { fields.add(YahooReportField("EV/Revenue", "%.2f".format(it), "Enterprise value divided by revenue")) }
                        stats.rawDouble("enterpriseToEbitda")?.let { fields.add(YahooReportField("EV/EBITDA", "%.2f".format(it), "Enterprise value divided by EBITDA; common valuation metric")) }
                        stats.rawLong("floatShares")?.let { fields.add(YahooReportField("Float Shares", "%,d".format(it), "Shares available for public trading")) }
                        stats.rawLong("sharesOutstanding")?.let { fields.add(YahooReportField("Shares Outstanding", "%,d".format(it), "Total number of shares issued")) }
                        stats.rawLong("sharesShort")?.let { fields.add(YahooReportField("Shares Short", "%,d".format(it), "Number of shares currently sold short")) }
                        stats.rawDouble("shortRatio")?.let { fields.add(YahooReportField("Short Ratio", "%.2f".format(it), "Days to cover short positions based on average volume")) }
                        stats.rawDouble("shortPercentOfFloat")?.let { fields.add(YahooReportField("Short % of Float", "%.1f%%".format(it * 100), "Percentage of float shares that are sold short")) }
                        stats.rawDouble("heldPercentInsiders")?.let { fields.add(YahooReportField("% Held by Insiders", "%.1f%%".format(it * 100), "Percentage of shares held by company insiders")) }
                        stats.rawDouble("heldPercentInstitutions")?.let { fields.add(YahooReportField("% Held by Institutions", "%.1f%%".format(it * 100), "Percentage of shares held by institutional investors")) }
                        stats.rawDouble("52WeekChange")?.let { fields.add(YahooReportField("52-Week Change", "%.1f%%".format(it * 100), "Price change over the last 52 weeks")) }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Key Statistics", fields))
                    }

                    // Summary Profile
                    result["summaryProfile"]?.jsonObject?.let { profile ->
                        val fields = mutableListOf<YahooReportField>()
                        profile["sector"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Sector", it, "Business sector classification")) }
                        profile["industry"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Industry", it, "Specific industry within the sector")) }
                        profile["fullTimeEmployees"]?.jsonPrimitive?.longOrNull?.let { fields.add(YahooReportField("Employees", "%,d".format(it), "Number of full-time employees")) }
                        profile["country"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Country", it, "Country of headquarters")) }
                        profile["city"]?.jsonPrimitive?.contentOrNull?.let { city ->
                            val state = profile["state"]?.jsonPrimitive?.contentOrNull
                            fields.add(YahooReportField("Location", if (state != null) "$city, $state" else city, "Headquarters location"))
                        }
                        profile["website"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Website", it, "Company website URL")) }
                        profile["longBusinessSummary"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Business Summary", it, "Description of the company's business")) }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Company Profile", fields))
                    }

                    // Calendar Events
                    result["calendarEvents"]?.jsonObject?.let { cal ->
                        val fields = mutableListOf<YahooReportField>()
                        cal["earnings"]?.jsonObject?.let { earnings ->
                            earnings["earningsDate"]?.jsonArray?.firstOrNull()?.jsonObject?.get("raw")?.jsonPrimitive?.longOrNull?.let {
                                val date = java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                                fields.add(YahooReportField("Next Earnings Date", date.toString(), "Expected next earnings report date"))
                            }
                        }
                        cal.rawLong("exDividendDate")?.let {
                            val date = java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            fields.add(YahooReportField("Ex-Dividend Date", date.toString(), "Date after which new buyers won't receive the next dividend"))
                        }
                        cal.rawLong("dividendDate")?.let {
                            val date = java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            fields.add(YahooReportField("Dividend Date", date.toString(), "Date the dividend is paid to shareholders"))
                        }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Upcoming Events", fields))
                    }

                    // Recommendation Trend
                    result["recommendationTrend"]?.jsonObject?.get("trend")?.jsonArray?.firstOrNull()?.jsonObject?.let { trend ->
                        val fields = mutableListOf<YahooReportField>()
                        trend["strongBuy"]?.jsonPrimitive?.longOrNull?.let { fields.add(YahooReportField("Strong Buy", it.toString(), "Analysts recommending Strong Buy")) }
                        trend["buy"]?.jsonPrimitive?.longOrNull?.let { fields.add(YahooReportField("Buy", it.toString(), "Analysts recommending Buy")) }
                        trend["hold"]?.jsonPrimitive?.longOrNull?.let { fields.add(YahooReportField("Hold", it.toString(), "Analysts recommending Hold")) }
                        trend["sell"]?.jsonPrimitive?.longOrNull?.let { fields.add(YahooReportField("Sell", it.toString(), "Analysts recommending Sell")) }
                        trend["strongSell"]?.jsonPrimitive?.longOrNull?.let { fields.add(YahooReportField("Strong Sell", it.toString(), "Analysts recommending Strong Sell")) }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Analyst Recommendations", fields))
                    }

                    // Fund Profile (ETFs)
                    result["fundProfile"]?.jsonObject?.let { fund ->
                        val fields = mutableListOf<YahooReportField>()
                        fund["categoryName"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Category", it, "Fund category classification")) }
                        fund["family"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Fund Family", it, "Asset management company")) }
                        fund["legalType"]?.jsonPrimitive?.contentOrNull?.let { fields.add(YahooReportField("Legal Type", it, "Legal structure of the fund")) }
                        fund["feesExpensesInvestment"]?.jsonObject?.let { fees ->
                            fees.rawDouble("annualReportExpenseRatio")?.let { fields.add(YahooReportField("Expense Ratio", "%.2f%%".format(it * 100), "Annual fee charged by the fund as percentage of assets")) }
                        }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Fund Profile", fields))
                    }

                    // Top Holdings (ETFs)
                    result["topHoldings"]?.jsonObject?.let { holdings ->
                        val fields = mutableListOf<YahooReportField>()
                        holdings["holdings"]?.jsonArray?.take(10)?.forEach { holding ->
                            val obj = holding.jsonObject
                            val sym = obj["symbol"]?.jsonPrimitive?.contentOrNull ?: "N/A"
                            val pct = obj["holdingPercent"]?.jsonObject?.get("raw")?.jsonPrimitive?.doubleOrNull
                            val pctStr = if (pct != null) "%.2f%%".format(pct * 100) else "N/A"
                            fields.add(YahooReportField(sym, pctStr, "Portfolio weight of this holding"))
                        }
                        if (fields.isNotEmpty()) sections.add(YahooReportSection("Top Holdings", fields))
                    }
                }
            } finally {
                summaryConn.disconnect()
            }
        } catch (_: Exception) { }

        sections
    }

    private fun formatLargeNumber(value: Double): String {
        return when {
            value >= 1_000_000_000_000 -> "${"%.2f".format(value / 1_000_000_000_000)}T"
            value >= 1_000_000_000 -> "${"%.2f".format(value / 1_000_000_000)}B"
            value >= 1_000_000 -> "${"%.2f".format(value / 1_000_000)}M"
            value >= 1_000 -> "${"%.0f".format(value)}"
            else -> "%.2f".format(value)
        }
    }
}
