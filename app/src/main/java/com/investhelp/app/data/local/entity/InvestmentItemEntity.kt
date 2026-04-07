package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.investhelp.app.model.InvestmentType

@Entity(tableName = "investment_items")
data class InvestmentItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ticker: String? = null,
    val type: InvestmentType,
    val currentPrice: Double
)
