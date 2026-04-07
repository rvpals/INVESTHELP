package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentTransactionDao {

    @Query("SELECT * FROM investment_transactions ORDER BY date DESC, time DESC")
    fun getAllTransactions(): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions")
    suspend fun getAllTransactionsSnapshot(): List<InvestmentTransactionEntity>

    @Query("DELETE FROM investment_transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM investment_transactions WHERE accountId = :accountId ORDER BY date DESC, time DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE investmentItemId = :itemId ORDER BY date DESC, time DESC")
    fun getTransactionsByItem(itemId: Long): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<InvestmentTransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: InvestmentTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: InvestmentTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: InvestmentTransactionEntity)
}
