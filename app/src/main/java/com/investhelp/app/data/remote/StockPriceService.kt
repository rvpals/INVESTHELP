package com.investhelp.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class StockQuote(
    val price: Double,
    val previousClose: Double
)

@Singleton
class StockPriceService @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchPrice(ticker: String): Double = fetchQuote(ticker).price

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
