package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.investhelp.app.data.local.entity.CsvImportMappingEntity

@Dao
interface CsvImportMappingDao {

    @Query("SELECT * FROM csv_import_mappings WHERE importType = :importType")
    suspend fun getMapping(importType: String): CsvImportMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMapping(mapping: CsvImportMappingEntity)

    @Query("DELETE FROM csv_import_mappings WHERE importType = :importType")
    suspend fun deleteMapping(importType: String)
}
