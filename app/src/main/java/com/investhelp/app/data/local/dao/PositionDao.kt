package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.PositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {

    @Query("SELECT * FROM positions ORDER BY ticker ASC")
    fun getAllPositions(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE ticker = :ticker AND accountId = :accountId")
    suspend fun getPosition(ticker: String, accountId: Long): PositionEntity?

    @Query("SELECT * FROM positions WHERE accountId = :accountId ORDER BY ticker ASC")
    fun getPositionsByAccount(accountId: Long): Flow<List<PositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosition(position: PositionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(positions: List<PositionEntity>)

    @Query("DELETE FROM positions WHERE ticker = :ticker AND accountId = :accountId")
    suspend fun deletePosition(ticker: String, accountId: Long)

    @Query("DELETE FROM positions")
    suspend fun deleteAll()

    @Query("SELECT * FROM positions")
    suspend fun getAllPositionsSnapshot(): List<PositionEntity>

    @Query("SELECT COALESCE(SUM(value), 0.0) FROM positions WHERE accountId = :accountId")
    suspend fun sumValueByAccount(accountId: Long): Double

    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM positions WHERE ticker = :ticker")
    suspend fun sumQuantityByTicker(ticker: String): Double
}
