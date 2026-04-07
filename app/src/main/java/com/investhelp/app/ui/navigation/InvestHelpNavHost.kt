package com.investhelp.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.investhelp.app.auth.BiometricHelper
import com.investhelp.app.ui.account.AccountDetailScreen
import com.investhelp.app.ui.account.AccountFormScreen
import com.investhelp.app.ui.account.AccountListScreen
import com.investhelp.app.ui.account.AccountViewModel
import com.investhelp.app.ui.auth.AuthViewModel
import com.investhelp.app.ui.auth.SetupScreen
import com.investhelp.app.ui.auth.UnlockScreen
import com.investhelp.app.ui.dashboard.DashboardScreen
import com.investhelp.app.ui.dashboard.DashboardViewModel
import com.investhelp.app.ui.item.ItemDetailScreen
import com.investhelp.app.ui.item.ItemFormScreen
import com.investhelp.app.ui.item.ItemListScreen
import com.investhelp.app.ui.item.ItemStatisticsScreen
import com.investhelp.app.ui.item.ItemViewModel
import com.investhelp.app.ui.position.PositionScreen
import com.investhelp.app.ui.position.PositionViewModel
import com.investhelp.app.ui.settings.SettingsScreen
import com.investhelp.app.ui.settings.SettingsViewModel
import com.investhelp.app.ui.transaction.TransactionFormScreen
import com.investhelp.app.ui.transaction.TransactionListScreen
import com.investhelp.app.ui.transaction.TransactionViewModel

@Composable
fun InvestHelpNavHost(
    navController: NavHostController,
    startDestination: Any,
    activity: FragmentActivity,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<SetupRoute> {
            val viewModel: AuthViewModel = hiltViewModel()
            SetupScreen(
                onSetup = { password -> viewModel.setupPassword(password) }
            )
        }

        composable<UnlockRoute> {
            val viewModel: AuthViewModel = hiltViewModel()
            UnlockScreen(
                activity = activity,
                biometricHelper = viewModel.biometricHelper,
                onUnlockWithPassword = { password -> viewModel.unlockWithPassword(password) },
                onUnlockWithBiometric = { viewModel.unlockWithBiometric() }
            )
        }

        composable<DashboardRoute> {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAccount = { accountId ->
                    navController.navigate(AccountDetailRoute(accountId))
                },
                onAddAccount = {
                    navController.navigate(AccountFormRoute())
                }
            )
        }

        composable<AccountListRoute> {
            val viewModel: AccountViewModel = hiltViewModel()
            AccountListScreen(
                viewModel = viewModel,
                onNavigateToAccount = { accountId ->
                    navController.navigate(AccountDetailRoute(accountId))
                },
                onAddAccount = {
                    navController.navigate(AccountFormRoute())
                }
            )
        }

        composable<AccountDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AccountDetailRoute>()
            val viewModel: AccountViewModel = hiltViewModel()
            AccountDetailScreen(
                accountId = route.accountId,
                viewModel = viewModel,
                onEditAccount = {
                    navController.navigate(AccountFormRoute(route.accountId))
                },
                onNavigateToTransaction = { transactionId ->
                    navController.navigate(TransactionFormRoute(transactionId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AccountFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AccountFormRoute>()
            val viewModel: AccountViewModel = hiltViewModel()
            AccountFormScreen(
                accountId = if (route.accountId == -1L) null else route.accountId,
                viewModel = viewModel,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ItemListRoute> {
            val viewModel: ItemViewModel = hiltViewModel()
            ItemListScreen(
                viewModel = viewModel,
                onNavigateToItem = { itemId ->
                    navController.navigate(ItemDetailRoute(itemId))
                },
                onAddItem = {
                    navController.navigate(ItemFormRoute())
                }
            )
        }

        composable<ItemDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ItemDetailRoute>()
            val viewModel: ItemViewModel = hiltViewModel()
            ItemDetailScreen(
                itemId = route.itemId,
                viewModel = viewModel,
                onEditItem = {
                    navController.navigate(ItemFormRoute(route.itemId))
                },
                onViewStatistics = {
                    navController.navigate(ItemStatisticsRoute(route.itemId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ItemFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ItemFormRoute>()
            val viewModel: ItemViewModel = hiltViewModel()
            ItemFormScreen(
                itemId = if (route.itemId == -1L) null else route.itemId,
                viewModel = viewModel,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ItemStatisticsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ItemStatisticsRoute>()
            val viewModel: ItemViewModel = hiltViewModel()
            ItemStatisticsScreen(
                itemId = route.itemId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<TransactionListRoute> {
            val viewModel: TransactionViewModel = hiltViewModel()
            TransactionListScreen(
                viewModel = viewModel,
                onAddTransaction = {
                    navController.navigate(TransactionFormRoute())
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(TransactionFormRoute(transactionId))
                }
            )
        }

        composable<TransactionFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TransactionFormRoute>()
            val viewModel: TransactionViewModel = hiltViewModel()
            TransactionFormScreen(
                transactionId = if (route.transactionId == -1L) null else route.transactionId,
                viewModel = viewModel,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<PositionListRoute> {
            val viewModel: PositionViewModel = hiltViewModel()
            PositionScreen(viewModel = viewModel)
        }

        composable<SettingsRoute> {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = viewModel)
        }
    }
}
