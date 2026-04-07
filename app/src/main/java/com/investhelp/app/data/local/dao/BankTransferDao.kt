package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.investhelp.app.data.local.entity.BankTransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankTransferDao {

    @Query("SELECT * FROM bank_transfers ORDER BY date DESC")
    fun getAllTransfers(): Flow<List<BankTransferEntity>>

    @Query("SELECT * FROM bank_transfers ORDER BY date DESC")
    suspend fun getAllTransfersSnapshot(): List<BankTransferEntity>

    @Query("SELECT * FROM bank_transfers WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransfersByAccount(accountId: Long): Flow<List<BankTransferEntity>>

    @Query("SELECT * FROM bank_transfers WHERE id = :id")
    fun getTransferById(id: Long): Flow<BankTransferEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: BankTransferEntity): Long

    @Update
    suspend fun updateTransfer(transfer: BankTransferEntity)

    @Delete
    suspend fun deleteTransfer(transfer: BankTransferEntity)

    @Query("DELETE FROM bank_transfers")
    suspend fun deleteAll()
}
