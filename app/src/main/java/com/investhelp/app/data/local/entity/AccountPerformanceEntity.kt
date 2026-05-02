package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "account_performance",
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
        Index(value = ["accountId", "date"], unique = true)
    ]
)
data class AccountPerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val totalValue: Double,
    val date: LocalDate,
    val note: String = ""
)
