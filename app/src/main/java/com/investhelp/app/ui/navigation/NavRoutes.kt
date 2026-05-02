package com.investhelp.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object DashboardRoute
@Serializable object AccountListRoute
@Serializable data class AccountDetailRoute(val accountId: Long)
@Serializable data class AccountFormRoute(val accountId: Long = -1)
@Serializable object ItemListRoute
@Serializable data class ItemDetailRoute(val ticker: String)
@Serializable data class ItemFormRoute(val ticker: String = "", val accountId: Long = -1)
@Serializable data class ItemStatisticsRoute(val ticker: String)
@Serializable object TransactionListRoute
@Serializable data class TransactionFormRoute(val transactionId: Long = -1)
@Serializable data class AnalyzePriceRoute(val ticker: String)
@Serializable data class SimulationRoute(val ticker: String = "", val shares: Double = 0.0, val customDays: Int = 0)
@Serializable object BankTransferListRoute
@Serializable data class BankTransferFormRoute(val transferId: Long = -1)
@Serializable object SettingsRoute
@Serializable object SqlExplorerRoute
@Serializable object AccountPerformanceRoute
@Serializable object WatchListRoute
@Serializable object HelpRoute
