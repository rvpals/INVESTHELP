package com.investhelp.app.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 5,
    val accounts: List<BackupAccount>,
    val items: List<BackupItem>,
    val transactions: List<BackupTransaction>,
    val bankTransfers: List<BackupBankTransfer> = emptyList(),
    val performanceRecords: List<BackupPerformanceRecord> = emptyList(),
    val watchLists: List<BackupWatchList> = emptyList(),
    val watchListItems: List<BackupWatchListItem> = emptyList(),
    val changeHistory: List<BackupChangeHistory> = emptyList(),
    val definitions: List<BackupDefinition> = emptyList(),
    val sqlLibrary: List<BackupSqlLibrary> = emptyList(),
    val aiLibrary: List<BackupAiLibrary> = emptyList()
)

@Serializable
data class BackupAccount(
    val id: Long,
    val name: String,
    val description: String,
    val initialValue: Double
)

@Serializable
data class BackupItem(
    val ticker: String = "",
    val name: String,
    val type: String,
    val currentPrice: Double,
    val quantity: Double = 0.0,
    val cost: Double = 0.0,
    val dayGainLoss: Double = 0.0,
    val totalGainLoss: Double = 0.0,
    val value: Double = 0.0,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val dividendRate: Double = 0.0,
    // compat fields (ignored in current export)
    val accountId: Long = -1,
    val id: Long = 0,
    val numShares: Double = 0.0
)

@Serializable
data class BackupTransaction(
    val id: Long,
    val dateEpochDay: Long,
    val timeSecondOfDay: Int? = null,
    val action: String,
    val accountId: Long = 0,
    val ticker: String,
    val numberOfShares: Double,
    val pricePerShare: Double,
    val totalAmount: Double = 0.0,
    val note: String = ""
)

@Serializable
data class BackupBankTransfer(
    val id: Long,
    val dateEpochDay: Long,
    val amount: Double,
    val accountId: Long,
    val note: String = ""
)

@Serializable
data class BackupPerformanceRecord(
    val id: Long,
    val accountId: Long,
    val totalValue: Double,
    val dateEpochDay: Long,
    val note: String = ""
)

@Serializable
data class BackupWatchList(
    val id: Long,
    val name: String
)

@Serializable
data class BackupWatchListItem(
    val id: Long,
    val watchListId: Long,
    val ticker: String,
    val shares: Double,
    val priceWhenAdded: Double,
    val addedDateEpochDay: Long,
    val reminderDateTimeEpochMs: Long? = null,
    val reminderMessage: String? = null
)

@Serializable
data class BackupChangeHistory(
    val id: Long,
    val dateEpochDay: Long,
    val etfValue: Double,
    val stockValue: Double,
    val totalValue: Double,
    val dailyChangeEtf: Double = 0.0,
    val dailyChangeStock: Double = 0.0,
    val dailyChangeTotal: Double = 0.0
)

@Serializable
data class BackupDefinition(
    val id: Long,
    val name: String,
    val description: String
)

@Serializable
data class BackupSqlLibrary(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,
    val sql: String
)

@Serializable
data class BackupAiLibrary(
    val id: Long,
    val name: String,
    val description: String,
    val promptText: String
)
