package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.SharpeCacheEntity

@Dao
interface SharpeCacheDao {

    @Query("SELECT * FROM sharpe_ratio_cache WHERE id = 1 LIMIT 1")
    suspend fun get(): SharpeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SharpeCacheEntity)

    @Query("DELETE FROM sharpe_ratio_cache")
    suspend fun deleteAll()
}
