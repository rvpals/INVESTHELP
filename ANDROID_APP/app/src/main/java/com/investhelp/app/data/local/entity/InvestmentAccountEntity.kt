package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "investment_accounts")
data class InvestmentAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val initialValue: Double,
    val lastUpdatedOn: LocalDateTime? = null,
    val lastValue: Double? = null
)
