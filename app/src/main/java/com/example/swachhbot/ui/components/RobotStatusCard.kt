package com.example.swachhbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swachhbot.model.RobotStatus
import com.example.swachhbot.ui.theme.*

@Composable
fun RobotStatusCard(
    robotName: String,
    status: RobotStatus,
    batteryLevel: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = robotName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(status = status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${batteryLevel.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = getBatteryColor(batteryLevel)
                )
                Text(
                    text = "Battery",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusDot(status: RobotStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        RobotStatus.IDLE -> StatusOnline
        RobotStatus.CLEANING -> StatusCleaning
        RobotStatus.PAUSED -> StatusPaused
        RobotStatus.RETURNING -> StatusReturning
        RobotStatus.CHARGING -> StatusOnline
        RobotStatus.ERROR -> StatusError
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

fun getBatteryColor(level: Float): Color {
    return when {
        level > 60 -> StatusOnline
        level > 20 -> StatusPaused
        else -> StatusError
    }
}
