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

    // Hai biến quan trọng để bù trừ khoảng cách bị mất do Crop
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

            // Vẽ các đường nối (Sử dụng các giá trị đã được scale và offset)
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

            // Vẽ các điểm mốc
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

        // LOGIC CENTER CROP:
        // 1. Tính tỉ lệ scale dựa trên max để phủ kín View (Center Crop)
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        scaleFactor = max(scaleX, scaleY)

        // 2. Tính khoảng cách bị lệch (Offset)
        // Offset = (Kích thước ảnh sau khi scale - Kích thước View thực tế) / 2
        offsetX = (imageWidth * scaleFactor - width) / 2f
        offsetY = (imageHeight * scaleFactor - height) / 2f

        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}