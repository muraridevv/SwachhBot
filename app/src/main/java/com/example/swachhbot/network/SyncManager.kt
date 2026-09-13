package com.example.swachhbot.network

import com.example.swachhbot.db.entity.CleaningSessionEntity
import com.example.swachhbot.db.entity.ObjectEntity
import com.example.swachhbot.model.CellState
import com.example.swachhbot.repository.HouseRepository
import com.example.swachhbot.vision.DetectedObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Bridges the Android local Room database with the backend.
 *
 * Keeps the local database as the source of truth while the robot is offline,
 * then reconciles with the server when connectivity is restored.
 */
class SyncManager(
    private val repository: HouseRepository,
    private val backend: BackendClient,
    private val houseId: String,
    private val robotId: String
) {

    /** Push the local occupancy grid up to the backend. */
    suspend fun pushMap(grid: Array<CellState>, gridWidth: Int, gridHeight: Int, cellSize: Double) =
        withContext(Dispatchers.IO) {
            backend.api.saveMap(
                houseId,
                MapUpdateRequest(
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    cellSize = cellSize,
                    mapData = serializeGrid(grid)
                )
            )
        }

    /** Push all locally remembered objects to the backend (idempotent upsert). */
    suspend fun pushObjects() = withContext(Dispatchers.IO) {
        repository.getObjects(houseId).first().forEach { entity ->
            backend.api.upsertObject(houseId, entity.toDto(houseId))
        }
    }

    /** Push a just-finished cleaning session. */
    suspend fun pushSession(session: CleaningSessionEntity) = withContext(Dispatchers.IO) {
        backend.api.createSession(
            houseId,
            SessionDto(
                startedAt = Instant.ofEpochMilli(session.timestamp).toString(),
                endedAt = Instant.ofEpochMilli(session.timestamp + session.durationSeconds * 1000).toString(),
                durationSeconds = session.durationSeconds,
                cleanedPercentage = session.cleanedPercentage.toDouble(),
                areaCleanedSqm = 0.0
            )
        )
    }

    /** Report a spot the robot keeps struggling with. */
    suspend fun reportProblem(x: Double, y: Double, radius: Double, description: String) =
        withContext(Dispatchers.IO) {
            backend.api.reportProblem(
                houseId,
                ProblemAreaDto(x = x, y = y, radius = radius, description = description)
            )
        }

    /** Pull server-side knowledge back into the local cache. */
    suspend fun pullMap(): MapDto? = withContext(Dispatchers.IO) {
        runCatching { backend.api.getMap(houseId) }.getOrNull()
    }

    suspend fun pullObjects(): List<ObjectDto> = withContext(Dispatchers.IO) {
        runCatching { backend.api.getObjects(houseId) }.getOrDefault(emptyList())
    }

    // ----- Mapping helpers -----

    private fun serializeGrid(grid: Array<CellState>): String {
        val sb = StringBuilder(grid.size)
        for (cell in grid) {
            sb.append(
                when (cell) {
                    CellState.UNKNOWN -> 'U'
                    CellState.FREE -> 'F'
                    CellState.OBSTACLE -> 'O'
                    CellState.CLEANED -> 'C'
                    CellState.OCCUPIED -> 'O'
                }
            )
        }
        return sb.toString()
    }
}

// ----- Local <-> DTO mappers -----

fun ObjectEntity.toDto(houseId: String) = ObjectDto(
    id = id,
    houseId = houseId,
    type = type,
    category = category,
    status = status,
    x = x.toDouble(),
    y = y.toDouble(),
    confidence = confidence.toDouble(),
    firstDetected = Instant.ofEpochMilli(firstDetected).toString(),
    lastDetected = Instant.ofEpochMilli(lastDetected).toString(),
    detectionCount = detectionCount
)

fun DetectedObject.toDto(houseId: String) = ObjectDto(
    id = id,
    houseId = houseId,
    type = type,
    category = category.name,
    status = status.name,
    x = x.toDouble(),
    y = y.toDouble(),
    confidence = confidence.toDouble(),
    firstDetected = Instant.ofEpochMilli(firstDetected).toString(),
    lastDetected = Instant.ofEpochMilli(lastDetected).toString(),
    detectionCount = detectionCount
)
