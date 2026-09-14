package com.example.swachhbot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.model.RobotStatus
import com.example.swachhbot.ui.components.RobotStatusCard
import com.example.swachhbot.viewmodel.SimulatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAssistant: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: SimulatorViewModel = viewModel()
) {
    val robotState by viewModel.robotState.collectAsState()
    val cleaningStats by viewModel.cleaningStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SwachhBot", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome back 👋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            RobotStatusCard(
                robotName = "SwachhBot-01",
                status = robotState.status,
                batteryLevel = robotState.battery
            )

            // Current Activity Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Current Activity",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (robotState.status == RobotStatus.CLEANING)
                            "Cleaning ${cleaningStats.currentRoom}" else "Robot is ${robotState.status.name.lowercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (robotState.status == RobotStatus.CLEANING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { cleaningStats.percentageCleaned / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Map")
                }
                OutlinedButton(
                    onClick = onOpenAssistant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ask Assistant")
                }
            }
        }
    }
}
