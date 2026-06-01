package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.investhelp.app.data.local.entity.SqlLibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SqlLibraryDao {

    @Query("SELECT * FROM sql_library ORDER BY category, name")
    fun getAll(): Flow<List<SqlLibraryEntity>>

    @Query("SELECT * FROM sql_library ORDER BY category, name")
    suspend fun getAllSnapshot(): List<SqlLibraryEntity>

    @Query("SELECT DISTINCT category FROM sql_library ORDER BY category")
    fun getCategories(): Flow<List<String>>

    @Insert
    suspend fun insert(entry: SqlLibraryEntity)

    @Delete
    suspend fun delete(entry: SqlLibraryEntity)

    @Query("DELETE FROM sql_library WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sql_library")
    suspend fun deleteAll()
}
