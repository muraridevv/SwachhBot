package com.example.swachhbot.simulation

import com.example.swachhbot.model.HouseMap
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.model.SensorReading
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clean abstraction for robot sensors. A real robot would implement this
 * by polling actual LiDAR or ultrasonic hardware over serial/network.
 */
interface RobotSensors {
    fun getReadings(robotState: RobotState): List<SensorReading>
}

/**
 * Simulates a 360-degree LiDAR scanning the virtual HouseMap.
 */
class SimulatedSensors(private val houseMap: HouseMap) : RobotSensors {
    private val numRays = 72 // Ray every 5 degrees
    private val maxRange = 400f // 4 meters max range

    override fun getReadings(robotState: RobotState): List<SensorReading> {
        val readings = mutableListOf<SensorReading>()
        
        for (i in 0 until numRays) {
            val angle = (robotState.rotationDegrees + (i * 360f / numRays)) % 360f
            val rad = Math.toRadians((angle - 90).toDouble()) // -90 to align 0 to screen UP
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()
            
            val hitDist = castRay(robotState.x, robotState.y, dx, dy)
            if (hitDist != null && hitDist <= maxRange) {
                readings.add(SensorReading(angle, hitDist, true))
            } else {
                readings.add(SensorReading(angle, maxRange, false))
            }
        }
        return readings
    }

    private fun castRay(px: Float, py: Float, dx: Float, dy: Float): Float? {
        var closestDist: Float? = null

        // Check Walls
        for (wall in houseMap.walls) {
            val dist = raySegmentIntersect(px, py, dx, dy, wall.startX, wall.startY, wall.endX, wall.endY)
            if (dist != null && (closestDist == null || dist < closestDist)) {
                closestDist = dist
            }
        }

        // Check Furniture (Treating bounding box as 4 wall segments)
        for (room in houseMap.rooms) {
            for (furn in room.furniture) {
                // Calculate corners of rotated furniture
                val rad = Math.toRadians(furn.rotationDegrees.toDouble())
                val c = cos(rad).toFloat()
                val s = sin(rad).toFloat()
                val hw = furn.width / 2f
                val hh = furn.height / 2f
                
                val cornersX = floatArrayOf(
                    furn.x + hw * c - hh * s, furn.x - hw * c - hh * s,
                    furn.x - hw * c + hh * s, furn.x + hw * c + hh * s
                )
                val cornersY = floatArrayOf(
                    furn.y + hw * s + hh * c, furn.y - hw * s + hh * c,
                    furn.y - hw * s - hh * c, furn.y + hw * s - hh * c
                )

                for (i in 0 until 4) {
                    val next = (i + 1) % 4
                    val dist = raySegmentIntersect(px, py, dx, dy, cornersX[i], cornersY[i], cornersX[next], cornersY[next])
                    if (dist != null && (closestDist == null || dist < closestDist)) {
                        closestDist = dist
                    }
                }
            }
        }
        return closestDist
    }

    // Mathematical Ray-LineSegment intersection
    private fun raySegmentIntersect(px: Float, py: Float, dx: Float, dy: Float, ax: Float, ay: Float, bx: Float, by: Float): Float? {
        val v1x = ax - px
        val v1y = ay - py
        val v2x = bx - ax
        val v2y = by - ay
        val v3x = -dy
        val v3y = dx
        val dot = v2x * v3x + v2y * v3y
        if (abs(dot) < 0.00001f) return null
        val t1 = (v2x * v1y - v2y * v1x) / dot
        val t2 = (v1x * v3x + v1y * v3y) / dot
        if (t1 >= 0.0f && t2 >= 0.0f && t2 <= 1.0f) return t1
        return null
    }
}