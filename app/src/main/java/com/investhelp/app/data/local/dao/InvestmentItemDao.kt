package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentItemDao {

    @Query("SELECT * FROM investment_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<InvestmentItemEntity>>

    @Query("SELECT * FROM investment_items")
    suspend fun getAllItemsSnapshot(): List<InvestmentItemEntity>

    @Query("DELETE FROM investment_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM investment_items WHERE id = :id")
    fun getItemById(id: Long): Flow<InvestmentItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InvestmentItemEntity): Long

    @Update
    suspend fun updateItem(item: InvestmentItemEntity)

    @Delete
    suspend fun deleteItem(item: InvestmentItemEntity)

    @Query(
        """
        SELECT COALESCE(
            SUM(CASE WHEN action = 'Buy' THEN numberOfShares
                      WHEN action = 'Sell' THEN -numberOfShares
                      ELSE 0 END),
            0.0
        )
        FROM investment_transactions WHERE investmentItemId = :itemId
        """
    )
    suspend fun computeSharesOwned(itemId: Long): Double

    @Query(
        """
        SELECT AVG(pricePerShare) FROM investment_transactions
        WHERE investmentItemId = :itemId AND action = :action
        AND date >= :startDate AND date <= :endDate
        """
    )
    suspend fun avgPrice(itemId: Long, action: String, startDate: Long, endDate: Long): Double?

    @Query(
        """
        SELECT MAX(pricePerShare) FROM investment_transactions
        WHERE investmentItemId = :itemId AND action = :action
        AND date >= :startDate AND date <= :endDate
        """
    )
    suspend fun maxPrice(itemId: Long, action: String, startDate: Long, endDate: Long): Double?

    @Query(
        """
        SELECT MIN(pricePerShare) FROM investment_transactions
        WHERE investmentItemId = :itemId AND action = :action
        AND date >= :startDate AND date <= :endDate
        """
    )
    suspend fun minPrice(itemId: Long, action: String, startDate: Long, endDate: Long): Double?
}
