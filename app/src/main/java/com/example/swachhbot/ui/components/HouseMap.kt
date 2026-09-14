package com.example.swachhbot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.example.swachhbot.model.CellState
import com.example.swachhbot.model.HouseMap
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.ui.theme.RobotMint

@Composable
fun HouseMapCanvas(
    robotState: RobotState,
    houseMap: HouseMap,
    occupancyGrid: Array<CellState>?,
    trigger: Long,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }

    val robotColor = RobotMint
    val wallColor = Color(0xFF4A4A4A)
    val gridLineColor = Color(0xFF1E2E28)

    Canvas(modifier = modifier
        .fillMaxSize()
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y
        )
        .transformable(state = transformableState)
    ) {
        @Suppress("UNUSED_VARIABLE")
        val dummy = trigger
        
        val mapScale = minOf(size.width / houseMap.width, size.height / houseMap.height) * 0.9f
        val offsetX = (size.width - houseMap.width * mapScale) / 2f
        val offsetY = (size.height - houseMap.height * mapScale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(mapScale, mapScale, pivot = Offset.Zero)
        }) {
            // 0. Draw Background Grid
            val step = 100f
            for (x in 0..(houseMap.width / step).toInt()) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(x * step, 0f),
                    end = Offset(x * step, houseMap.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(houseMap.height / step).toInt()) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y * step),
                    end = Offset(houseMap.width, y * step),
                    strokeWidth = 1f
                )
            }

            // 1. Draw Map Blueprint lines and labels
            houseMap.rooms.forEach { room ->
                drawRect(
                    color = Color(0x11B8FFCF),
                    topLeft = Offset(room.x, room.y),
                    size = Size(room.width, room.height)
                )
                
                // Draw labels using native canvas for scaling control
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(
                        room.name,
                        room.x + room.width / 2f,
                        room.y + room.height / 2f,
                        paint
                    )
                }
            }

            // Draw Furniture
            houseMap.rooms.forEach { room ->
                room.furniture.forEach { furn ->
                    rotate(furn.rotationDegrees, pivot = Offset(furn.x, furn.y)) {
                        drawRect(
                            color = Color(0x44888888),
                            topLeft = Offset(furn.x - furn.width / 2f, furn.y - furn.height / 2f),
                            size = Size(furn.width, furn.height)
                        )
                    }
                }
            }

            // Draw Walls
            houseMap.walls.forEach { wall ->
                drawLine(
                    color = wallColor,
                    start = Offset(wall.startX, wall.startY),
                    end = Offset(wall.endX, wall.endY),
                    strokeWidth = wall.thickness,
                    cap = StrokeCap.Round
                )
            }
            
            // 2. Draw Occupancy Grid Mapping
            if (occupancyGrid != null) {
                val cellSize = 10f
                val gridW = (houseMap.width / cellSize).toInt()
                
                val cFree = Color(0x334CAF50) // Very faint green
                val cObstacle = Color(0xFFFF5252) // Vibrant Red
                val cCleaned = RobotMint.copy(alpha = 0.6f)
                
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
            val robotRadius = 30f
            
            // Robot shadow/glow
            drawCircle(
                color = robotColor.copy(alpha = 0.3f),
                radius = robotRadius + 5f,
                center = Offset(robotState.x, robotState.y)
            )
            
            // Robot body
            drawCircle(
                color = robotColor,
                radius = robotRadius,
                center = Offset(robotState.x, robotState.y)
            )

            // Direction indicator (Robot Head)
            rotate(degrees = robotState.rotationDegrees, pivot = Offset(robotState.x, robotState.y)) {
                drawCircle(
                    color = Color.Black,
                    radius = 8f,
                    center = Offset(robotState.x, robotState.y - robotRadius + 10f)
                )
                
                // Vision cone (Faint)
                drawArc(
                    color = robotColor.copy(alpha = 0.2f),
                    startAngle = 240f,
                    sweepAngle = 60f,
                    useCenter = true,
                    topLeft = Offset(robotState.x - 150f, robotState.y - 150f),
                    size = Size(300f, 300f)
                )
            }
        }
    }
}
