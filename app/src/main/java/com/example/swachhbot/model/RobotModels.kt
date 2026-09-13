package com.example.swachhbot.model

enum class RobotStatus { 
    IDLE, MOVING_FORWARD, MOVING_BACKWARD, ROTATING_LEFT, ROTATING_RIGHT, STOPPED 
}

// User intents for continuous movement control
enum class MoveIntent { FORWARD, BACKWARD, NONE }
enum class TurnIntent { LEFT, RIGHT, NONE }

data class RobotState(
    val x: Float = 500f, 
    val y: Float = 500f, 
    val rotationDegrees: Float = 0f, // 0 means facing "Up"
    val velocity: Float = 0f,        // Current speed in pixels/second
    val battery: Float = 100f,
    val status: RobotStatus = RobotStatus.IDLE
)

data class RoomBounds(
    val width: Float, 
    val height: Float
)
