package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "csv_import_mappings")
data class CsvImportMappingEntity(
    @PrimaryKey val importType: String,
    val mappingsJson: String,
    val dateFormatJson: String = ""
)
