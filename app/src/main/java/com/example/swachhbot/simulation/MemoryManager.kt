package com.example.swachhbot.simulation

import com.example.swachhbot.vision.DetectedObject
import com.example.swachhbot.vision.ObjectCategory
import com.example.swachhbot.vision.ObjectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Represents the robot's "brain" or memory.
 * Stores objects seen via the camera and tracks their persistence.
 */
class MemoryManager {
    private val _rememberedObjects = MutableStateFlow<List<DetectedObject>>(emptyList())
    val rememberedObjects: StateFlow<List<DetectedObject>> = _rememberedObjects.asStateFlow()

    fun updateMemory(newDetections: List<DetectedObject>, robotX: Float, robotY: Float) {
        _rememberedObjects.update { currentList ->
            val updatedList = currentList.toMutableList()
            
            for (newObj in newDetections) {
                // Map screen coordinates to virtual map coordinates (simplified for prototype)
                val mappedX = robotX + (newObj.boundingBox?.centerX()?.minus(240)?.times(0.5f) ?: 0f)
                val mappedY = robotY + (newObj.boundingBox?.centerY()?.minus(320)?.times(0.5f) ?: 0f)

                val existingIdx = updatedList.indexOfFirst { it.type == newObj.type }
                
                if (existingIdx != -1) {
                    val existing = updatedList[existingIdx]
                    val newCount = existing.detectionCount + 1
                    val newStatus = when {
                        newObj.category == ObjectCategory.MOVING -> ObjectStatus.MOVING
                        newCount > 10 -> ObjectStatus.KNOWN
                        else -> ObjectStatus.NEW
                    }
                    
                    updatedList[existingIdx] = existing.copy(
                        x = mappedX,
                        y = mappedY,
                        confidence = newObj.confidence,
                        lastDetected = System.currentTimeMillis(),
                        detectionCount = newCount,
                        status = newStatus
                    )
                } else {
                    updatedList.add(newObj.copy(
                        x = mappedX, 
                        y = mappedY,
                        status = if (newObj.category == ObjectCategory.MOVING) ObjectStatus.MOVING else ObjectStatus.NEW
                    ))
                }
            }
            
            // simple decay for temporary objects
            updatedList.filter { 
                it.status == ObjectStatus.KNOWN || 
                System.currentTimeMillis() - it.lastDetected < 60000 
            }
        }
    }

    fun loadFromHistory(persisted: List<DetectedObject>) {
        _rememberedObjects.value = persisted
    }

    fun clearMemory() {
        _rememberedObjects.value = emptyList()
    }
}
