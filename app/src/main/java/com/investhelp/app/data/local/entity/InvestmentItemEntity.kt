package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.investhelp.app.model.InvestmentType

@Entity(
    tableName = "investment_items",
    primaryKeys = ["ticker", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = InvestmentAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("ticker")]
)
data class InvestmentItemEntity(
    val ticker: String,
    val accountId: Long,
    val name: String,
    val type: InvestmentType,
    val currentPrice: Double,
    val quantity: Double,
    val cost: Double,
    val dayGainLoss: Double,
    val totalGainLoss: Double,
    val value: Double
)
