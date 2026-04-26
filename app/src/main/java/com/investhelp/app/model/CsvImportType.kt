package com.investhelp.app.model

enum class CsvImportType(
    val label: String,
    val mappableFields: List<String>,
    val requiredFields: List<String>,
    val dateTimeFields: List<String>
) {
    Transaction(
        label = "Transaction Records",
        mappableFields = listOf(
            "Skip", "date", "time", "action", "accountName",
            "ticker", "numberOfShares", "pricePerShare", "totalAmount", "note"
        ),
        requiredFields = listOf("ticker", "numberOfShares", "pricePerShare", "action"),
        dateTimeFields = listOf("date", "time")
    ),
    Position(
        label = "Position Details",
        mappableFields = listOf(
            "Skip", "ticker", "name", "type", "currentPrice",
            "quantity", "cost", "dayGainLoss", "totalGainLoss", "value"
        ),
        requiredFields = listOf("ticker"),
        dateTimeFields = emptyList()
    ),
    Performance(
        label = "Performance Records",
        mappableFields = listOf(
            "Skip", "accountName", "totalValue", "date", "note"
        ),
        requiredFields = listOf("totalValue"),
        dateTimeFields = listOf("date")
    )
}
