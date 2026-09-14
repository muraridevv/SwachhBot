package com.example.swachhbot.vision

import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageProxy

enum class ObjectCategory {
    PERMANENT, // Furniture like sofa, bed, table
    TEMPORARY, // Items like bottle, backpack, shoes
    MOVING     // People, pets
}

enum class ObjectStatus { NEW, KNOWN, TEMPORARY, MOVING, REMOVED }

data class DetectedObject(
    val id: String,
    val type: String,
    val confidence: Float,
    val x: Float = 0f, // Map X
    val y: Float = 0f, // Map Y
    val boundingBox: Rect? = null,
    val firstDetected: Long = System.currentTimeMillis(),
    val lastDetected: Long = System.currentTimeMillis(),
    val detectionCount: Int = 1,
    val status: ObjectStatus = ObjectStatus.NEW,
    val category: ObjectCategory
)

/**
 * Clean abstraction for object detection.
 * Allows replacing ML Kit with a custom TFLite model or cloud API later.
 */
interface ObjectDetectionEngine {
    fun processImage(
        imageProxy: ImageProxy, 
        onDetected: (List<DetectedObject>) -> Unit, 
        onError: (Exception) -> Unit
    )
    fun close()
}
