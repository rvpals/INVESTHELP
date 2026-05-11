package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChangeHistoryDao {

    @Query("SELECT * FROM change_history ORDER BY date DESC")
    fun getAllRecords(): Flow<List<ChangeHistoryEntity>>

    @Query("SELECT * FROM change_history ORDER BY date DESC")
    suspend fun getAllRecordsSnapshot(): List<ChangeHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: ChangeHistoryEntity)

    @Query("SELECT * FROM change_history WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: Long): ChangeHistoryEntity?

    @Query("DELETE FROM change_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM change_history")
    suspend fun deleteAll()
}
