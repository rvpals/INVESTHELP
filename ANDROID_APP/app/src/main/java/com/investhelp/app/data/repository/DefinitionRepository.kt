package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.DefinitionDao
import com.investhelp.app.data.local.entity.DefinitionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface DefinitionRepository {
    fun getAllDefinitions(): Flow<List<DefinitionEntity>>
    suspend fun getDefinitionById(id: Long): DefinitionEntity?
    suspend fun insertDefinition(definition: DefinitionEntity): Long
    suspend fun updateDefinition(definition: DefinitionEntity)
    suspend fun deleteDefinition(definition: DefinitionEntity)
    suspend fun deleteAll()
}

@Singleton
class DefinitionRepositoryImpl @Inject constructor(
    private val definitionDao: DefinitionDao
) : DefinitionRepository {

    override fun getAllDefinitions(): Flow<List<DefinitionEntity>> =
        definitionDao.getAllDefinitions()

    override suspend fun getDefinitionById(id: Long): DefinitionEntity? =
        definitionDao.getDefinitionById(id)

    override suspend fun insertDefinition(definition: DefinitionEntity): Long =
        definitionDao.insertDefinition(definition)

    override suspend fun updateDefinition(definition: DefinitionEntity) =
        definitionDao.updateDefinition(definition)

    override suspend fun deleteDefinition(definition: DefinitionEntity) =
        definitionDao.deleteDefinition(definition)

    override suspend fun deleteAll() =
        definitionDao.deleteAll()
}
