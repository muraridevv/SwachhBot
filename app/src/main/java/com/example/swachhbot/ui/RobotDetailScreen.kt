package com.example.swachhbot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.ui.theme.StatusError
import com.example.swachhbot.ui.theme.StatusOnline
import com.example.swachhbot.viewmodel.SimulatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotDetailScreen(
    viewModel: SimulatorViewModel = viewModel()
) {
    val robotState by viewModel.robotState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Robot Details", fontWeight = FontWeight.Bold) }
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
            // General Info
            DetailSection(title = "General Information") {
                DetailItem(label = "Model", value = "SwachhBot V2 Pro")
                DetailItem(label = "Serial", value = "SB-2026-X49")
                DetailItem(label = "Firmware", value = "v1.4.2 (Phase 21)")
            }

            // Status Section
            DetailSection(title = "Real-time Status") {
                DetailItem(label = "Operational State", value = robotState.status.name)
                DetailItem(label = "Velocity", value = "${robotState.velocity.toInt()} mm/s")
                DetailItem(label = "Heading", value = "${robotState.rotationDegrees.toInt()}°")
            }

            // Health Section
            DetailSection(title = "Sensor Health") {
                HealthItem(label = "LiDAR / Distance", healthy = true)
                HealthItem(label = "IMU", healthy = true)
                HealthItem(label = "Wheel Encoders", healthy = true)
                HealthItem(label = "Cliff Sensors", healthy = true)
                HealthItem(label = "AI Camera", healthy = true)
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HealthItem(label: String, healthy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (healthy) "HEALTHY" else "FAULT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (healthy) StatusOnline else StatusError
        )
    }
}
