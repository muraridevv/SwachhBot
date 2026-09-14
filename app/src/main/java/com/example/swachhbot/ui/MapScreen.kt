package com.example.swachhbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.ui.components.HouseMapCanvas
import com.example.swachhbot.viewmodel.SimulatorViewModel
import com.example.swachhbot.vision.ObjectStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: SimulatorViewModel = viewModel()
) {
    val robotState by viewModel.robotState.collectAsState()
    val mapVersion by viewModel.mapVersion.collectAsState()
    val rememberedObjects by viewModel.rememberedObjects.collectAsState()
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interactive Map", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.resetMap() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Map")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            HouseMapCanvas(
                robotState = robotState,
                houseMap = viewModel.houseMap,
                occupancyGrid = viewModel.occupancyGrid,
                trigger = mapVersion
            )

            // AI Vision Overlay
            if (isCameraEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(160.dp, 120.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    CameraView(onObjectDetected = { viewModel.onObjectsDetected(it) })
                }
            }

            // Overlays
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                // AI Memory Indicator
                if (rememberedObjects.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("DETECTED OBJECTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            rememberedObjects.sortedByDescending { it.lastDetected }.take(3).forEach {
                                val statusColor = when (it.status) {
                                    ObjectStatus.KNOWN -> Color.Cyan
                                    ObjectStatus.NEW -> Color.Green
                                    else -> Color.Yellow
                                }
                                Text("• ${it.type}", color = statusColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Controls overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isCameraEnabled,
                    onClick = { viewModel.toggleCamera() },
                    label = { Text("AI Vision") },
                    leadingIcon = if (isCameraEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}
