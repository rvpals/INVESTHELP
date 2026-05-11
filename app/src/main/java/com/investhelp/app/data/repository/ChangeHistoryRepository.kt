package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.ChangeHistoryDao
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface ChangeHistoryRepository {
    fun getAllRecords(): Flow<List<ChangeHistoryEntity>>
    suspend fun getAllRecordsSnapshot(): List<ChangeHistoryEntity>
    suspend fun upsertRecord(record: ChangeHistoryEntity)
    suspend fun getRecordByDate(date: LocalDate): ChangeHistoryEntity?
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
}

@Singleton
class ChangeHistoryRepositoryImpl @Inject constructor(
    private val changeHistoryDao: ChangeHistoryDao
) : ChangeHistoryRepository {

    override fun getAllRecords(): Flow<List<ChangeHistoryEntity>> =
        changeHistoryDao.getAllRecords()

    override suspend fun getAllRecordsSnapshot(): List<ChangeHistoryEntity> =
        changeHistoryDao.getAllRecordsSnapshot()

    override suspend fun upsertRecord(record: ChangeHistoryEntity) =
        changeHistoryDao.upsertRecord(record)

    override suspend fun getRecordByDate(date: LocalDate): ChangeHistoryEntity? =
        changeHistoryDao.getRecordByDate(date.toEpochDay())

    override suspend fun deleteById(id: Long) =
        changeHistoryDao.deleteById(id)

    override suspend fun deleteAll() =
        changeHistoryDao.deleteAll()
}
