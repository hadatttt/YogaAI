package com.hadat.aiyoga.yogautils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint().apply { color = Color.YELLOW; strokeWidth = 10f; style = Paint.Style.FILL }
    private var linePaint = Paint().apply { color = Color.GREEN; strokeWidth = 6f; style = Paint.Style.STROKE }

    fun setResults(poseLandmarkerResult: PoseLandmarkerResult, imageHeight: Int, imageWidth: Int) {
        this.results = poseLandmarkerResult
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                // Vẽ xương nối
                PoseLandmarker.POSE_LANDMARKS.forEach {
                    val start = landmark[it.start()]
                    val end = landmark[it.end()]
                    canvas.drawLine(
                        start.x() * width, start.y() * height,
                        end.x() * width, end.y() * height,
                        linePaint
                    )
                }
                // Vẽ khớp
                for (point in landmark) {
                    canvas.drawCircle(point.x() * width, point.y() * height, 8f, pointPaint)
                }
            }
        }
    }
}