package com.example.swachhbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Draws the latest object detector results on top of the camera preview. */
class DetectionOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SAFE_COLOR
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 38f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private var detections: List<RectF> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1
    private var safetyState = SafetyState.CLEAR

    fun setDetections(
        boxes: List<RectF>,
        sourceWidth: Int,
        sourceHeight: Int,
        newSafetyState: SafetyState
    ) {
        detections = boxes
        imageWidth = sourceWidth.coerceAtLeast(1)
        imageHeight = sourceHeight.coerceAtLeast(1)
        safetyState = newSafetyState
        boxPaint.color = when (safetyState) {
            SafetyState.CLEAR -> SAFE_COLOR
            SafetyState.CAUTION -> CAUTION_COLOR
            SafetyState.STOP -> STOP_COLOR
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = maxOf(width / imageWidth.toFloat(), height / imageHeight.toFloat())
        val offsetX = (width - imageWidth * scale) / 2f
        val offsetY = (height - imageHeight * scale) / 2f
        detections.forEach { sourceBox ->
            val box = RectF(
                sourceBox.left * scale + offsetX,
                sourceBox.top * scale + offsetY,
                sourceBox.right * scale + offsetX,
                sourceBox.bottom * scale + offsetY
            )
            canvas.drawRoundRect(box, 12f, 12f, boxPaint)
            canvas.drawText(safetyState.name, box.left + 8f, (box.top - 12f).coerceAtLeast(40f), labelPaint)
        }
    }

    private companion object {
        val SAFE_COLOR = Color.rgb(112, 255, 174)
        val CAUTION_COLOR = Color.rgb(255, 193, 7)
        val STOP_COLOR = Color.rgb(255, 82, 82)
    }
}
