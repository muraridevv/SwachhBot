package com.example.swachhbot.model

enum class RobotStatus {
    IDLE, CLEANING, PAUSED, RETURNING, CHARGING, ERROR
}

// User or Planner intents for continuous movement control
enum class MoveIntent { FORWARD, BACKWARD, NONE }
enum class TurnIntent { LEFT, RIGHT, NONE }

data class RobotState(
    val x: Float = 150f, // Starting position in Demo Map
    val y: Float = 150f,
    val rotationDegrees: Float = 90f, // Start facing East (90 degrees) for Zig-Zag
    val velocity: Float = 0f,
    val battery: Float = 100f,
    val status: RobotStatus = RobotStatus.IDLE,
    val isColliding: Boolean = false
)

data class RoomBounds(
    val width: Float,
    val height: Float
)

data class CleaningStats(
    val cleanedAreaSq: Int = 0,
    val totalCleanableAreaSq: Int = 1, // avoid div by 0
    val elapsedTimeSeconds: Long = 0L,
    val estimatedTimeRemaining: Long = 0L,
    val currentRoom: String = "Unknown"
) {
    val percentageCleaned: Float 
        get() = if (totalCleanableAreaSq > 0) (cleanedAreaSq.toFloat() / totalCleanableAreaSq) * 100f else 0f
}

data class CleaningSession(
    val timestamp: Long,
    val durationSeconds: Long,
    val cleanedPercentage: Float,
    val roomId: String? = null
)
