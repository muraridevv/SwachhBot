package com.example.swachhbot.network

/**
 * Kotlin mirrors of the Spring Boot DTOs.
 *
 * Timestamps are ISO-8601 strings to match Jackson's default Instant
 * serialization on the backend (write-dates-as-timestamps = false).
 */
data class HouseDto(
    val id: String? = null,
    val name: String,
    val width: Double,
    val height: Double
)

data class MapDto(
    val id: String? = null,
    val houseId: String? = null,
    val gridWidth: Int,
    val gridHeight: Int,
    val cellSize: Double,
    val mapData: String,
    val version: Long = 0
)

data class MapUpdateRequest(
    val gridWidth: Int,
    val gridHeight: Int,
    val cellSize: Double,
    val mapData: String
)

data class ObjectDto(
    val id: String,
    val houseId: String? = null,
    val type: String,
    val category: String,
    val status: String,
    val roomName: String? = null,
    val x: Double,
    val y: Double,
    val confidence: Double,
    val firstDetected: String,
    val lastDetected: String,
    val detectionCount: Int
)

data class RobotStateDto(
    val robotId: String,
    val houseId: String? = null,
    val x: Double,
    val y: Double,
    val rotation: Double,
    val velocity: Double,
    val battery: Double,
    val status: String
)

data class SessionDto(
    val id: String? = null,
    val houseId: String? = null,
    val roomId: String? = null,
    val startedAt: String,
    val endedAt: String? = null,
    val durationSeconds: Long,
    val cleanedPercentage: Double,
    val areaCleanedSqm: Double
)

data class ProblemAreaDto(
    val id: String? = null,
    val houseId: String? = null,
    val x: Double,
    val y: Double,
    val radius: Double,
    val description: String,
    val frequency: Int = 1,
    val lastSeen: String? = null
)

/** Payload pushed by the server over /ws/telemetry. */
data class TelemetryMessage(
    val type: String? = null,
    val robotId: String? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val rotation: Double = 0.0,
    val velocity: Double = 0.0,
    val battery: Double = 0.0,
    val status: String? = null,
    val cleaningPercent: Double? = null,
    val timestamp: String? = null
)
