package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.AccountPerformanceDao
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface AccountPerformanceRepository {
    fun getAllRecords(): Flow<List<AccountPerformanceEntity>>
    fun getRecordsByAccount(accountId: Long): Flow<List<AccountPerformanceEntity>>
    fun getRecordsByAccounts(accountIds: List<Long>): Flow<List<AccountPerformanceEntity>>
    suspend fun existsRecord(accountId: Long, date: LocalDate): Boolean
    suspend fun insertRecord(record: AccountPerformanceEntity): Long
    suspend fun updateRecord(record: AccountPerformanceEntity)
    suspend fun deleteRecord(record: AccountPerformanceEntity)
}

@Singleton
class AccountPerformanceRepositoryImpl @Inject constructor(
    private val dao: AccountPerformanceDao
) : AccountPerformanceRepository {
    override fun getAllRecords() = dao.getAllRecords()
    override fun getRecordsByAccount(accountId: Long) = dao.getRecordsByAccount(accountId)
    override fun getRecordsByAccounts(accountIds: List<Long>) = dao.getRecordsByAccounts(accountIds)
    override suspend fun existsRecord(accountId: Long, date: LocalDate) =
        dao.countByAccountAndDate(accountId, date.toEpochDay()) > 0
    override suspend fun insertRecord(record: AccountPerformanceEntity) = dao.insertRecord(record)
    override suspend fun updateRecord(record: AccountPerformanceEntity) = dao.updateRecord(record)
    override suspend fun deleteRecord(record: AccountPerformanceEntity) = dao.deleteRecord(record)
}
