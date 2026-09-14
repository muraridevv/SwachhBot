package com.example.swachhbot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("General Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            ListItem(
                headlineContent = { Text("Dark Theme") },
                supportingContent = { Text("Automatically adjust based on system") },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )
            
            ListItem(
                headlineContent = { Text("Haptic Feedback") },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )

            HorizontalDivider()

            Text("Robotics Integration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ListItem(
                headlineContent = { Text("ROS 2 Gateway") },
                supportingContent = { Text("Connected to raspberrypi.local:9090") },
                trailingContent = { StatusDot(online = true) }
            )

            ListItem(
                headlineContent = { Text("AI Assistant") },
                supportingContent = { Text("Spring AI + Ollama (Llama 3.1)") }
            )

            HorizontalDivider()
            
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ListItem(headlineContent = { Text("App Version") }, trailingContent = { Text("2.1.0-pro") })
            ListItem(headlineContent = { Text("Hardware Revision") }, trailingContent = { Text("Phase 21") })
        }
    }
}

@Composable
fun StatusDot(online: Boolean) {
    Surface(
        modifier = Modifier.size(8.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (online) com.example.swachhbot.ui.theme.StatusOnline else com.example.swachhbot.ui.theme.StatusError
    ) {}
}
