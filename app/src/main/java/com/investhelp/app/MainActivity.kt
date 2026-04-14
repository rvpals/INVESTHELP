package com.investhelp.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.Delete
import com.investhelp.app.ui.dashboard.DashboardViewModel
import com.investhelp.app.ui.navigation.AccountListRoute
import com.investhelp.app.ui.navigation.BankTransferListRoute
import com.investhelp.app.ui.navigation.DashboardRoute
import com.investhelp.app.ui.navigation.InvestHelpNavHost
import com.investhelp.app.ui.navigation.ItemListRoute
import com.investhelp.app.ui.navigation.AccountPerformanceRoute
import com.investhelp.app.ui.navigation.SettingsRoute
import com.investhelp.app.ui.navigation.SqlExplorerRoute
import com.investhelp.app.ui.navigation.SimulationRoute
import com.investhelp.app.ui.navigation.TransactionListRoute
import com.investhelp.app.ui.theme.InvestHelpTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val tint: Color
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Default.Dashboard, DashboardRoute, Color(0xFF1E88E5)),
    BottomNavItem("Items", Icons.Default.PieChart, ItemListRoute, Color(0xFF43A047)),
    BottomNavItem("Transfer", Icons.Default.AccountBalanceWallet, BankTransferListRoute, Color(0xFFF57C00)),
    BottomNavItem("Transaction", Icons.Default.Receipt, TransactionListRoute, Color(0xFF8E24AA)),
    BottomNavItem("Simulation", Icons.AutoMirrored.Filled.TrendingUp, SimulationRoute(), Color(0xFFE53935))
)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InvestHelpTheme {
                val navController = rememberNavController()

                Scaffold(
                    topBar = {
                        GlobalTopBar(navController)
                    },
                    bottomBar = {
                        BottomNavigationBar(navController)
                    }
                ) { padding ->
                    InvestHelpNavHost(
                        navController = navController,
                        startDestination = DashboardRoute,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTopBar(navController: NavHostController) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    var menuExpanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Invest Help") },
            text = {
                Column {
                    Text("Version 1.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Investment tracking app for managing portfolios, positions, and transactions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbout = false; showLog = true }) {
                    Text("Show Log")
                }
            }
        )
    }

    if (showLog) {
        val logEntries = remember { AppLog.entries }
        AlertDialog(
            onDismissRequest = { showLog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Application Log")
                    IconButton(onClick = { AppLog.clear(); showLog = false }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear log")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (logEntries.isEmpty()) {
                        Text(
                            "No log entries yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        logEntries.asReversed().forEach { entry ->
                            Text(
                                text = entry.formatted(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLog = false }) {
                    Text("Close")
                }
            }
        )
    }

    TopAppBar(
        title = {
            Card(
                onClick = {
                    dashboardViewModel.refreshAllPrices()
                    navController.navigate(DashboardRoute) {
                        popUpTo(DashboardRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = MaterialTheme.shapes.medium
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Portfolio: ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = currencyFormat.format(uiState.totalPortfolioValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Accounts") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(AccountListRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Performance") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(AccountPerformanceRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(SettingsRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("SQL Explorer") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(SqlExplorerRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = { menuExpanded = false; showAbout = true },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        }
    )
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier.shadow(elevation = 12.dp),
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selected) item.tint else item.tint.copy(alpha = 0.5f)
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        color = if (selected) item.tint else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                selected = selected,
                alwaysShowLabel = true,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
