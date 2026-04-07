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
    suspend fun computeSharesOwned(itemId: Long): Double
    suspend fun getItemStatistics(itemId: Long, startDate: LocalDate, endDate: LocalDate): ItemStatistics
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

    override suspend fun computeSharesOwned(itemId: Long): Double =
        itemDao.computeSharesOwned(itemId)

    override suspend fun getItemStatistics(
        itemId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): ItemStatistics {
        val start = startDate.toEpochDay()
        val end = endDate.toEpochDay()
        return ItemStatistics(
            avgBuyPrice = itemDao.avgPrice(itemId, "Buy", start, end),
            maxBuyPrice = itemDao.maxPrice(itemId, "Buy", start, end),
            minBuyPrice = itemDao.minPrice(itemId, "Buy", start, end),
            avgSellPrice = itemDao.avgPrice(itemId, "Sell", start, end),
            maxSellPrice = itemDao.maxPrice(itemId, "Sell", start, end),
            minSellPrice = itemDao.minPrice(itemId, "Sell", start, end)
        )
    }

    override suspend fun insertItem(item: InvestmentItemEntity): Long =
        itemDao.insertItem(item)

    override suspend fun updateItem(item: InvestmentItemEntity) =
        itemDao.updateItem(item)

    override suspend fun deleteItem(item: InvestmentItemEntity) =
        itemDao.deleteItem(item)
}
