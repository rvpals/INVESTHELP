package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.investhelp.app.data.local.entity.AiLibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiLibraryDao {

    @Query("SELECT * FROM ai_library ORDER BY name")
    fun getAll(): Flow<List<AiLibraryEntity>>

    @Insert
    suspend fun insert(entry: AiLibraryEntity)

    @Delete
    suspend fun delete(entry: AiLibraryEntity)

    @Query("DELETE FROM ai_library WHERE id = :id")
    suspend fun deleteById(id: Long)
}
