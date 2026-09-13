package com.example.swachhbot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.model.CellState
import com.example.swachhbot.model.CleaningStats
import com.example.swachhbot.model.HouseMap
import com.example.swachhbot.model.FurnitureType
import com.example.swachhbot.model.MoveIntent
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.model.RobotStatus
import com.example.swachhbot.model.TurnIntent
import com.example.swachhbot.viewmodel.SimulatorViewModel
import com.example.swachhbot.vision.DetectedObject
import com.example.swachhbot.vision.ObjectStatus
import kotlin.math.roundToInt

@Composable
fun SimulatorScreen(viewModel: SimulatorViewModel = viewModel()) {
    val robotState by viewModel.robotState.collectAsState()
    val cleaningStats by viewModel.cleaningStats.collectAsState()
    val mapVersion by viewModel.mapVersion.collectAsState()
    val rememberedObjects by viewModel.rememberedObjects.collectAsState()
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsState()
    val houseMap = viewModel.houseMap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF142420))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "SWACHHBOT SIMULATOR V2",
            color = Color(0xFFB8FFCF),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Virtual House / Floor Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF142420), RoundedCornerShape(12.dp))
        ) {
            HouseCanvas(
                robotState = robotState, 
                houseMap = houseMap, 
                occupancyGrid = viewModel.occupancyGrid,
                trigger = mapVersion
            )
            
            // AI Vision Overlay
            if (isCameraEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(160.dp, 120.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    CameraView(onObjectDetected = { viewModel.onObjectsDetected(it) })
                }
            }
            
            // AI Memory Indicator
            if (rememberedObjects.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color(0x88000000), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Text("HOUSE KNOWLEDGE:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    rememberedObjects.sortedByDescending { it.lastDetected }.take(5).forEach {
                        val statusColor = when(it.status) {
                            ObjectStatus.KNOWN -> Color.Cyan
                            ObjectStatus.NEW -> Color.Green
                            else -> Color.Yellow
                        }
                        Text("• ${it.type} (${it.status})", color = statusColor, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry and Controls Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelemetryPanel(
                robotState = robotState, 
                stats = cleaningStats,
                modifier = Modifier.weight(1f)
            )
            ControlPad(
                viewModel = viewModel, 
                robotState = robotState,
                isCameraEnabled = isCameraEnabled
            )
        }
    }
}

@Composable
fun HouseCanvas(
    robotState: RobotState,
    houseMap: HouseMap,
    occupancyGrid: Array<CellState>?,
    trigger: Long
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // use 'trigger' just to force Compose to re-read 'occupancyGrid'
        val dummy = trigger
        
        val mapScale = minOf(size.width / houseMap.width, size.height / houseMap.height)
        val offsetX = (size.width - houseMap.width * mapScale) / 2f
        val offsetY = (size.height - houseMap.height * mapScale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(mapScale, mapScale, pivot = Offset.Zero)
        }) {
            // 1. Draw Map Blueprint lines (faint ground truth)
            houseMap.rooms.forEach { room ->
                drawRect(
                    color = Color(0x33233630), // Faint blueprint
                    topLeft = Offset(room.x, room.y),
                    size = Size(room.width, room.height)
                )
            }
            
            // 2. Draw Occupancy Grid Mapping
            if (occupancyGrid != null) {
                val cellSize = 10f
                val gridW = (houseMap.width / cellSize).toInt()
                
                // Colors for different states
                val cFree = Color(0xFF424242) // Dark Gray for free space
                val cObstacle = Color(0xFFE53935) // Red for obstacle
                val cCleaned = Color(0x88B8FFCF) // Semi-transparent mint for cleaned
                
                for (i in occupancyGrid.indices) {
                    val state = occupancyGrid[i]
                    if (state == CellState.UNKNOWN) continue
                    
                    val x = (i % gridW) * cellSize
                    val y = (i / gridW) * cellSize
                    
                    val cellColor = when (state) {
                        CellState.FREE -> cFree
                        CellState.OBSTACLE -> cObstacle
                        CellState.CLEANED -> cCleaned
                        else -> Color.Transparent
                    }
                    
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(x, y),
                        size = Size(cellSize, cellSize)
                    )
                }
            }

            // 3. Draw Robot
            val robotColor = Color(0xFFB8FFCF)
            val robotRadius = 30f

            drawCircle(
                color = robotColor,
                radius = robotRadius,
                center = Offset(robotState.x, robotState.y)
            )

            // Draw direction indicator
            rotate(degrees = robotState.rotationDegrees, pivot = Offset(robotState.x, robotState.y)) {
                drawLine(
                    color = Color.Red,
                    start = Offset(robotState.x, robotState.y),
                    end = Offset(robotState.x, robotState.y - robotRadius),
                    strokeWidth = 8f
                )
            }
        }
    }
}

fun getFurnitureColor(type: FurnitureType): Color {
    return when (type) {
        FurnitureType.SOFA -> Color(0xFF5D4037)
        FurnitureType.BED -> Color(0xFF3F51B5)
        FurnitureType.TABLE -> Color(0xFF795548)
        FurnitureType.CHAIR -> Color(0xFFFF9800)
        FurnitureType.REFRIGERATOR -> Color(0xFF9E9E9E)
        else -> Color.DarkGray
    }
}

@Composable
fun TelemetryPanel(robotState: RobotState, stats: CleaningStats, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 16.dp)) {
        val estMin = stats.estimatedTimeRemaining / 60
        val estSec = stats.estimatedTimeRemaining % 60
        
        TelemetryRow("Room", stats.currentRoom)
        TelemetryRow("Cleaned", "${stats.percentageCleaned.roundToInt()}% (${(stats.cleanedAreaSq * 0.01f).roundToInt()} m²)")
        TelemetryRow("Elapsed", "${stats.elapsedTimeSeconds}s")
        TelemetryRow("Est. Remaining", "${estMin}m ${estSec}s")
        Spacer(modifier = Modifier.height(4.dp))
        TelemetryRow("Battery", "${robotState.battery.roundToInt()}%")
        TelemetryRow("Status", robotState.status.name)
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ControlPad(viewModel: SimulatorViewModel, robotState: RobotState, isCameraEnabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.toggleCamera() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCameraEnabled) Color(0xFF4CAF50) else Color(0xFF525252)
                )
            ) {
                Text(if (isCameraEnabled) "Vision ON" else "Vision OFF")
            }
        }

        // Map Storage Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { viewModel.saveMap() }) { Text("Save Map") }
            TextButton(onClick = { viewModel.loadMap() }) { Text("Load Map") }
            TextButton(onClick = { viewModel.resetMap() }) { Text("Reset Map", color = Color(0xFFE53935)) }
        }
        
        if (robotState.status == RobotStatus.IDLE) {
            Button(onClick = { viewModel.startCleaning() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Start Auto-Clean")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (robotState.status == RobotStatus.PAUSED) {
                    Button(onClick = { viewModel.startCleaning() }) { Text("Resume") }
                } else {
                    Button(onClick = { viewModel.pauseCleaning() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("Pause") }
                }
                Button(onClick = { viewModel.stop() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) {
                    Text("Stop")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Manual override controls
        HoldButton(
            onStart = { viewModel.setMoveIntent(MoveIntent.FORWARD) },
            onEnd = { viewModel.setMoveIntent(MoveIntent.NONE) }
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Forward")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HoldButton(
                onStart = { viewModel.setTurnIntent(TurnIntent.LEFT) },
                onEnd = { viewModel.setTurnIntent(TurnIntent.NONE) }
            ) {
                Text("<")
            }
            HoldButton(
                onStart = { viewModel.setTurnIntent(TurnIntent.RIGHT) },
                onEnd = { viewModel.setTurnIntent(TurnIntent.NONE) }
            ) {
                Text(">")
            }
        }
        HoldButton(
            onStart = { viewModel.setMoveIntent(MoveIntent.BACKWARD) },
            onEnd = { viewModel.setMoveIntent(MoveIntent.NONE) }
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Backward")
        }
    }
}

/**
 * Custom button composable that executes events upon Press (Down) and Release (Up/Cancel).
 * Perfect for continuous game-like inputs (Hold to move).
 */
@Composable
fun HoldButton(
    onStart: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Trigger state changes based on standard Compose interaction press tracking
    LaunchedEffect(isPressed) {
        if (isPressed) {
            onStart()
        } else {
            onEnd()
        }
    }

    Button(
        onClick = { /* Ignored: Action handled by interaction source effect */ },
        interactionSource = interactionSource,
        modifier = modifier,
        colors = colors,
        content = content
    )
}
