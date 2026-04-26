package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.investhelp.app.model.InvestmentType

@Entity(tableName = "investment_items")
data class InvestmentItemEntity(
    @PrimaryKey
    val ticker: String,
    val name: String,
    val type: InvestmentType,
    val currentPrice: Double,
    val quantity: Double,
    val cost: Double,
    val dayGainLoss: Double,
    val totalGainLoss: Double,
    val value: Double,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0
)
