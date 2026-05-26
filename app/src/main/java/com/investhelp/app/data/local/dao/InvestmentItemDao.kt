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

    @Query("SELECT * FROM investment_positions ORDER BY ticker ASC")
    fun getAllItems(): Flow<List<InvestmentItemEntity>>

    @Query("SELECT * FROM investment_positions")
    suspend fun getAllItemsSnapshot(): List<InvestmentItemEntity>

    @Query("DELETE FROM investment_positions")
    suspend fun deleteAll()

    @Query("SELECT * FROM investment_positions WHERE ticker = :ticker")
    suspend fun getItemByTicker(ticker: String): InvestmentItemEntity?

    @Query("SELECT * FROM investment_positions WHERE ticker = :ticker")
    fun observeItemByTicker(ticker: String): Flow<InvestmentItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: InvestmentItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InvestmentItemEntity>)

    @Query("DELETE FROM investment_positions WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("UPDATE investment_positions SET currentPrice = :price WHERE ticker = :ticker")
    suspend fun updatePriceByTicker(ticker: String, price: Double)

    @Query("UPDATE investment_positions SET logo = :logo WHERE ticker = :ticker")
    suspend fun updateLogoByTicker(ticker: String, logo: ByteArray)

    @Query(
        """
        UPDATE investment_positions
        SET name = :name, type = :type, currentPrice = :currentPrice
        WHERE ticker = :ticker
        """
    )
    suspend fun updateMetadataByTicker(ticker: String, name: String, type: InvestmentType, currentPrice: Double)

    @Query("SELECT COALESCE(SUM(value), 0.0) FROM investment_positions")
    suspend fun sumAllValues(): Double

    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM investment_positions WHERE ticker = :ticker")
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
