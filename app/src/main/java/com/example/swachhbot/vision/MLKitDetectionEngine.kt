package com.example.swachhbot.vision

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.UUID

class MLKitDetectionEngine : ObjectDetectionEngine {

    // Object Detector is used to find where objects are (bounding boxes)
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification() // Gives us generic classes like FOOD, FASHION, PLACE (we'll ignore these and use Image Labeler for specifics)
            .build()
    )

    // Image Labeler is used to figure out exactly what the object is
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)
            .build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun processImage(
        imageProxy: ImageProxy,
        onDetected: (List<DetectedObject>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            objectDetector.process(image)
                .addOnSuccessListener { objects ->
                    if (objects.isEmpty()) {
                        onDetected(emptyList())
                        imageProxy.close()
                        return@addOnSuccessListener
                    }

                    // We found bounding boxes. Let's pass the whole image to the labeler.
                    // Note: A highly optimized approach crops the InputImage using the bounds 
                    // before passing to the labeler, but ML Kit's labeler performs well on the full frame 
                    // and typically identifies the most prominent objects that correspond to the bounding boxes.
                    labeler.process(image)
                        .addOnSuccessListener { labels ->
                            val detectedObjects = mutableListOf<DetectedObject>()
                            
                            // Naive matching: assign the highest confidence label to the largest/most prominent bounding box
                            val bestLabel = labels.maxByOrNull { it.confidence }
                            val bestBox = objects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

                            if (bestBox != null && bestLabel != null) {
                                val category = categorizeObject(bestLabel.text)
                                detectedObjects.add(
                                    DetectedObject(
                                        id = bestBox.trackingId?.toString() ?: UUID.randomUUID().toString(),
                                        type = bestLabel.text,
                                        confidence = bestLabel.confidence,
                                        boundingBox = bestBox.boundingBox,
                                        category = category
                                    )
                                )
                            }
                            
                            onDetected(detectedObjects)
                            imageProxy.close()
                        }
                        .addOnFailureListener {
                            onError(it)
                            imageProxy.close()
                        }
                }
                .addOnFailureListener {
                    onError(it)
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun categorizeObject(label: String): ObjectCategory {
        val l = label.lowercase()
        return when {
            // Permanent Furniture
            l.contains("chair") || l.contains("sofa") || l.contains("couch") || 
            l.contains("table") || l.contains("bed") || l.contains("refrigerator") || 
            l.contains("furniture") || l.contains("desk") -> ObjectCategory.PERMANENT
            
            // Moving Entities
            l.contains("person") || l.contains("human") || l.contains("man") || 
            l.contains("woman") || l.contains("child") || l.contains("pet") || 
            l.contains("dog") || l.contains("cat") -> ObjectCategory.MOVING
            
            // Temporary Items
            l.contains("bottle") || l.contains("backpack") || l.contains("bag") || 
            l.contains("shoe") || l.contains("clothing") || l.contains("cup") -> ObjectCategory.TEMPORARY
            
            else -> ObjectCategory.TEMPORARY // Default unknown objects to temporary
        }
    }

    override fun close() {
        objectDetector.close()
        labeler.close()
    }
}
