package com.example.swachhbot

import android.Manifest
import android.content.pm.PackageManager
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
    private lateinit var safetyText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private val safetyMonitor = CleanerSafetyMonitor()
    private val recentLabels = ArrayDeque<String>()

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
        safetyText = findViewById(R.id.safetyText)
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
        // ML Kit returns coordinates in the rotated input image's orientation.
        val isQuarterTurn = imageProxy.imageInfo.rotationDegrees == 90 ||
            imageProxy.imageInfo.rotationDegrees == 270
        val width = if (isQuarterTurn) imageProxy.height else imageProxy.width
        val height = if (isQuarterTurn) imageProxy.width else imageProxy.height

        // Run both lightweight on-device models together: boxes locate obstacles and labels name them.
        val labelsTask = labeler.process(image)
        val objectsTask = objectDetector.process(image)
        com.google.android.gms.tasks.Tasks.whenAllComplete(labelsTask, objectsTask)
            .addOnCompleteListener {
                val labels = if (labelsTask.isSuccessful) labelsTask.result else emptyList()
                val boxes = if (objectsTask.isSuccessful) objectsTask.result.map { it.boundingBox } else emptyList()
                runOnUiThread {
                    val safetyState = safetyMonitor.update(
                        boxes.map { DetectionBox(it.width(), it.height()) },
                        width,
                        height
                    )
                    overlay.setDetections(boxes, width, height, safetyState)
                    safetyText.text = safetyState.message
                    // Image labeling is frame-wide, so never present it as an object name unless
                    // the object detector also found an object in this frame.
                    val bestLabel = if (boxes.isNotEmpty()) {
                        stableLabel(labels.maxByOrNull { it.confidence }?.text)
                    } else {
                        recentLabels.clear()
                        null
                    }
                    statusText.text = when {
                        safetyState == SafetyState.STOP -> "Obstacle detected — stop cleaner"
                        bestLabel != null -> "I see $bestLabel"
                        boxes.isNotEmpty() -> "Checking ${boxes.size} object${if (boxes.size == 1) "" else "s"}…"
                        else -> "Scanning for objects…"
                    }
                }
                imageProxy.close()
            }
    }

    /** Only expose a label after it repeats, reducing flicker from single-frame guesses. */
    private fun stableLabel(label: String?): String? {
        if (label == null) return null
        recentLabels.addLast(label)
        if (recentLabels.size > LABEL_HISTORY_SIZE) recentLabels.removeFirst()
        return label.takeIf { recentLabels.count { item -> item == label } >= LABEL_CONFIRMATION_COUNT }
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
        const val LABEL_HISTORY_SIZE = 4
        const val LABEL_CONFIRMATION_COUNT = 3
    }
}
