package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.ItemStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface InvestmentItemRepository {
    fun getAllItems(): Flow<List<InvestmentItemEntity>>
    fun observeItemByTicker(ticker: String): Flow<InvestmentItemEntity?>
    suspend fun getItemByTicker(ticker: String): InvestmentItemEntity?
    suspend fun getAllItemsSnapshot(): List<InvestmentItemEntity>
    suspend fun computeSharesOwned(ticker: String): Double
    suspend fun sumQuantityByTicker(ticker: String): Double
    suspend fun sumAllValues(): Double
    suspend fun getItemStatistics(ticker: String, startDate: LocalDate, endDate: LocalDate): ItemStatistics
    suspend fun upsertItem(item: InvestmentItemEntity)
    suspend fun upsertAll(items: List<InvestmentItemEntity>)
    suspend fun updatePriceByTicker(ticker: String, price: Double)
    suspend fun updateMetadataByTicker(ticker: String, name: String, type: InvestmentType, currentPrice: Double)
    suspend fun deleteByTicker(ticker: String)
    suspend fun deleteAll()
}

@Singleton
class InvestmentItemRepositoryImpl @Inject constructor(
    private val itemDao: InvestmentItemDao
) : InvestmentItemRepository {

    override fun getAllItems(): Flow<List<InvestmentItemEntity>> =
        itemDao.getAllItems()

    override fun observeItemByTicker(ticker: String): Flow<InvestmentItemEntity?> =
        itemDao.observeItemByTicker(ticker)

    override suspend fun getItemByTicker(ticker: String): InvestmentItemEntity? =
        itemDao.getItemByTicker(ticker)

    override suspend fun getAllItemsSnapshot(): List<InvestmentItemEntity> =
        itemDao.getAllItemsSnapshot()

    override suspend fun computeSharesOwned(ticker: String): Double =
        itemDao.computeSharesOwned(ticker)

    override suspend fun sumQuantityByTicker(ticker: String): Double =
        itemDao.sumQuantityByTicker(ticker)

    override suspend fun sumAllValues(): Double =
        itemDao.sumAllValues()

    override suspend fun getItemStatistics(
        ticker: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): ItemStatistics {
        val start = startDate.toEpochDay()
        val end = endDate.toEpochDay()
        return ItemStatistics(
            avgBuyPrice = itemDao.avgPrice(ticker, "Buy", start, end),
            maxBuyPrice = itemDao.maxPrice(ticker, "Buy", start, end),
            minBuyPrice = itemDao.minPrice(ticker, "Buy", start, end),
            avgSellPrice = itemDao.avgPrice(ticker, "Sell", start, end),
            maxSellPrice = itemDao.maxPrice(ticker, "Sell", start, end),
            minSellPrice = itemDao.minPrice(ticker, "Sell", start, end)
        )
    }

    override suspend fun upsertItem(item: InvestmentItemEntity) =
        itemDao.upsertItem(item)

    override suspend fun upsertAll(items: List<InvestmentItemEntity>) =
        itemDao.upsertAll(items)

    override suspend fun updatePriceByTicker(ticker: String, price: Double) =
        itemDao.updatePriceByTicker(ticker, price)

    override suspend fun updateMetadataByTicker(ticker: String, name: String, type: InvestmentType, currentPrice: Double) =
        itemDao.updateMetadataByTicker(ticker, name, type, currentPrice)

    override suspend fun deleteByTicker(ticker: String) =
        itemDao.deleteByTicker(ticker)

    override suspend fun deleteAll() =
        itemDao.deleteAll()
}
