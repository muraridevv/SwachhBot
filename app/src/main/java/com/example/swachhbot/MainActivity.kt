package com.example.swachhbot

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraPreview: PreviewView
    private lateinit var overlay: DetectionOverlay
    private lateinit var statusText: TextView
    private lateinit var cameraExecutor: ExecutorService

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(MIN_CONFIDENCE).build()
    )
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build()
    )
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showCameraPermissionRequired()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraPreview = findViewById(R.id.cameraPreview)
        overlay = findViewById(R.id.detectionOverlay)
        statusText = findViewById(R.id.statusText)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (hasCameraPermission()) startCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        statusText.text = "Looking for objects…"
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = cameraPreview.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ::analyzeImage) }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val width = imageProxy.width
        val height = imageProxy.height

        // Run both lightweight on-device models together: boxes locate obstacles and labels name them.
        val labelsTask = labeler.process(image)
        val objectsTask = objectDetector.process(image)
        com.google.android.gms.tasks.Tasks.whenAllComplete(labelsTask, objectsTask)
            .addOnCompleteListener {
                val labels = if (labelsTask.isSuccessful) labelsTask.result else emptyList()
                val boxes = if (objectsTask.isSuccessful) objectsTask.result.map { RectF(it.boundingBox) } else emptyList()
                runOnUiThread {
                    overlay.setDetections(boxes, width, height)
                    val bestLabel = labels.maxByOrNull { it.confidence }
                    statusText.text = when {
                        bestLabel != null -> "I see ${bestLabel.text} (${(bestLabel.confidence * 100).toInt()}%)"
                        boxes.isNotEmpty() -> "${boxes.size} object${if (boxes.size == 1) "" else "s"} detected"
                        else -> "Scanning for objects…"
                    }
                }
                imageProxy.close()
            }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showCameraPermissionRequired() {
        statusText.text = getString(R.string.camera_permission_required)
    }

    override fun onDestroy() {
        super.onDestroy()
        labeler.close()
        objectDetector.close()
        cameraExecutor.shutdown()
    }

    private companion object {
        const val MIN_CONFIDENCE = 0.55f
    }
}
