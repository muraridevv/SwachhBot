package com.example.swachhbot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.swachhbot.db.SwachhDatabase
import com.example.swachhbot.db.entity.*
import com.example.swachhbot.model.*
import com.example.swachhbot.network.RobotStateDto
import com.example.swachhbot.repository.HouseRepository
import com.example.swachhbot.repository.impl.RoomHouseRepository
import com.example.swachhbot.simulation.*
import com.example.swachhbot.network.*
import com.example.swachhbot.vision.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import kotlin.time.Duration.Companion.seconds

class SimulatorViewModel(application: Application) : AndroidViewModel(application) {
    private val client = BackendClient(BackendConfig.BASE_URL)
    private val gson = Gson()
    private val repository: HouseRepository = RoomHouseRepository(SwachhDatabase.getDatabase(application))
    private val simulator = RobotSimulator(application)
    private val memoryManager = MemoryManager()
    
    val houseMap: HouseMap = DemoMapFactory.createDemoHouse()
    private val houseId = HouseIdentity.get(application)
    private val robotId = "swachhbot-01"

    val robotState: StateFlow<RobotState> = simulator.robotState
    val cleaningStats: StateFlow<CleaningStats> = simulator.cleaningStats
    val rememberedObjects: StateFlow<List<DetectedObject>> = memoryManager.rememberedObjects
    val mapVersion: StateFlow<Long> = simulator.mapVersion
    val occupancyGrid: Array<CellState>? get() = simulator.occupancyMapper.grid

    var isCameraEnabled = MutableStateFlow(false)

    init {
        simulator.initMap(houseMap)
        loadHouseKnowledge()
        
        viewModelScope.launch {
            simulator.runSimulationLoop()
        }
        
        // Auto-save map and knowledge periodically
        viewModelScope.launch {
            while(true) {
                delay(30.seconds)
                saveHouseKnowledge()
            }
        }

        // Command Polling Loop (Phase 12+)
        viewModelScope.launch {
            while (true) {
                delay(1.seconds)
                pollCommands()
            }
        }

        // Telemetry Pushing Loop (Phase 12+)
        viewModelScope.launch {
            robotState.collect { state ->
                pushTelemetry(state)
            }
        }
    }

    private suspend fun pollCommands() {
        val commands = runCatching { client.api.getPendingCommands(robotId) }.getOrNull()
        commands?.filter { it.status == "PENDING" }?.forEach { cmd ->
            handleCommand(cmd)
            client.api.acknowledgeCommand(cmd.id, CommandAckRequest("ACKNOWLEDGED"))
        }
    }

    private fun handleCommand(cmd: CommandDto) {
        when (cmd.command) {
            "START_CLEANING" -> startCleaning()
            "STOP" -> stop()
            "PAUSE" -> pauseCleaning()
            "MOVE" -> {
                val payload = gson.fromJson(cmd.payload, MovePayload::class.java)
                simulator.setMotionCommand(payload.linear, payload.angular, payload.duration)
            }
            "START_EXPLORATION" -> {
                // For simulation, we assume exploration is driven by the backend ExplorationService.
                // We just acknowledge the command.
            }
        }
    }

    private data class MovePayload(val linear: Double, val angular: Double, val duration: Long)

    private suspend fun pushTelemetry(state: RobotState) {
        runCatching {
            client.api.pushRobotState(robotId, RobotStateDto(
                robotId = robotId,
                houseId = houseId,
                x = state.x.toDouble(),
                y = state.y.toDouble(),
                rotation = state.rotationDegrees.toDouble(),
                velocity = state.velocity.toDouble(),
                battery = state.battery.toDouble(),
                status = state.status.name,
                isColliding = state.isColliding
            ))
        }
    }

    private fun loadHouseKnowledge() {
        viewModelScope.launch {
            val dbHouse = repository.getHouse(houseId)
            if (dbHouse == null) {
                repository.saveHouse(HouseEntity(houseId, "Sample House", houseMap.width, houseMap.height))
            }

            // Sync house to backend (Phase 12+)
            runCatching {
                client.api.getHouse(houseId)
            }.onFailure {
                // If house doesn't exist on backend, create it.
                runCatching {
                    client.api.createHouse(HouseRequest(houseId, "Sample House", houseMap.width.toDouble(), houseMap.height.toDouble()))
                    
                    // Add rooms specifically to the backend
                    houseMap.rooms.forEach { room ->
                        client.api.addRoom(houseId, RoomRequest(
                            id = room.id,
                            name = room.name,
                            x = room.x.toDouble(),
                            y = room.y.toDouble(),
                            width = room.width.toDouble(),
                            height = room.height.toDouble()
                        ))
                    }
                }
            }

            // Load map
            val latestGrid = repository.getLatestGrid(houseId)
            if (latestGrid != null) {
                simulator.occupancyMapper.loadFromSerializedData(latestGrid.gridData)
            }

            // Load memory
            repository.getObjects(houseId).first().let { entities ->
                val objects = entities.map { it.toDomainModel() }
                memoryManager.loadFromHistory(objects)
            }
        }
    }

    fun saveHouseKnowledge() {
        viewModelScope.launch {
            // Save Occupancy Grid
            val gridData = simulator.occupancyMapper.getSerializedData()
            repository.saveOccupancyGrid(OccupancyGridEntity(houseId = houseId, gridData = gridData, timestamp = System.currentTimeMillis()))

            // Save Objects locally
            memoryManager.rememberedObjects.value.forEach { obj ->
                repository.saveObject(obj.toEntity(houseId))
            }

            // Sync to backend if available (Phase 18 vision tracking)
            runCatching {
                val sync = SyncManager(repository, client, houseId, robotId)
                sync.pushObjects(houseMap.rooms)
            }
        }
    }

    fun onObjectsDetected(detections: List<DetectedObject>) {
        val state = robotState.value
        memoryManager.updateMemory(detections, state.x, state.y)
    }

    fun toggleCamera() {
        isCameraEnabled.value = !isCameraEnabled.value
    }

    fun setMoveIntent(intent: MoveIntent) = simulator.apply { moveIntent = intent }
    fun setTurnIntent(intent: TurnIntent) = simulator.apply { turnIntent = intent }
    
    fun startCleaning() {
        simulator.startCleaning() // Ensure UI shows it's cleaning
        viewModelScope.launch {
            runCatching {
                client.api.startExploration()
            }
        }
    }
    
    fun pauseCleaning() = simulator.pauseCleaning()
    
    fun stop() {
        val session = simulator.stopCleaning()
        if (session != null) {
            viewModelScope.launch {
                repository.saveSession(CleaningSessionEntity(
                    houseId = houseId,
                    timestamp = session.timestamp,
                    durationSeconds = session.durationSeconds,
                    cleanedPercentage = session.cleanedPercentage
                ))
                // Also push to backend if available (Phase 19 intelligence)
                runCatching {
                    val sync = SyncManager(repository, client, houseId, robotId)
                    sync.pushSession(CleaningSessionEntity(
                        houseId = houseId,
                        timestamp = session.timestamp,
                        durationSeconds = session.durationSeconds,
                        cleanedPercentage = session.cleanedPercentage
                    ), session.roomId)
                }
            }
        }
        viewModelScope.launch { runCatching { client.api.stopExploration() } }
        saveHouseKnowledge()
    }

    fun resetMap() {
        simulator.occupancyMapper.resetMap()
        memoryManager.clearMemory()
        viewModelScope.launch {
            // we could also clear DB here if needed
        }
    }

    // Manual persistence triggers wired to the UI buttons.
    fun saveMap() {
        saveHouseKnowledge()
    }

    fun loadMap() {
        loadHouseKnowledge()
    }
}

// --- Mappings ---

fun ObjectEntity.toDomainModel() = DetectedObject(
    id = id, type = type, confidence = confidence, x = x, y = y,
    firstDetected = firstDetected, lastDetected = lastDetected,
    detectionCount = detectionCount, status = ObjectStatus.valueOf(status),
    category = ObjectCategory.valueOf(category)
)

fun DetectedObject.toEntity(houseId: String) = ObjectEntity(
    id = id, houseId = houseId, type = type, category = category.name,
    x = x, y = y, confidence = confidence,
    firstDetected = firstDetected, lastDetected = lastDetected,
    detectionCount = detectionCount, status = status.name
)
