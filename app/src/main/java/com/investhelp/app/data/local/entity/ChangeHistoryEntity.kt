package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "change_history",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class ChangeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val etfValue: Double,
    val stockValue: Double,
    val totalValue: Double,
    val dailyChangeEtf: Double = 0.0,
    val dailyChangeStock: Double = 0.0,
    val dailyChangeTotal: Double = 0.0
)
