package com.investhelp.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.documentfile.provider.DocumentFile
import android.net.Uri
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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.runtime.LaunchedEffect
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
import com.investhelp.app.ui.navigation.DashboardRoute
import com.investhelp.app.ui.navigation.InvestHelpNavHost
import com.investhelp.app.ui.navigation.ItemDetailRoute
import com.investhelp.app.ui.navigation.AccountPerformanceRoute
import com.investhelp.app.ui.navigation.SettingsRoute
import com.investhelp.app.ui.navigation.NextDayActionsRoute
import com.investhelp.app.ui.navigation.SqlExplorerRoute
import com.investhelp.app.ui.navigation.SimulationRoute
import com.investhelp.app.ui.navigation.TransactionListRoute
import com.investhelp.app.ui.navigation.WatchListRoute
import com.investhelp.app.ui.navigation.PositionDetailRoute
import com.investhelp.app.ui.navigation.HelpRoute
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.BarChart
import com.investhelp.app.ui.navigation.VolatilityAnalysisRoute
import android.util.Base64
import com.investhelp.app.data.local.DatabaseProvider
import com.investhelp.app.ui.settings.SettingsViewModel
import com.investhelp.app.ui.theme.InvestHelpTheme
import com.investhelp.app.ui.theme.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val tint: Color
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Default.Dashboard, DashboardRoute, Color(0xFF1E88E5)),
    BottomNavItem("Positions", Icons.Default.PieChart, PositionDetailRoute, Color(0xFF43A047)),
    BottomNavItem("Transaction", Icons.Default.Receipt, TransactionListRoute, Color(0xFF8E24AA))
)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var dbProvider: DatabaseProvider

    private var pendingTicker: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreferences.init(this)
        pendingTicker = intent?.getStringExtra(ReminderReceiver.EXTRA_TICKER)
        setContent {
            InvestHelpTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    pendingTicker?.let { ticker ->
                        navController.navigate(ItemDetailRoute(ticker)) {
                            launchSingleTop = true
                        }
                        pendingTicker = null
                    }
                }

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

    private val EXCLUDED_TABLES = setOf(
        "room_master_table", "android_metadata", "sqlite_sequence",
        "volatility_cache"  // cached computed data — regenerated on demand, not user data
    )

    private fun exportAllTablesGeneric(): String {
        val db = dbProvider.database.openHelper.readableDatabase
        val tablesCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
        val tables = mutableListOf<String>()
        while (tablesCursor.moveToNext()) {
            val name = tablesCursor.getString(0)
            if (name !in EXCLUDED_TABLES && !name.startsWith("sqlite_")) tables.add(name)
        }
        tablesCursor.close()

        val sb = StringBuilder()
        sb.append("{\"version\":6,\"tables\":{")
        tables.forEachIndexed { tIdx, table ->
            if (tIdx > 0) sb.append(",")
            sb.append("\"$table\":[")
            val colCursor = db.query("PRAGMA table_info($table)")
            val cols = mutableListOf<Pair<String, String>>()
            while (colCursor.moveToNext()) {
                cols.add(colCursor.getString(1) to colCursor.getString(2).uppercase())
            }
            colCursor.close()

            val dataCursor = db.query("SELECT * FROM $table")
            var rIdx = 0
            while (dataCursor.moveToNext()) {
                if (rIdx > 0) sb.append(",")
                sb.append("{")
                cols.forEachIndexed { cIdx, (colName, colType) ->
                    if (cIdx > 0) sb.append(",")
                    sb.append("\"$colName\":")
                    if (dataCursor.isNull(cIdx)) {
                        sb.append("null")
                    } else when {
                        colType.contains("BLOB") -> {
                            val bytes = dataCursor.getBlob(cIdx)
                            sb.append("\"${Base64.encodeToString(bytes, Base64.NO_WRAP)}\"")
                        }
                        colType.contains("INT") -> sb.append(dataCursor.getLong(cIdx))
                        colType.contains("REAL") || colType.contains("FLOAT") || colType.contains("DOUBLE") ->
                            sb.append(dataCursor.getDouble(cIdx))
                        else -> {
                            val str = dataCursor.getString(cIdx)
                                .replace("\\", "\\\\").replace("\"", "\\\"")
                                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
                            sb.append("\"$str\"")
                        }
                    }
                }
                sb.append("}")
                rIdx++
            }
            dataCursor.close()
            sb.append("]")
        }
        sb.append("}}")
        return sb.toString()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        run {
            val prefs = getSharedPreferences(SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
            val autoBackup = prefs.getBoolean(SettingsViewModel.KEY_AUTO_BACKUP_ON_EXIT, false)
            val folderUriStr = prefs.getString(SettingsViewModel.KEY_BACKUP_FOLDER_URI, null)
            if (!autoBackup || folderUriStr == null) return@run
            val lastBackupTime = prefs.getString(SettingsViewModel.KEY_LAST_AUTO_BACKUP_TIME, null)
            if (lastBackupTime != null) {
                val last = LocalDateTime.parse(lastBackupTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                if (java.time.Duration.between(last, LocalDateTime.now()).toMinutes() < 30) return@run
            }
            val folderUri = Uri.parse(folderUriStr)
            val keepCount = prefs.getInt(
                SettingsViewModel.KEY_AUTO_BACKUP_KEEP_COUNT,
                SettingsViewModel.DEFAULT_AUTO_BACKUP_KEEP_COUNT
            )
            runBlocking(Dispatchers.IO) {
                try {
                    val jsonString = exportAllTablesGeneric()
                    val timestamp = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
                    )
                    val fileName = "invest_help_backup_$timestamp.json"

                    val folder = DocumentFile.fromTreeUri(this@MainActivity, folderUri)
                        ?: return@runBlocking

                    val existingBackups = folder.listFiles()
                        .filter { it.name?.startsWith("invest_help_backup_") == true && it.name?.endsWith(".json") == true }
                        .sortedBy { it.name }

                    val toDelete = existingBackups.size - keepCount + 1
                    if (toDelete > 0) {
                        existingBackups.take(toDelete).forEach { it.delete() }
                    }

                    val file = folder.createFile("application/json", fileName)
                    file?.uri?.let { fileUri ->
                        contentResolver.openOutputStream(fileUri)?.use { out ->
                            out.write(jsonString.toByteArray())
                        }
                        prefs.edit().putString(
                            SettingsViewModel.KEY_LAST_AUTO_BACKUP_TIME,
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        ).apply()
                    }
                } catch (_: Exception) {
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
    val refreshStatus by dashboardViewModel.refreshStatus.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    var menuExpanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchTicker by remember { mutableStateOf("") }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Invest Help") },
            text = {
                Column {
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Investment tracking app for managing portfolios, positions, and transactions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("What's New", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Positions tabs: flat Row layout with STOCK/ETF/Analysis/Dividend (icon + equal width)\n" +
                        "• Dividend tab: total annual income, Stock/ETF exploding pie charts, sortable tables\n" +
                        "• Generic backup/restore (v6) — auto-discovers all tables via sqlite_master\n" +
                        "• Removed dead code: ItemListScreen, ItemStatisticsScreen, unused routes",
                        style = MaterialTheme.typography.bodySmall
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

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = {
                showSearchDialog = false
                searchTicker = ""
            },
            title = { Text("Search Ticker") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = searchTicker,
                    onValueChange = { searchTicker = it.uppercase() },
                    label = { Text("Ticker") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = searchTicker.trim()
                        if (t.isNotEmpty()) {
                            showSearchDialog = false
                            searchTicker = ""
                            navController.navigate(ItemDetailRoute(t))
                        }
                    },
                    enabled = searchTicker.trim().isNotEmpty()
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSearchDialog = false
                    searchTicker = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = { showSearchDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search Ticker",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
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
            IconButton(
                onClick = {
                    navController.navigate(WatchListRoute) {
                        popUpTo(DashboardRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon3D(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Watch List",
                    baseColor = Color(0xFF7B1FA2),
                    iconSize = 18.dp,
                    boxSize = 30.dp
                )
            }
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
                    leadingIcon = { Icon3D(Icons.Default.ShowChart, null, Color(0xFF2E7D32), iconSize = 16.dp, boxSize = 28.dp) }
                )
                DropdownMenuItem(
                    text = { Text("Simulation") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(SimulationRoute()) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon3D(Icons.AutoMirrored.Filled.TrendingUp, null, Color(0xFFE53935), iconSize = 16.dp, boxSize = 28.dp) }
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
                    text = { Text("Next-Day Actions") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(NextDayActionsRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon3D(Icons.AutoMirrored.Filled.TrendingUp, null, Color(0xFF00897B), iconSize = 16.dp, boxSize = 28.dp) }
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
                DropdownMenuItem(
                    text = { Text("Help") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(HelpRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon3D(Icons.Default.HelpOutline, null, Color(0xFF00838F), iconSize = 16.dp, boxSize = 28.dp) }
                )
                DropdownMenuItem(
                    text = { Text("Volatility Analysis") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(VolatilityAnalysisRoute) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    leadingIcon = { Icon3D(Icons.Default.BarChart, null, Color(0xFF7B1FA2), iconSize = 16.dp, boxSize = 28.dp) }
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

    if (isRefreshing && refreshStatus != null) {
        val status = refreshStatus!!
        val sign = if (status.changeAmount >= 0) "+" else ""
        val changeColor = if (status.changeAmount >= 0)
            Color(0xFF2E7D32) else Color(0xFFC62828)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Updating ${status.ticker}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${currencyFormat.format(status.price)}/share (${sign}${currencyFormat.format(status.changeAmount)}; ${sign}${"%.2f".format(status.changePercent)}%)",
                style = MaterialTheme.typography.labelMedium,
                color = changeColor
            )
        }
    }
    } // Column
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
