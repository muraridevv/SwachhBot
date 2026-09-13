package com.example.swachhbot.simulation

import com.example.swachhbot.model.MoveIntent
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.model.RobotStatus
import com.example.swachhbot.model.RoomBounds
import com.example.swachhbot.model.TurnIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RobotSimulator {
    var roomBounds = RoomBounds(1000f, 1000f)

    private val _robotState = MutableStateFlow(RobotState())
    val robotState: StateFlow<RobotState> = _robotState.asStateFlow()

    // Control intents set by the user (UI)
    var moveIntent = MoveIntent.NONE
    var turnIntent = TurnIntent.NONE

    // Physics Configuration
    private val maxSpeed = 300f       // Max pixels per second
    private val acceleration = 400f   // Pixels per second squared
    private val friction = 300f       // Deceleration when no move intent
    private val rotationSpeed = 120f  // Degrees per second
    private val robotRadius = 30f     // Used for collision

    fun updateRoomSize(width: Float, height: Float) {
        if (width <= 0 || height <= 0) return
        roomBounds = RoomBounds(width, height)
        
        // Prevent robot from being stuck outside bounds on resize
        _robotState.update { currentState ->
            currentState.copy(
                x = currentState.x.coerceIn(robotRadius, width - robotRadius),
                y = currentState.y.coerceIn(robotRadius, height - robotRadius)
            )
        }
    }

    suspend fun runSimulationLoop() {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val currentTime = System.currentTimeMillis()
            val dt = (currentTime - lastTime) / 1000f // Delta time in seconds
            lastTime = currentTime

            if (dt > 0) {
                // Cap maximum dt to prevent large physics jumps during lag spikes
                updatePhysics(dt.coerceAtMost(0.1f))
            }
            delay(16) // ~60 FPS
        }
    }

    private fun updatePhysics(dt: Float) {
        _robotState.update { currentState ->
            var newRot = currentState.rotationDegrees
            var newVel = currentState.velocity
            var newX = currentState.x
            var newY = currentState.y

            // 1. Process Rotation
            when (turnIntent) {
                TurnIntent.LEFT -> newRot -= rotationSpeed * dt
                TurnIntent.RIGHT -> newRot += rotationSpeed * dt
                TurnIntent.NONE -> {}
            }
            // Keep rotation within 0-359 degrees for clean telemetry
            newRot = (newRot % 360f).let { if (it < 0) it + 360f else it }

            // 2. Process Acceleration and Friction
            when (moveIntent) {
                MoveIntent.FORWARD -> newVel += acceleration * dt
                MoveIntent.BACKWARD -> newVel -= acceleration * dt
                MoveIntent.NONE -> {
                    // Apply friction towards 0
                    if (newVel > 0) {
                        newVel = (newVel - friction * dt).coerceAtLeast(0f)
                    } else if (newVel < 0) {
                        newVel = (newVel + friction * dt).coerceAtMost(0f)
                    }
                }
            }
            // Cap to max speed limits
            newVel = newVel.coerceIn(-maxSpeed, maxSpeed)

            // 3. Update Position based on velocity and heading
            // 0 degrees is UP (negative Y in Android Canvas coordinates).
            val rad = Math.toRadians((newRot - 90).toDouble())
            newX += (newVel * cos(rad) * dt).toFloat()
            newY += (newVel * sin(rad) * dt).toFloat()

            // 4. Handle Boundaries (Collision)
            val clampedX = newX.coerceIn(robotRadius, roomBounds.width - robotRadius)
            val clampedY = newY.coerceIn(robotRadius, roomBounds.height - robotRadius)
            if (newX != clampedX || newY != clampedY) {
                // Realistically, hitting a wall stops you abruptly
                newVel = 0f
                newX = clampedX
                newY = clampedY
            }

            // 5. Derive Status
            val newStatus = when {
                moveIntent == MoveIntent.FORWARD -> RobotStatus.MOVING_FORWARD
                moveIntent == MoveIntent.BACKWARD -> RobotStatus.MOVING_BACKWARD
                turnIntent == TurnIntent.LEFT -> RobotStatus.ROTATING_LEFT
                turnIntent == TurnIntent.RIGHT -> RobotStatus.ROTATING_RIGHT
                abs(newVel) > 1f -> if (newVel > 0) RobotStatus.MOVING_FORWARD else RobotStatus.MOVING_BACKWARD
                else -> RobotStatus.IDLE
            }

            // 6. Apply Continuous Battery Drain
            val drain = if (newStatus == RobotStatus.IDLE) 0.1f * dt else 1.0f * dt
            val newBattery = (currentState.battery - drain).coerceAtLeast(0f)

            // 7. Commit State
            currentState.copy(
                x = newX,
                y = newY,
                rotationDegrees = newRot,
                velocity = newVel,
                battery = newBattery,
                status = if (newBattery == 0f) RobotStatus.STOPPED else newStatus
            )
        }
    }
}
