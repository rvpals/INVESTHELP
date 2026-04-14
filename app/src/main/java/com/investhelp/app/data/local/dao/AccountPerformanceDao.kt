package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountPerformanceDao {

    @Query("SELECT * FROM account_performance ORDER BY dateTime DESC")
    fun getAllRecords(): Flow<List<AccountPerformanceEntity>>

    @Query("SELECT * FROM account_performance ORDER BY dateTime DESC")
    suspend fun getAllRecordsSnapshot(): List<AccountPerformanceEntity>

    @Query("SELECT * FROM account_performance WHERE accountId = :accountId ORDER BY dateTime ASC")
    fun getRecordsByAccount(accountId: Long): Flow<List<AccountPerformanceEntity>>

    @Query("SELECT * FROM account_performance WHERE accountId IN (:accountIds) ORDER BY dateTime ASC")
    fun getRecordsByAccounts(accountIds: List<Long>): Flow<List<AccountPerformanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AccountPerformanceEntity): Long

    @Delete
    suspend fun deleteRecord(record: AccountPerformanceEntity)

    @Query("DELETE FROM account_performance")
    suspend fun deleteAll()
}
