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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.model.MoveIntent
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.model.TurnIntent
import com.example.swachhbot.viewmodel.SimulatorViewModel
import kotlin.math.roundToInt

@Composable
fun SimulatorScreen(viewModel: SimulatorViewModel = viewModel()) {
    val robotState by viewModel.robotState.collectAsState()

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
                .background(Color(0xFF233630), RoundedCornerShape(12.dp))
                .onSizeChanged { size ->
                    viewModel.updateRoomSize(size.width.toFloat(), size.height.toFloat())
                }
        ) {
            HouseCanvas(robotState = robotState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry and Controls Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelemetryPanel(robotState = robotState, modifier = Modifier.weight(1f))
            ControlPad(viewModel = viewModel)
        }
    }
}

@Composable
fun HouseCanvas(robotState: RobotState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val robotColor = Color(0xFFB8FFCF)
        val robotRadius = 30f

        // Draw Robot body
        drawCircle(
            color = robotColor,
            radius = robotRadius,
            center = Offset(robotState.x, robotState.y)
        )

        // Draw direction indicator
        rotate(degrees = robotState.rotationDegrees, pivot = Offset(robotState.x, robotState.y)) {
            // Draw a line pointing "up" from the center to indicate front
            drawLine(
                color = Color.Red,
                start = Offset(robotState.x, robotState.y),
                end = Offset(robotState.x, robotState.y - robotRadius),
                strokeWidth = 8f
            )
        }
    }
}

@Composable
fun TelemetryPanel(robotState: RobotState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 16.dp)) {
        TelemetryRow("X / Y", "${robotState.x.roundToInt()} / ${robotState.y.roundToInt()}")
        TelemetryRow("Rotation", "${robotState.rotationDegrees.roundToInt()}°")
        TelemetryRow("Speed", "${robotState.velocity.roundToInt()} px/s")
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
fun ControlPad(viewModel: SimulatorViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            Button(
                onClick = { viewModel.stop() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF525252))
            ) {
                Text("STOP")
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
