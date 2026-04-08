package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.model.InvestmentType
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentItemDao {

    @Query("SELECT * FROM investment_items ORDER BY ticker ASC")
    fun getAllItems(): Flow<List<InvestmentItemEntity>>

    @Query("SELECT * FROM investment_items")
    suspend fun getAllItemsSnapshot(): List<InvestmentItemEntity>

    @Query("DELETE FROM investment_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM investment_items WHERE ticker = :ticker")
    fun getItemsByTicker(ticker: String): Flow<List<InvestmentItemEntity>>

    @Query("SELECT * FROM investment_items WHERE ticker = :ticker LIMIT 1")
    suspend fun getFirstByTicker(ticker: String): InvestmentItemEntity?

    @Query("SELECT * FROM investment_items WHERE ticker = :ticker AND accountId = :accountId")
    suspend fun getItem(ticker: String, accountId: Long): InvestmentItemEntity?

    @Query("SELECT * FROM investment_items WHERE accountId = :accountId ORDER BY ticker ASC")
    fun getItemsByAccount(accountId: Long): Flow<List<InvestmentItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: InvestmentItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InvestmentItemEntity>)

    @Query("DELETE FROM investment_items WHERE ticker = :ticker AND accountId = :accountId")
    suspend fun deleteItem(ticker: String, accountId: Long)

    @Query("DELETE FROM investment_items WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("UPDATE investment_items SET currentPrice = :price WHERE ticker = :ticker")
    suspend fun updatePriceByTicker(ticker: String, price: Double)

    @Query(
        """
        UPDATE investment_items
        SET name = :name, type = :type, currentPrice = :currentPrice
        WHERE ticker = :ticker
        """
    )
    suspend fun updateMetadataByTicker(ticker: String, name: String, type: InvestmentType, currentPrice: Double)

    @Query("SELECT COALESCE(SUM(value), 0.0) FROM investment_items WHERE accountId = :accountId")
    suspend fun sumValueByAccount(accountId: Long): Double

    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM investment_items WHERE ticker = :ticker")
    suspend fun sumQuantityByTicker(ticker: String): Double

    @Query(
        """
        SELECT COALESCE(
            SUM(CASE WHEN action = 'Buy' THEN numberOfShares
                      WHEN action = 'Sell' THEN -numberOfShares
                      ELSE 0 END),
            0.0
        )
        FROM investment_transactions WHERE ticker = :ticker
        """
    )
    suspend fun computeSharesOwned(ticker: String): Double

    @Query(
        """
        SELECT AVG(pricePerShare) FROM investment_transactions
        WHERE ticker = :ticker AND action = :action
        AND date >= :startDate AND date <= :endDate
        """
    )
    suspend fun avgPrice(ticker: String, action: String, startDate: Long, endDate: Long): Double?

    @Query(
        """
        SELECT MAX(pricePerShare) FROM investment_transactions
        WHERE ticker = :ticker AND action = :action
        AND date >= :startDate AND date <= :endDate
        """
    )
    suspend fun maxPrice(ticker: String, action: String, startDate: Long, endDate: Long): Double?

    @Query(
        """
        SELECT MIN(pricePerShare) FROM investment_transactions
        WHERE ticker = :ticker AND action = :action
        AND date >= :startDate AND date <= :endDate
        """
    )
    suspend fun minPrice(ticker: String, action: String, startDate: Long, endDate: Long): Double?
}
