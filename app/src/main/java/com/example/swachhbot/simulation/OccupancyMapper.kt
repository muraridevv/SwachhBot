package com.example.swachhbot.simulation

import android.content.Context
import com.example.swachhbot.model.CellState
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.model.SensorReading
import java.lang.StringBuilder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Handles building and maintaining the 2D Occupancy Grid map independently of UI.
 */
class OccupancyMapper(private val context: Context, private val mapWidth: Float, private val mapHeight: Float) {
    val cellSize = 10f
    val gridWidth = (mapWidth / cellSize).toInt()
    val gridHeight = (mapHeight / cellSize).toInt()
    
    // 1D array representing 2D grid for performance. Maps (x,y) to y * width + x
    var grid = Array(gridWidth * gridHeight) { CellState.UNKNOWN }
        private set
    
    // Increment this to notify UI of changes instead of copying the whole array
    var version = 0L
        private set

    private val prefs = context.getSharedPreferences("SwachhBotOccupancy", Context.MODE_PRIVATE)

    fun updateWithReadings(robotState: RobotState, readings: List<SensorReading>) {
        var changed = false
        val rx = robotState.x
        val ry = robotState.y

        for (reading in readings) {
            val rad = Math.toRadians((reading.angleDegrees - 90).toDouble())
            val hitX = rx + reading.distance * cos(rad).toFloat()
            val hitY = ry + reading.distance * sin(rad).toFloat()
            if (traceRay(rx, ry, hitX, hitY, reading.hit)) {
                changed = true
            }
        }
        if (changed) version++
    }

    fun markCleaned(robotX: Float, robotY: Float, radius: Float): Int {
        val gridX = (robotX / cellSize).toInt()
        val gridY = (robotY / cellSize).toInt()
        val radiusCells = (radius / cellSize).toInt()
        var newlyCleaned = 0

        for (i in gridX - radiusCells..gridX + radiusCells) {
            for (j in gridY - radiusCells..gridY + radiusCells) {
                if (i in 0 until gridWidth && j in 0 until gridHeight) {
                    val cx = i * cellSize + cellSize / 2f
                    val cy = j * cellSize + cellSize / 2f
                    val distSq = (cx - robotX) * (cx - robotX) + (cy - robotY) * (cy - robotY)
                    if (distSq <= radius * radius) {
                        val idx = j * gridWidth + i
                        // We only override FREE or UNKNOWN with CLEANED. 
                        // Obstacles stay obstacles.
                        if (grid[idx] == CellState.FREE || grid[idx] == CellState.UNKNOWN) {
                            grid[idx] = CellState.CLEANED
                            newlyCleaned++
                        }
                    }
                }
            }
        }
        if (newlyCleaned > 0) version++
        return newlyCleaned
    }

    /**
     * Bresenham's Line Algorithm to mark FREE cells along the ray path, 
     * and OBSTACLE at the very end if it was a hit.
     */
    private fun traceRay(startX: Float, startY: Float, endX: Float, endY: Float, hit: Boolean): Boolean {
        var x0 = (startX / cellSize).toInt().coerceIn(0, gridWidth - 1)
        var y0 = (startY / cellSize).toInt().coerceIn(0, gridHeight - 1)
        val x1 = (endX / cellSize).toInt().coerceIn(0, gridWidth - 1)
        val y1 = (endY / cellSize).toInt().coerceIn(0, gridHeight - 1)

        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        var changed = false

        while (true) {
            val idx = y0 * gridWidth + x0
            if (x0 == x1 && y0 == y1) {
                if (hit && grid[idx] != CellState.OBSTACLE) {
                    grid[idx] = CellState.OBSTACLE
                    changed = true
                }
                break
            } else {
                if (grid[idx] == CellState.UNKNOWN) {
                    grid[idx] = CellState.FREE
                    changed = true
                }
            }

            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
        return changed
    }

    // --- Storage ---
    fun getSerializedData(): String {
        val sb = StringBuilder(grid.size)
        for (cell in grid) {
            val char = when (cell) {
                CellState.UNKNOWN -> 'U'
                CellState.FREE -> 'F'
                CellState.OBSTACLE -> 'O'
                CellState.CLEANED -> 'C'
                else -> 'U'
            }
            sb.append(char)
        }
        return sb.toString()
    }

    fun loadFromSerializedData(data: String) {
        if (data.length == grid.size) {
            for (i in data.indices) {
                grid[i] = when (data[i]) {
                    'U' -> CellState.UNKNOWN
                    'F' -> CellState.FREE
                    'O' -> CellState.OBSTACLE
                    'C' -> CellState.CLEANED
                    else -> CellState.UNKNOWN
                }
            }
            version++
        }
    }

    fun saveMap() {
        val data = getSerializedData()
        prefs.edit().putString("map_data", data).apply()
    }

    fun loadMap() {
        val data = prefs.getString("map_data", null)
        if (data != null) {
            loadFromSerializedData(data)
        }
    }

    fun resetMap() {
        for (i in grid.indices) grid[i] = CellState.UNKNOWN
        prefs.edit().remove("map_data").apply()
        version++
    }

    fun getCleanedAreaSq(): Int = grid.count { it == CellState.CLEANED }
}
