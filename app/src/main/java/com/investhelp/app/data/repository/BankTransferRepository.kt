package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.BankTransferDao
import com.investhelp.app.data.local.entity.BankTransferEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface BankTransferRepository {
    fun getAllTransfers(): Flow<List<BankTransferEntity>>
    fun getTransfersByAccount(accountId: Long): Flow<List<BankTransferEntity>>
    fun getTransferById(id: Long): Flow<BankTransferEntity?>
    suspend fun insertTransfer(transfer: BankTransferEntity): Long
    suspend fun updateTransfer(transfer: BankTransferEntity)
    suspend fun deleteTransfer(transfer: BankTransferEntity)
}

@Singleton
class BankTransferRepositoryImpl @Inject constructor(
    private val dao: BankTransferDao
) : BankTransferRepository {
    override fun getAllTransfers() = dao.getAllTransfers()
    override fun getTransfersByAccount(accountId: Long) = dao.getTransfersByAccount(accountId)
    override fun getTransferById(id: Long) = dao.getTransferById(id)
    override suspend fun insertTransfer(transfer: BankTransferEntity) = dao.insertTransfer(transfer)
    override suspend fun updateTransfer(transfer: BankTransferEntity) = dao.updateTransfer(transfer)
    override suspend fun deleteTransfer(transfer: BankTransferEntity) = dao.deleteTransfer(transfer)
}
