package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.CsvImportMappingEntity
import com.investhelp.app.data.local.entity.NamedCsvMappingEntity

@Dao
interface CsvImportMappingDao {

    @Query("SELECT * FROM csv_import_mappings WHERE importType = :importType")
    suspend fun getMapping(importType: String): CsvImportMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMapping(mapping: CsvImportMappingEntity)

    @Query("DELETE FROM csv_import_mappings WHERE importType = :importType")
    suspend fun deleteMapping(importType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNamedMapping(mapping: NamedCsvMappingEntity)

    @Query("SELECT * FROM csv_named_mappings WHERE importType = :importType ORDER BY name ASC")
    suspend fun getNamedMappings(importType: String): List<NamedCsvMappingEntity>

    @Query("SELECT * FROM csv_named_mappings WHERE id = :id")
    suspend fun getNamedMappingById(id: Long): NamedCsvMappingEntity?

    @Query("DELETE FROM csv_named_mappings WHERE id = :id")
    suspend fun deleteNamedMapping(id: Long)
}
