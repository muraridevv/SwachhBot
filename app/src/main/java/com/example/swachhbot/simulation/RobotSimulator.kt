package com.example.swachhbot.simulation

import android.content.Context
import com.example.swachhbot.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RobotSimulator(private val context: Context) {
    var houseMap: HouseMap? = null

    private val _robotState = MutableStateFlow(RobotState())
    val robotState: StateFlow<RobotState> = _robotState.asStateFlow()
    
    private val _cleaningStats = MutableStateFlow(CleaningStats())
    val cleaningStats: StateFlow<CleaningStats> = _cleaningStats.asStateFlow()
    
    // Abstract Planner
    private val planner: CleaningPlanner = ZigZagPlanner()
    private var cleaningStartTime = 0L

    // Sensors & Occupancy Grid Mapping
    private lateinit var sensors: RobotSensors
    lateinit var occupancyMapper: OccupancyMapper
        private set

    private val _mapVersion = MutableStateFlow(0L)
    val mapVersion: StateFlow<Long> = _mapVersion.asStateFlow()

    // Control intents set by the user (UI) or planner
    var moveIntent = MoveIntent.NONE
    var turnIntent = TurnIntent.NONE

    // Hardware-independent motion control (Phase 14/16)
    private var targetLinearVelocity = 0f
    private var targetAngularVelocity = 0f
    private var velocityCommandExpiry = 0L

    fun setMotionCommand(linear: Double, angular: Double, durationMs: Long) {
        this.targetLinearVelocity = linear.toFloat()
        this.targetAngularVelocity = angular.toFloat()
        this.velocityCommandExpiry = System.currentTimeMillis() + durationMs
        // Override intents
        this.moveIntent = MoveIntent.NONE
        this.turnIntent = TurnIntent.NONE
    }

    // Physics Configuration
    private val maxSpeed = 300f       // Max pixels per second
    private val acceleration = 400f   // Pixels per second squared
    private val friction = 300f       // Deceleration when no move intent
    private val rotationSpeed = 120f  // Degrees per second
    private val robotRadius = 30f     // Used for collision

    fun initMap(map: HouseMap) {
        this.houseMap = map
        this.sensors = SimulatedSensors(map)
        this.occupancyMapper = OccupancyMapper(context, map.width, map.height)
    }

    fun startCleaning() {
        _robotState.update { it.copy(status = RobotStatus.CLEANING) }
        cleaningStartTime = System.currentTimeMillis()
        planner.reset()
        calculateTotalCleanableArea()
    }

    fun pauseCleaning() {
        _robotState.update { it.copy(status = RobotStatus.PAUSED) }
        moveIntent = MoveIntent.NONE
        turnIntent = TurnIntent.NONE
    }

    fun stopCleaning(): CleaningSession? {
        val finalStatus = _robotState.value.status
        _robotState.update { it.copy(status = RobotStatus.IDLE) }
        moveIntent = MoveIntent.NONE
        turnIntent = TurnIntent.NONE
        
        if (finalStatus == RobotStatus.CLEANING || finalStatus == RobotStatus.PAUSED) {
            val stats = _cleaningStats.value
            val currentRoomId = houseMap?.rooms?.firstOrNull { 
                _robotState.value.x >= it.x && _robotState.value.x <= it.x + it.width && 
                _robotState.value.y >= it.y && _robotState.value.y <= it.y + it.height 
            }?.id

            return CleaningSession(
                timestamp = System.currentTimeMillis(),
                durationSeconds = stats.elapsedTimeSeconds,
                cleanedPercentage = stats.percentageCleaned,
                roomId = currentRoomId
            )
        }
        return null
    }

    private fun calculateTotalCleanableArea() {
        val map = houseMap ?: return
        var count = 0
        for (i in 0 until (map.width / 10f).toInt()) {
            for (j in 0 until (map.height / 10f).toInt()) {
                val cx = i * 10f + 5f
                val cy = j * 10f + 5f
                if (!checkCollision(cx, cy, 5f)) count++
            }
        }
        _cleaningStats.update { it.copy(totalCleanableAreaSq = count.coerceAtLeast(1)) }
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

            // 0. Update Sensors & Occupancy Grid
            if (::sensors.isInitialized && ::occupancyMapper.isInitialized) {
                val readings = sensors.getReadings(currentState)
                occupancyMapper.updateWithReadings(currentState, readings)
                _mapVersion.value = occupancyMapper.version
            }

            // 1. Ask Planner for Intents if Cleaning
            if (currentState.status == RobotStatus.CLEANING) {
                val (mIntent, tIntent) = planner.getNextIntents(currentState) { cx, cy ->
                    checkCollision(cx, cy, robotRadius)
                }
                this.moveIntent = mIntent
                this.turnIntent = tIntent
                
                // Track time and area
                val elapsed = (System.currentTimeMillis() - cleaningStartTime) / 1000L
                val stats = _cleaningStats.value
                val remaining = if (stats.cleanedAreaSq > 0) {
                    val timePerCell = elapsed.toFloat() / stats.cleanedAreaSq
                    val cellsLeft = stats.totalCleanableAreaSq - stats.cleanedAreaSq
                    (cellsLeft * timePerCell).toLong()
                } else 0L
                
                val currentRoom = houseMap?.rooms?.firstOrNull { 
                    newX >= it.x && newX <= it.x + it.width && newY >= it.y && newY <= it.y + it.height 
                }?.name ?: "Unknown"

                if (::occupancyMapper.isInitialized) {
                    val newlyCleaned = occupancyMapper.markCleaned(newX, newY, robotRadius)
                    if (newlyCleaned > 0) {
                        _mapVersion.value = occupancyMapper.version
                        _cleaningStats.update { it.copy(cleanedAreaSq = occupancyMapper.getCleanedAreaSq()) }
                    }
                }

                _cleaningStats.update { 
                    it.copy(
                        elapsedTimeSeconds = elapsed,
                        estimatedTimeRemaining = remaining.coerceAtLeast(0L),
                        currentRoom = currentRoom
                    )
                }
            }

            // 2. Process Rotation
            if (System.currentTimeMillis() < velocityCommandExpiry) {
                newRot += targetAngularVelocity * dt
            } else {
                when (turnIntent) {
                    TurnIntent.LEFT -> newRot -= rotationSpeed * dt
                    TurnIntent.RIGHT -> newRot += rotationSpeed * dt
                    TurnIntent.NONE -> {}
                }
            }
            // Keep rotation within 0-359 degrees for clean telemetry
            newRot = (newRot % 360f).let { if (it < 0) it + 360f else it }

            // 3. Process Acceleration and Friction
            if (System.currentTimeMillis() < velocityCommandExpiry) {
                // Direct velocity control
                newVel = targetLinearVelocity
            } else {
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
            }
            // Cap to max speed limits
            newVel = newVel.coerceIn(-maxSpeed, maxSpeed)

            // 4. Proposed position based on velocity and heading
            // 0 degrees is UP (negative Y in Android Canvas coordinates).
            val rad = Math.toRadians((newRot - 90).toDouble())
            val stepX = (newVel * cos(rad) * dt).toFloat()
            val stepY = (newVel * sin(rad) * dt).toFloat()
            
            val propX = newX + stepX
            val propY = newY + stepY

            // 5. Handle Boundaries & Collisions
            if (!checkCollision(propX, propY, robotRadius)) {
                newX = propX
                newY = propY
            } else {
                // Collision detected! Abrupt stop.
                newVel = 0f
            }

            // 6. Derive Status
            val newStatus = when {
                currentState.status == RobotStatus.CLEANING -> RobotStatus.CLEANING
                currentState.status == RobotStatus.PAUSED -> RobotStatus.PAUSED
                else -> RobotStatus.IDLE
            }

            // 7. Apply Continuous Battery Drain
            val drain = if (newStatus == RobotStatus.IDLE) 0.1f * dt else 1.0f * dt
            val newBattery = (currentState.battery - drain).coerceAtLeast(0f)

            // 8. Commit State
            currentState.copy(
                x = newX,
                y = newY,
                rotationDegrees = newRot,
                velocity = newVel,
                battery = newBattery,
                status = if (newBattery == 0f) RobotStatus.ERROR else newStatus
            )
        }
    }

    private fun checkCollision(cx: Float, cy: Float, radius: Float): Boolean {
        val map = houseMap ?: return false // No map, no collision except maybe bounds?
        
        // Safety bounds backup
        if (cx - radius < 0 || cx + radius > map.width ||
            cy - radius < 0 || cy + radius > map.height) return true

        // Wall collisions
        for (wall in map.walls) {
            if (circleIntersectsLine(cx, cy, radius + wall.thickness / 2f, wall.startX, wall.startY, wall.endX, wall.endY)) {
                return true
            }
        }

        // Furniture collisions
        for (room in map.rooms) {
            for (furn in room.furniture) {
                if (circleIntersectsOBB(cx, cy, radius, furn)) return true
            }
        }
        
        return false
    }

    private fun circleIntersectsLine(cx: Float, cy: Float, r: Float, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val lineLenSq = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
        if (lineLenSq == 0f) {
            val dx = cx - x1
            val dy = cy - y1
            return (dx * dx + dy * dy) < (r * r)
        }
        var t = ((cx - x1) * (x2 - x1) + (cy - y1) * (y2 - y1)) / lineLenSq
        t = t.coerceIn(0f, 1f)
        val closestX = x1 + t * (x2 - x1)
        val closestY = y1 + t * (y2 - y1)
        val dx = cx - closestX
        val dy = cy - closestY
        return (dx * dx + dy * dy) < (r * r)
    }

    private fun circleIntersectsOBB(cx: Float, cy: Float, r: Float, f: Furniture): Boolean {
        val dx = cx - f.x
        val dy = cy - f.y
        val angleRad = -Math.toRadians(f.rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        
        val localCx = dx * cosA - dy * sinA
        val localCy = dx * sinA + dy * cosA

        val halfW = f.width / 2f
        val halfH = f.height / 2f
        val closestX = localCx.coerceIn(-halfW, halfW)
        val closestY = localCy.coerceIn(-halfH, halfH)

        val distX = localCx - closestX
        val distY = localCy - closestY
        return (distX * distX + distY * distY) < (r * r)
    }
}
