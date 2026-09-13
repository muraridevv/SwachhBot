package com.example.swachhbot.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.swachhbot.vision.DetectedObject
import com.example.swachhbot.vision.MLKitDetectionEngine
import com.example.swachhbot.vision.ObjectDetectionEngine
import java.util.concurrent.Executors

@Composable
fun CameraView(
    onObjectDetected: (List<DetectedObject>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var detectedObjects by remember { mutableStateOf(emptyList<DetectedObject>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                startCamera(ctx, lifecycleOwner, previewView) { objects ->
                    detectedObjects = objects
                    onObjectDetected(objects)
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Draw Bounding Boxes Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // In a real app, you need to map the image coordinates to the screen coordinates.
            // ML Kit returns bounds based on the ImageProxy dimensions. 
            // For this prototype, we'll do a simple scaling assuming a 480x640 analysis resolution.
            val imageWidth = 480f
            val imageHeight = 640f
            val scaleX = canvasWidth / imageWidth
            val scaleY = canvasHeight / imageHeight

            detectedObjects.forEach { obj ->
                val rect = obj.boundingBox ?: return@forEach
                val left = rect.left * scaleX
                val top = rect.top * scaleY
                val right = rect.right * scaleX
                val bottom = rect.bottom * scaleY

                drawRect(
                    color = androidx.compose.ui.graphics.Color.Green,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 5f)
                )

                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = Color.GREEN
                        textSize = 40f
                        style = Paint.Style.FILL
                    }
                    val text = "${obj.type} ${(obj.confidence * 100).toInt()}%"
                    drawText(text, left, top - 10f, paint)
                }
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onResult: (List<DetectedObject>) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val engine: ObjectDetectionEngine = MLKitDetectionEngine()
    val executor = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysis = ImageAnalysis.Builder()
            // Lower resolution for faster processing
            .setTargetResolution(android.util.Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor) { imageProxy ->
                    engine.processImage(
                        imageProxy = imageProxy,
                        onDetected = { objects ->
                            onResult(objects)
                        },
                        onError = { e ->
                            Log.e("CameraView", "Detection error", e)
                        }
                    )
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e("CameraView", "Use case binding failed", exc)
        }
    }, ContextCompat.getMainExecutor(context))
}
