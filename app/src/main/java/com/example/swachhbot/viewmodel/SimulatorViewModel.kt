package com.example.swachhbot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.swachhbot.db.SwachhDatabase
import com.example.swachhbot.db.entity.*
import com.example.swachhbot.model.*
import com.example.swachhbot.repository.HouseRepository
import com.example.swachhbot.repository.impl.RoomHouseRepository
import com.example.swachhbot.simulation.*
import com.example.swachhbot.vision.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import kotlin.time.Duration.Companion.seconds

class SimulatorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HouseRepository = RoomHouseRepository(SwachhDatabase.getDatabase(application))
    private val simulator = RobotSimulator(application)
    private val memoryManager = MemoryManager()
    
    val houseMap: HouseMap = DemoMapFactory.createDemoHouse()
    private val houseId = "DEMO_HOUSE_01"

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
    }

    private fun loadHouseKnowledge() {
        viewModelScope.launch {
            val dbHouse = repository.getHouse(houseId)
            if (dbHouse == null) {
                repository.saveHouse(HouseEntity(houseId, "Sample House", houseMap.width, houseMap.height))
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

            // Save Objects
            memoryManager.rememberedObjects.value.forEach { obj ->
                repository.saveObject(obj.toEntity(houseId))
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
    
    fun startCleaning() = simulator.startCleaning()
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
            }
        }
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
