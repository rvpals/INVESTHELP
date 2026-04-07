package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.investhelp.app.model.TransactionAction
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "investment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = InvestmentAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index("ticker")
    ]
)
data class InvestmentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime? = null,
    val action: TransactionAction,
    val accountId: Long,
    val ticker: String,
    val numberOfShares: Double,
    val pricePerShare: Double,
    val totalAmount: Double = 0.0,
    val note: String = ""
)
