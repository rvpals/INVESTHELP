package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.investhelp.app.data.local.entity.DefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DefinitionDao {

    @Query("SELECT * FROM definitions ORDER BY name ASC")
    fun getAllDefinitions(): Flow<List<DefinitionEntity>>

    @Query("SELECT * FROM definitions ORDER BY name ASC")
    suspend fun getAllDefinitionsSnapshot(): List<DefinitionEntity>

    @Query("SELECT * FROM definitions WHERE id = :id")
    suspend fun getDefinitionById(id: Long): DefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinition(definition: DefinitionEntity): Long

    @Update
    suspend fun updateDefinition(definition: DefinitionEntity)

    @Delete
    suspend fun deleteDefinition(definition: DefinitionEntity)

    @Query("DELETE FROM definitions")
    suspend fun deleteAll()
}
