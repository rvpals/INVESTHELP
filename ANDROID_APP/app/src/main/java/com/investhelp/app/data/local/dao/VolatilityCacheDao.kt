package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.VolatilityCacheEntity

@Dao
interface VolatilityCacheDao {
    @Query("SELECT * FROM volatility_cache ORDER BY type, annualizedVolPct ASC")
    suspend fun getAll(): List<VolatilityCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VolatilityCacheEntity>)

    @Query("DELETE FROM volatility_cache")
    suspend fun deleteAll()

    @Query("SELECT MAX(calculatedAt) FROM volatility_cache")
    suspend fun getLastCalculatedAt(): Long?
}
