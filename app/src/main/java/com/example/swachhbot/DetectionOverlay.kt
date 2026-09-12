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
        color = Color.rgb(112, 255, 174)
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

    fun setDetections(boxes: List<RectF>, sourceWidth: Int, sourceHeight: Int) {
        detections = boxes
        imageWidth = sourceWidth.coerceAtLeast(1)
        imageHeight = sourceHeight.coerceAtLeast(1)
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
            canvas.drawText("OBJECT", box.left + 8f, (box.top - 12f).coerceAtLeast(40f), labelPaint)
        }
    }
}
