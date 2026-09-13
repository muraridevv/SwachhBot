package com.example.swachhbot

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.media.Image
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer {
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var overlay: DetectionOverlay
    private lateinit var mapView: MapView
    private lateinit var statusText: TextView
    private lateinit var safetyText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var arSession: Session? = null
    private val safetyMonitor = CleanerSafetyMonitor()
    private val recentLabels = ArrayDeque<String>()
    private lateinit var mapManager: MapManager

    private var displayRotation = Surface.ROTATION_0
    private var backgroundTextureId = -1
    private val backgroundRenderer = BackgroundRenderer()

    @Volatile
    private var isProcessing = false

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
        if (granted) {
            statusText.text = "Permission granted. Starting AR..."
            setupArSession()
        } else {
            showCameraPermissionRequired()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceView = findViewById(R.id.surfaceView)
        overlay = findViewById(R.id.detectionOverlay)
        mapView = findViewById(R.id.mapView)
        statusText = findViewById(R.id.statusText)
        safetyText = findViewById(R.id.safetyText)
        cameraExecutor = Executors.newSingleThreadExecutor()
        mapManager = MapManager(this)

        setupButtons()

        // Set up GLSurfaceView
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        if (hasCameraPermission()) {
            setupArSession()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            setupArSession()
        }
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
        surfaceView.onPause()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnClean).setOnClickListener {
            val isCleaning = it.tag == "cleaning"
            if (isCleaning) {
                it.tag = "idle"
                (it as Button).text = getString(R.string.btn_start_clean)
                statusText.text = "Cleaning paused"
            } else {
                it.tag = "cleaning"
                (it as Button).text = getString(R.string.btn_stop_clean)
                statusText.text = "Cleaning in progress..."
            }
        }

        findViewById<Button>(R.id.btnResetMap).setOnClickListener {
            mapManager.clearMap()
            statusText.text = "Map cleared"
        }
    }

    private fun setupArSession() {
        if (arSession != null) {
            try {
                arSession?.resume()
                surfaceView.onResume()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error resuming AR session", e)
            }
            return
        }

        try {
            when (ArCoreApk.getInstance().checkAvailability(this)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    val session = Session(this)
                    val config = Config(session)
                    // Use LATEST_CAMERA_IMAGE as requested
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.focusMode = Config.FocusMode.AUTO
                    session.configure(config)
                    if (backgroundTextureId != -1) {
                        session.setCameraTextureName(backgroundTextureId)
                    }
                    session.resume()
                    arSession = session
                    surfaceView.onResume()
                    statusText.text = "AR Mapping Active"
                }
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    statusText.text = "ARCore update required"
                }
                else -> {
                    statusText.text = "Device does not support AR"
                }
            }
        } catch (e: UnavailableException) {
            statusText.text = "AR Error: ${e.message}"
        }
    }

    // GLSurfaceView.Renderer implementation
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        backgroundTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, backgroundTextureId)
        arSession?.setCameraTextureName(backgroundTextureId)
        backgroundRenderer.createOnGlThread()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        arSession?.setDisplayGeometry(displayRotation, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val session = arSession ?: return
        try {
            // Call arSession?.update() to get the latest Frame
            val frame = session.update()
            
            // Draw the camera background
            if (backgroundTextureId != -1) {
                backgroundRenderer.draw(frame, backgroundTextureId)
            }
            
            val cameraPose = frame.camera.pose
            if (frame.camera.trackingState == TrackingState.TRACKING) {
                mapManager.updatePath(cameraPose)
            }
            
            // Update MapView with current state
            runOnUiThread {
                mapView.updateData(mapManager.detections, cameraPose, mapManager.path)
            }
            
            // If not currently processing, acquire camera image and analyze
            if (!isProcessing) {
                val cameraImage = try {
                    frame.acquireCameraImage()
                } catch (e: Exception) {
                    null
                }
                
                if (cameraImage != null) {
                    isProcessing = true
                    try {
                        analyzeArImage(cameraImage, frame)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Analysis failed to start", e)
                        cameraImage.close()
                        isProcessing = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "AR Frame update error", e)
        }
    }

    private fun analyzeArImage(image: Image, frame: Frame) {
        // Calculate correct rotation for ML Kit based on display rotation
        val rotationDegrees = when (displayRotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> 0
        }

        val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
        val cameraPose = frame.camera.pose
        
        val labelsTask = labeler.process(inputImage)
        val objectsTask = objectDetector.process(inputImage)

        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val correctedWidth = if (isRotated) image.height else image.width
        val correctedHeight = if (isRotated) image.width else image.height

        Tasks.whenAllComplete(labelsTask, objectsTask)
            .addOnCompleteListener(cameraExecutor) {
                try {
                    val labels = if (labelsTask.isSuccessful) labelsTask.result else emptyList()
                    val objects = if (objectsTask.isSuccessful) objectsTask.result else emptyList()
                    val boxes = objects.map { RectF(it.boundingBox) }

                    runOnUiThread {
                        // Update the UI (safety monitor, overlay)
                        val safetyState = safetyMonitor.update(
                            boxes.map { DetectionBox(it.width().toInt(), it.height().toInt()) },
                            correctedWidth,
                            correctedHeight
                        )
                        overlay.setDetections(boxes, correctedWidth, correctedHeight, safetyState)
                        safetyText.text = safetyState.message
                        
                        // Update safety indicator color
                        val (textColor, bgColor) = when (safetyState) {
                            SafetyState.CLEAR -> Pair(
                                ContextCompat.getColor(this@MainActivity, R.color.robot_mint),
                                ContextCompat.getColor(this@MainActivity, R.color.robot_teal_dark_translucent)
                            )
                            SafetyState.CAUTION -> Pair(
                                ContextCompat.getColor(this@MainActivity, R.color.black),
                                ContextCompat.getColor(this@MainActivity, R.color.robot_yellow)
                            )
                            SafetyState.STOP -> Pair(
                                ContextCompat.getColor(this@MainActivity, R.color.white),
                                ContextCompat.getColor(this@MainActivity, R.color.robot_red)
                            )
                        }
                        safetyText.setTextColor(textColor)
                        safetyText.setBackgroundColor(bgColor)

                        val bestLabel = if (objects.isNotEmpty()) {
                            stableLabel(labels.maxByOrNull { it.confidence }?.text)
                        } else {
                            recentLabels.clear()
                            null
                        }

                        if (bestLabel != null && frame.camera.trackingState == TrackingState.TRACKING) {
                            mapManager.addDetection(bestLabel, cameraPose)
                        }

                        statusText.text = when {
                            safetyState == SafetyState.STOP -> "Obstacle detected — stop cleaner"
                            bestLabel != null -> "I see $bestLabel"
                            boxes.isNotEmpty() -> "Checking ${boxes.size} object${if (boxes.size == 1) "" else "s"}…"
                            else -> "Scanning for objects…"
                        }
                    }
                } finally {
                    image.close()
                    isProcessing = false
                }
            }
    }

    private fun stableLabel(label: String?): String? {
        if (label == null) return null
        recentLabels.addLast(label)
        if (recentLabels.size > LABEL_HISTORY_SIZE) recentLabels.removeFirst()
        return label.takeIf { recentLabels.count { item -> item == label } >= LABEL_CONFIRMATION_COUNT }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showCameraPermissionRequired() {
        statusText.text = "Camera permission is required for AR mapping"
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
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
