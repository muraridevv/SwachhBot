package com.example.swachhbot.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Top-level screens of the app. */
enum class AppScreen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MAP("Map", Icons.Default.Place),
    CLEAN("Clean", Icons.Default.PlayArrow),
    ROBOT("Robot", Icons.Default.Info),
    LEARNING("Knowledge", Icons.AutoMirrored.Filled.MenuBook),
    MORE("Settings", Icons.Default.Settings),
    ASSISTANT("Assistant", Icons.Default.Face)
}

@Composable
fun SwachhBotApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppScreen.entries.filter { it != AppScreen.ASSISTANT }.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen != AppScreen.ASSISTANT) {
                ExtendedFloatingActionButton(
                    onClick = { currentScreen = AppScreen.ASSISTANT },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Face, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Assistant")
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    onOpenAssistant = { currentScreen = AppScreen.ASSISTANT },
                    onOpenMap = { currentScreen = AppScreen.MAP }
                )
                AppScreen.MAP -> MapScreen()
                AppScreen.CLEAN -> CleaningScreen()
                AppScreen.ROBOT -> RobotDetailScreen()
                AppScreen.LEARNING -> LearningScreen(onBack = { currentScreen = AppScreen.HOME })
                AppScreen.MORE -> SettingsScreen(onBack = { currentScreen = AppScreen.HOME })
                AppScreen.ASSISTANT -> AssistantScreen(onBack = { currentScreen = AppScreen.HOME })
            }
        }
    }
}
