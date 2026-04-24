package com.hadat.aiyoga.utils
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
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var correctionRays: List<com.hadat.aiyoga.yogamain.CorrectionRay> = emptyList()
    private var rayPaint = Paint()
    init {
        initPaints()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        rayPaint.color = Color.RED
        rayPaint.strokeWidth = 8f
        rayPaint.style = Paint.Style.STROKE
        linePaint.color = Color.WHITE
        linePaint.alpha = 100
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND

        pointPaint.color = Color.WHITE
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.alpha = 150
        pointPaint.style = Paint.Style.FILL
    }
    fun setCorrectionRays(rays: List<com.hadat.aiyoga.yogamain.CorrectionRay>) {
        this.correctionRays = rays
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        results?.let { result ->
            val landmarksList = result.landmarks()
            if (landmarksList.isEmpty()) return

            val lm = landmarksList[0]

            // draw points
            lm.forEach {
                canvas.drawPoint(
                    it.x() * imageWidth * scaleFactor,
                    it.y() * imageHeight * scaleFactor,
                    pointPaint
                )
            }

            // draw skeleton
            PoseLandmarker.POSE_LANDMARKS.forEach {
                canvas.drawLine(
                    lm[it!!.start()].x() * imageWidth * scaleFactor,
                    lm[it.start()].y() * imageHeight * scaleFactor,
                    lm[it.end()].x() * imageWidth * scaleFactor,
                    lm[it.end()].y() * imageHeight * scaleFactor,
                    linePaint
                )
            }
        }

        // draw rays
        correctionRays.forEach { ray ->

            rayPaint.color = when (ray.severity) {
                1 -> Color.YELLOW   // nhẹ
                2 -> Color.rgb(255,165,0)
                3 -> Color.RED
                else -> Color.RED
            }

            rayPaint.strokeWidth = when (ray.severity) {
                1 -> 6f
                2 -> 10f
                3 -> 14f
                else -> 8f
            }

            canvas.drawLine(
                ray.startX * imageWidth * scaleFactor,
                ray.startY * imageHeight * scaleFactor,
                ray.endX * imageWidth * scaleFactor,
                ray.endY * imageHeight * scaleFactor,
                rayPaint
            )
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

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}