package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "csv_named_mappings")
data class NamedCsvMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val importType: String,
    val mappingsJson: String,
    val dateFormatJson: String = ""
)
