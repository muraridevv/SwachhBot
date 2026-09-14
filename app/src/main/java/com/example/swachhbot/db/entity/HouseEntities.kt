package com.example.swachhbot.db.entity

import androidx.room.*

@Entity(tableName = "houses")
data class HouseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val width: Float,
    val height: Float
)

@Entity(
    tableName = "rooms",
    foreignKeys = [ForeignKey(
        entity = HouseEntity::class,
        parentColumns = ["id"],
        childColumns = ["houseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("houseId")]
)
data class RoomEntity(
    @PrimaryKey val id: String,
    val houseId: String,
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Entity(
    tableName = "detected_objects",
    foreignKeys = [ForeignKey(
        entity = HouseEntity::class,
        parentColumns = ["id"],
        childColumns = ["houseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("houseId")]
)
data class ObjectEntity(
    @PrimaryKey val id: String,
    val houseId: String,
    val type: String,
    val category: String,
    val x: Float,
    val y: Float,
    val confidence: Float,
    val firstDetected: Long,
    val lastDetected: Long,
    val detectionCount: Int,
    val status: String // NEW, KNOWN, TEMPORARY, MOVING, REMOVED
)

@Entity(
    tableName = "occupancy_grids",
    foreignKeys = [ForeignKey(
        entity = HouseEntity::class,
        parentColumns = ["id"],
        childColumns = ["houseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("houseId")]
)
data class OccupancyGridEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val houseId: String,
    val gridData: String,
    val timestamp: Long
)

@Entity(
    tableName = "cleaning_sessions",
    foreignKeys = [ForeignKey(
        entity = HouseEntity::class,
        parentColumns = ["id"],
        childColumns = ["houseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("houseId")]
)
data class CleaningSessionEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val houseId: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val cleanedPercentage: Float
)

@Entity(
    tableName = "problem_areas",
    foreignKeys = [ForeignKey(
        entity = HouseEntity::class,
        parentColumns = ["id"],
        childColumns = ["houseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("houseId")]
)
data class ProblemAreaEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val houseId: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val description: String,
    val frequency: Int
)
