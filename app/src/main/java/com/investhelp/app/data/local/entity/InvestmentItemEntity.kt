package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.investhelp.app.model.InvestmentType

@Entity(tableName = "investment_positions")
data class InvestmentItemEntity(
    @PrimaryKey
    val ticker: String,
    val name: String,
    val type: InvestmentType,
    val currentPrice: Double,
    val quantity: Double,
    val dayGainLoss: Double,
    val value: Double,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val logo: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InvestmentItemEntity) return false
        return ticker == other.ticker
    }

    override fun hashCode(): Int = ticker.hashCode()
}
