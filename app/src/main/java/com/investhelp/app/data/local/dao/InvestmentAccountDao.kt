package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentAccountDao {

    @Query("SELECT * FROM investment_accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<InvestmentAccountEntity>>

    @Query("SELECT * FROM investment_accounts")
    suspend fun getAllAccountsSnapshot(): List<InvestmentAccountEntity>

    @Query("DELETE FROM investment_accounts")
    suspend fun deleteAll()

    @Query("SELECT * FROM investment_accounts WHERE id = :id")
    fun getAccountById(id: Long): Flow<InvestmentAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: InvestmentAccountEntity): Long

    @Update
    suspend fun updateAccount(account: InvestmentAccountEntity)

    @Delete
    suspend fun deleteAccount(account: InvestmentAccountEntity)

    @Query(
        """
        SELECT COALESCE(SUM(p.value), 0.0)
        FROM investment_items p
        WHERE p.accountId = :accountId
        """
    )
    suspend fun computeCurrentValue(accountId: Long): Double
}
