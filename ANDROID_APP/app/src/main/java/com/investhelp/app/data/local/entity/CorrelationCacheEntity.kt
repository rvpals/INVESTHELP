package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "correlation_cache")
data class CorrelationCacheEntity(
    @PrimaryKey val id: Int = 1,           // single-row table; always id = 1
    val tickersJson: String,               // JSON array  ["AAPL","MSFT",...]
    val matrixJson: String,                // JSON 2-D array [[1.0,0.8],[0.8,1.0],...]
    val marketCorrelationJson: String,     // JSON object {"AAPL":0.76,"MSFT":0.82,...}
    val failedTickersJson: String,         // JSON array  ["TSLA"] or []
    val calculatedAt: Long                 // epoch seconds
)
