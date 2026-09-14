package com.example.swachhbot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.model.RobotStatus
import com.example.swachhbot.viewmodel.SimulatorViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningScreen(
    viewModel: SimulatorViewModel = viewModel()
) {
    val robotState by viewModel.robotState.collectAsState()
    val cleaningStats by viewModel.cleaningStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cleaning Control", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularProgressIndicator(
                    progress = { cleaningStats.percentageCleaned / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${cleaningStats.percentageCleaned.roundToInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cleaned",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Area", value = "${(cleaningStats.cleanedAreaSq * 0.01f).roundToInt()} m²")
                StatItem(label = "Time", value = "${cleaningStats.elapsedTimeSeconds}s")
            }

            HorizontalDivider()

            Text(
                text = "Currently in ${cleaningStats.currentRoom}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            // Main Controls
            if (robotState.status == RobotStatus.IDLE) {
                Button(
                    onClick = { viewModel.startCleaning() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Cleaning")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (robotState.status == RobotStatus.PAUSED) {
                        Button(
                            onClick = { viewModel.startCleaning() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Resume")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.pauseCleaning() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Pause")
                        }
                    }
                    Button(
                        onClick = { viewModel.stop() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
