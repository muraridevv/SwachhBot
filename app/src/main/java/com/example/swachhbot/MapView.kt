package com.example.swachhbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.google.ar.core.Pose
import kotlin.math.atan2

class MapView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val robotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        style = Paint.Style.FILL
    }
    private val objectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 2f
    }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44B8FFCF") // Semi-transparent mint
        style = Paint.Style.STROKE
        strokeWidth = 30f // Simulates a 30cm cleaning width
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val robotPath = Path().apply {
        moveTo(0f, -20f)
        lineTo(-15f, 15f)
        lineTo(15f, 15f)
        close()
    }

    private var objects = emptyList<MapManager.DetectedObject>()
    private var robotPose: Pose? = null
    private var cleanedPath = emptyList<MapManager.Point2D>()
    private val drawPath = Path()
    
    // Meters to pixels scale
    private val scale = 100f

    fun updateData(newObjects: List<MapManager.DetectedObject>, currentPose: Pose?, path: List<MapManager.Point2D>) {
        objects = newObjects
        robotPose = currentPose
        cleanedPath = path
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Center the view on the robot or the origin
        val centerX = width / 2f
        val centerY = height / 2f
        
        canvas.translate(centerX, centerY)

        // Draw grid
        drawGrid(canvas)

        // Draw cleaned path
        if (cleanedPath.isNotEmpty()) {
            drawPath.reset()
            cleanedPath.forEachIndexed { index, point ->
                val x = point.x * scale
                val z = point.z * scale
                if (index == 0) drawPath.moveTo(x, z)
                else drawPath.lineTo(x, z)
            }
            canvas.drawPath(drawPath, pathPaint)
        }

        // Draw objects
        objects.forEach { obj ->
            val x = obj.pose.tx * scale
            val z = obj.pose.tz * scale
            canvas.drawCircle(x, z, 10f, objectPaint)
            canvas.drawText(obj.label, x, z + 35f, textPaint)
        }

        // Draw robot
        robotPose?.let { pose ->
            val rx = pose.tx() * scale
            val rz = pose.tz() * scale
            
            canvas.save()
            canvas.translate(rx, rz)
            
            // Simpler rotation for 2D top-down (around Y axis in AR space)
            // ARCore uses quaternions: [x, y, z, w]
            val qy = pose.qy()
            val qw = pose.qw()
            val angle = Math.toDegrees(2.0 * atan2(qy.toDouble(), qw.toDouble())).toFloat()
            canvas.rotate(-angle)

            canvas.drawPath(robotPath, robotPaint)
            canvas.restore()
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val gridSize = 1f * scale // 1 meter grid
        val halfW = width / 2f
        val halfH = height / 2f
        
        for (i in -10..10) {
            val pos = i * gridSize
            canvas.drawLine(pos, -halfH, pos, halfH, gridPaint)
            canvas.drawLine(-halfW, pos, halfW, pos, gridPaint)
        }
    }
}
