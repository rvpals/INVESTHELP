package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.CorrelationCacheEntity

@Dao
interface CorrelationCacheDao {

    @Query("SELECT * FROM correlation_cache WHERE id = 1 LIMIT 1")
    suspend fun get(): CorrelationCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CorrelationCacheEntity)

    @Query("DELETE FROM correlation_cache")
    suspend fun deleteAll()

    @Query("SELECT calculatedAt FROM correlation_cache WHERE id = 1 LIMIT 1")
    suspend fun getCalculatedAt(): Long?
}
