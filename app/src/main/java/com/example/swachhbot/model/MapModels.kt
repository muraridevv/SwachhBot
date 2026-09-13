package com.example.swachhbot.model

enum class FurnitureType { SOFA, BED, TABLE, CHAIR, REFRIGERATOR, UNKNOWN }

data class Furniture(
    val id: String,
    val type: FurnitureType,
    val x: Float, // Center X
    val y: Float, // Center Y
    val width: Float,
    val height: Float,
    val rotationDegrees: Float = 0f
)

data class Wall(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val thickness: Float = 10f
)

data class Room(
    val id: String,
    val name: String,
    val x: Float, // Top-left X boundary
    val y: Float, // Top-left Y boundary
    val width: Float,
    val height: Float,
    val furniture: List<Furniture> = emptyList()
)

data class HouseMap(
    val width: Float,
    val height: Float,
    val rooms: List<Room>,
    val walls: List<Wall>
)

// --- Phase 5 Mapping Models ---

enum class CellState { UNKNOWN, FREE, OCCUPIED, CLEANED, OBSTACLE }

data class SensorReading(
    val angleDegrees: Float,
    val distance: Float,
    val hit: Boolean
)
