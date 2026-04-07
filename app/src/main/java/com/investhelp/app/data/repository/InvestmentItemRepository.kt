package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.model.ItemStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface InvestmentItemRepository {
    fun getAllItems(): Flow<List<InvestmentItemEntity>>
    fun getItemById(id: Long): Flow<InvestmentItemEntity?>
    suspend fun getItemByTicker(ticker: String): InvestmentItemEntity?
    suspend fun computeSharesOwned(ticker: String): Double
    suspend fun getItemStatistics(ticker: String, startDate: LocalDate, endDate: LocalDate): ItemStatistics
    suspend fun insertItem(item: InvestmentItemEntity): Long
    suspend fun updateItem(item: InvestmentItemEntity)
    suspend fun deleteItem(item: InvestmentItemEntity)
}

@Singleton
class InvestmentItemRepositoryImpl @Inject constructor(
    private val itemDao: InvestmentItemDao
) : InvestmentItemRepository {

    override fun getAllItems(): Flow<List<InvestmentItemEntity>> =
        itemDao.getAllItems()

    override fun getItemById(id: Long): Flow<InvestmentItemEntity?> =
        itemDao.getItemById(id)

    override suspend fun getItemByTicker(ticker: String): InvestmentItemEntity? =
        itemDao.getItemByTicker(ticker)

    override suspend fun computeSharesOwned(ticker: String): Double =
        itemDao.computeSharesOwned(ticker)

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

    override suspend fun insertItem(item: InvestmentItemEntity): Long =
        itemDao.insertItem(item)

    override suspend fun updateItem(item: InvestmentItemEntity) =
        itemDao.updateItem(item)

    override suspend fun deleteItem(item: InvestmentItemEntity) =
        itemDao.deleteItem(item)
}
