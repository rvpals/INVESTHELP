package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "positions",
    primaryKeys = ["ticker", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = InvestmentAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class PositionEntity(
    val ticker: String,
    val accountId: Long,
    val quantity: Double,
    val cost: Double,
    val dayGainLoss: Double,
    val totalGainLoss: Double,
    val value: Double
)
