package com.hadat.aiyoga.utils.yogautils


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.R
import kotlin.math.max

class OverlayViewImage(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    init {
        initPaints()
    }

    fun clear() {
        results = null
        invalidate()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE
        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        results?.let { poseLandmarkerResult ->
            val landmarks = poseLandmarkerResult.landmarks().firstOrNull() ?: return

            PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                val start = landmarks[connection.start()]
                val end = landmarks[connection.end()]

                canvas.drawLine(
                    start.x() * imageWidth * scaleFactor - offsetX,
                    start.y() * imageHeight * scaleFactor - offsetY,
                    end.x() * imageWidth * scaleFactor - offsetX,
                    end.y() * imageHeight * scaleFactor - offsetY,
                    linePaint
                )
            }

            for (normalizedLandmark in landmarks) {
                canvas.drawPoint(
                    normalizedLandmark.x() * imageWidth * scaleFactor - offsetX,
                    normalizedLandmark.y() * imageHeight * scaleFactor - offsetY,
                    pointPaint
                )
            }
        }
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        scaleFactor = max(scaleX, scaleY)

        offsetX = (imageWidth * scaleFactor - width) / 2f
        offsetY = (imageHeight * scaleFactor - height) / 2f

        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}