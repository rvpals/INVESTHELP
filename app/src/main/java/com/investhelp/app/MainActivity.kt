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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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
import com.investhelp.app.ui.navigation.WatchListRoute
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Portfolio: ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = currencyFormat.format(uiState.totalPortfolioValue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (isRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon3D(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    baseColor = MaterialTheme.colorScheme.primary,
                    iconSize = 20.dp,
                    boxSize = 32.dp
                )
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
                    leadingIcon = { Icon3D(Icons.Default.AccountBalance, null, Color(0xFF1565C0), iconSize = 16.dp, boxSize = 28.dp) }
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
                    leadingIcon = { Icon3D(Icons.AutoMirrored.Filled.TrendingUp, null, Color(0xFF2E7D32), iconSize = 16.dp, boxSize = 28.dp) }
                )
                DropdownMenuItem(
                    text = { Text("Watch List") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(WatchListRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon3D(Icons.Default.Star, null, Color(0xFF7B1FA2), iconSize = 16.dp, boxSize = 28.dp) }
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
                    leadingIcon = { Icon3D(Icons.Default.Settings, null, Color(0xFF616161), iconSize = 16.dp, boxSize = 28.dp) }
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
                    leadingIcon = { Icon3D(Icons.Default.Storage, null, Color(0xFFE65100), iconSize = 16.dp, boxSize = 28.dp) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = { menuExpanded = false; showAbout = true },
                    leadingIcon = { Icon3D(Icons.Default.Info, null, Color(0xFF0277BD), iconSize = 16.dp, boxSize = 28.dp) }
                )
            }
        }
    )
}

@Composable
fun Icon3D(
    imageVector: ImageVector,
    contentDescription: String?,
    baseColor: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    boxSize: Dp = 34.dp
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.9f),
            baseColor,
            baseColor.copy(red = baseColor.red * 0.7f, green = baseColor.green * 0.7f, blue = baseColor.blue * 0.7f)
        )
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(boxSize)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .background(gradient, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = Color.White
        )
    }
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
                    Icon3D(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        baseColor = if (selected) item.tint else item.tint.copy(alpha = 0.4f),
                        iconSize = if (selected) 22.dp else 20.dp,
                        boxSize = if (selected) 36.dp else 32.dp
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
