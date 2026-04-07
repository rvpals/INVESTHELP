package com.investhelp.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object SetupRoute
@Serializable object UnlockRoute
@Serializable object DashboardRoute
@Serializable object AccountListRoute
@Serializable data class AccountDetailRoute(val accountId: Long)
@Serializable data class AccountFormRoute(val accountId: Long = -1)
@Serializable object ItemListRoute
@Serializable data class ItemDetailRoute(val itemId: Long)
@Serializable data class ItemFormRoute(val itemId: Long = -1)
@Serializable data class ItemStatisticsRoute(val itemId: Long)
@Serializable object TransactionListRoute
@Serializable data class TransactionFormRoute(val transactionId: Long = -1)
@Serializable object PositionListRoute
@Serializable object SimulationRoute
@Serializable object SettingsRoute
