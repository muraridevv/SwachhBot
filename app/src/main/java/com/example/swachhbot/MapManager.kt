package com.example.swachhbot

import android.content.Context
import android.util.Log
import com.google.ar.core.Pose
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.sqrt

class MapManager(private val context: Context) {
    data class SerializablePose(val tx: Float, val ty: Float, val tz: Float, val qx: Float, val qy: Float, val qz: Float, val qw: Float)
    data class DetectedObject(val label: String, val pose: SerializablePose, val timestamp: Long)
    data class Point2D(val x: Float, val z: Float)

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("SwachhBotMap", Context.MODE_PRIVATE)
    private var _detections = mutableListOf<DetectedObject>()
    val detections: List<DetectedObject> get() = synchronized(this) { _detections.toList() }
    
    private var _path = mutableListOf<Point2D>()
    val path: List<Point2D> get() = synchronized(this) { _path.toList() }

    init {
        loadMap()
    }

    fun addDetection(label: String, pose: Pose) {
        synchronized(this) {
            val serializablePose = SerializablePose(
                pose.tx(), pose.ty(), pose.tz(),
                pose.qx(), pose.qy(), pose.qz(), pose.qw()
            )
            
            // Avoid duplicates nearby (within 0.5 meters)
            val isDuplicate = _detections.any { 
                it.label == label && distance(it.pose, serializablePose) < 0.5f 
            }

            if (!isDuplicate) {
                _detections.add(DetectedObject(label, serializablePose, System.currentTimeMillis()))
                saveMap()
                Log.d("MapManager", "Saved unique detection: $label at $pose")
            }
        }
    }

    fun updatePath(pose: Pose) {
        synchronized(this) {
            val newPoint = Point2D(pose.tx(), pose.tz())
            if (_path.isEmpty()) {
                _path.add(newPoint)
            } else {
                val lastPoint = _path.last()
                val dx = newPoint.x - lastPoint.x
                val dz = newPoint.z - lastPoint.z
                val dist = sqrt(dx * dx + dz * dz)
                
                // Only add point if moved more than 10cm
                if (dist > 0.1f) {
                    _path.add(newPoint)
                    // Save path periodically or let it be session-based.
                    // For now, keep it session-based for performance.
                }
            }
        }
    }

    private fun distance(p1: SerializablePose, p2: SerializablePose): Float {
        val dx = p1.tx - p2.tx
        val dy = p1.ty - p2.ty
        val dz = p1.tz - p2.tz
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun saveMap() {
        val json = gson.toJson(_detections)
        prefs.edit().putString("saved_map", json).apply()
    }

    private fun loadMap() {
        val json = prefs.getString("saved_map", null)
        if (json != null) {
            val type = object : TypeToken<List<DetectedObject>>() {}.type
            val loaded: List<DetectedObject> = gson.fromJson(json, type)
            _detections.clear()
            _detections.addAll(loaded)
            Log.d("MapManager", "Loaded ${_detections.size} objects from storage")
        }
    }

    fun clearMap() {
        synchronized(this) {
            _detections.clear()
            _path.clear()
            prefs.edit().remove("saved_map").apply()
        }
    }
}
