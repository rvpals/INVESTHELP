package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<InvestmentTransactionEntity>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<InvestmentTransactionEntity>>
    fun getTransactionsByTicker(ticker: String): Flow<List<InvestmentTransactionEntity>>
    fun getTransactionById(id: Long): Flow<InvestmentTransactionEntity?>
    suspend fun insertTransaction(transaction: InvestmentTransactionEntity): Long
    suspend fun updateTransaction(transaction: InvestmentTransactionEntity)
    suspend fun deleteTransaction(transaction: InvestmentTransactionEntity)
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: InvestmentTransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<InvestmentTransactionEntity>> =
        transactionDao.getAllTransactions()

    override fun getTransactionsByAccount(accountId: Long): Flow<List<InvestmentTransactionEntity>> =
        transactionDao.getTransactionsByAccount(accountId)

    override fun getTransactionsByTicker(ticker: String): Flow<List<InvestmentTransactionEntity>> =
        transactionDao.getTransactionsByTicker(ticker)

    override fun getTransactionById(id: Long): Flow<InvestmentTransactionEntity?> =
        transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: InvestmentTransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: InvestmentTransactionEntity) =
        transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: InvestmentTransactionEntity) =
        transactionDao.deleteTransaction(transaction)
}
