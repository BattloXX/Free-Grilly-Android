package org.battlo.freegrilly.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.battlo.freegrilly.R

data class NavItem(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

val navItems = listOf(
    NavItem("dashboard", Icons.Default.Thermostat, R.string.nav_dashboard),
    NavItem("library", Icons.Default.MenuBook, R.string.nav_library),
    NavItem("history", Icons.Default.History, R.string.nav_history),
    NavItem("settings", Icons.Default.Settings, R.string.nav_settings),
)

@Composable
fun AdaptiveScaffold(
    isCompact: Boolean,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    if (isCompact) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) { content() }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                Spacer(Modifier.height(16.dp))
                navItems.forEach { item ->
                    NavigationRailItem(
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
