package com.investhelp.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.investhelp.app.auth.AuthState
import com.investhelp.app.ui.auth.AuthViewModel
import com.investhelp.app.ui.navigation.AccountListRoute
import com.investhelp.app.ui.navigation.DashboardRoute
import com.investhelp.app.ui.navigation.InvestHelpNavHost
import com.investhelp.app.ui.navigation.ItemListRoute
import com.investhelp.app.ui.navigation.PositionListRoute
import com.investhelp.app.ui.navigation.SettingsRoute
import com.investhelp.app.ui.navigation.SetupRoute
import com.investhelp.app.ui.navigation.TransactionListRoute
import com.investhelp.app.ui.navigation.UnlockRoute
import com.investhelp.app.ui.theme.InvestHelpTheme
import dagger.hilt.android.AndroidEntryPoint

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Default.Dashboard, DashboardRoute),
    BottomNavItem("Accounts", Icons.Default.AccountBalance, AccountListRoute),
    BottomNavItem("Items", Icons.Default.ShowChart, ItemListRoute),
    BottomNavItem("Positions", Icons.Default.PieChart, PositionListRoute),
    BottomNavItem("Settings", Icons.Default.Settings, SettingsRoute)
)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InvestHelpTheme {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                val isUnlocked = authState is AuthState.Unlocked

                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.NeedsSetup -> navController.navigate(SetupRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                        is AuthState.Unlocked -> navController.navigate(DashboardRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                        is AuthState.Locked -> navController.navigate(UnlockRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                        else -> {}
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (isUnlocked) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { padding ->
                    InvestHelpNavHost(
                        navController = navController,
                        startDestination = UnlockRoute,
                        activity = this@MainActivity,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selected,
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
