package com.example.swachhbot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
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
    Canvas(modifier = modifier.fillMaxSize()) {
        // use 'trigger' just to force Compose to re-read 'occupancyGrid'
        @Suppress("UNUSED_VARIABLE")
        val dummy = trigger
        
        val mapScale = minOf(size.width / houseMap.width, size.height / houseMap.height)
        val offsetX = (size.width - houseMap.width * mapScale) / 2f
        val offsetY = (size.height - houseMap.height * mapScale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(mapScale, mapScale, pivot = Offset.Zero)
        }) {
            // 1. Draw Map Blueprint lines
            houseMap.rooms.forEach { room ->
                drawRect(
                    color = Color(0x33233630),
                    topLeft = Offset(room.x, room.y),
                    size = Size(room.width, room.height)
                )
            }
            
            // 2. Draw Occupancy Grid Mapping
            if (occupancyGrid != null) {
                val cellSize = 10f
                val gridW = (houseMap.width / cellSize).toInt()
                
                val cFree = Color(0xFF424242)
                val cObstacle = Color(0xFFE53935)
                val cCleaned = RobotMint.copy(alpha = 0.5f)
                
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
            drawCircle(
                color = RobotMint,
                radius = robotRadius,
                center = Offset(robotState.x, robotState.y)
            )

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
