package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "volatility_cache")
data class VolatilityCacheEntity(
    @PrimaryKey val ticker: String,
    val companyName: String?,
    val type: String,
    val shares: Double,
    val currentPrice: Double,
    val annualizedVolPct: Double,
    val dailyStdDevPct: Double,
    val volatilityLabel: String,
    val low52w: Double,
    val high52w: Double,
    val rangePositionPct: Double,
    val sampleCount: Int,
    val calculatedAt: Long
)
