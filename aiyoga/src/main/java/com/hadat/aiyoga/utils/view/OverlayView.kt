package com.hadat.aiyoga.utils.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var results: PoseLandmarkerResult? = null
    private var smoothedLandmarks: List<NormalizedLandmark>? = null

    private var scaleFactor = 1f
    private var imageWidth = 1
    private var imageHeight = 1

    private var wrongBones: Map<String, Int> = emptyMap()
    private val severityCache = mutableMapOf<String, Float>()

    private val pointPaint = Paint()
    private val linePaint = Paint()

    // Threading
    private var drawingThread: Thread? = null
    private var isRunning = false
    private val holder: SurfaceHolder = getHolder().apply { addCallback(this@OverlayView) }

    init {
        initPaints()
        // Đảm bảo SurfaceView có nền trong suốt để thấy được Camera phía sau
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    // --- Giữ nguyên các hàm Logic của bạn ---

    fun setWrongBones(bones: Map<String, Int>) {
        wrongBones = bones
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode
    ) {
        results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        updateScaleFactor(runningMode)
        updateSmoothedLandmarks(poseLandmarkerResults)
    }

    private fun updateSmoothedLandmarks(result: PoseLandmarkerResult) {
        val newLandmarks = result.landmarks().firstOrNull() ?: return
        // Tăng alpha lên một chút (0.5 - 0.6) nếu thấy nó phản hồi hơi chậm so với tay chân thật
        smoothedLandmarks = smooth(smoothedLandmarks, newLandmarks, 0.6f)
    }

    private fun smooth(
        old: List<NormalizedLandmark>?,
        new: List<NormalizedLandmark>,
        alpha: Float = 0.6f
    ): List<NormalizedLandmark> {
        if (old == null) return new
        return new.mapIndexed { i, lm ->
            val prev = old.getOrNull(i)
            if (prev == null) lm
            else NormalizedLandmark.create(
                prev.x() * alpha + lm.x() * (1 - alpha),
                prev.y() * alpha + lm.y() * (1 - alpha),
                prev.z() * alpha + lm.z() * (1 - alpha)
            )
        }
    }

    private fun updateScaleFactor(runningMode: RunningMode) {
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO ->
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            RunningMode.LIVE_STREAM ->
                max(width * 1f / imageWidth, height * 1f / imageHeight)
        }
    }

    private fun initPaints() {
        pointPaint.apply {
            color = Color.WHITE
            strokeWidth = 8f
            alpha = 180
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        linePaint.apply {
            color = Color.WHITE
            strokeWidth = 6f
            alpha = 120
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    // --- Quản lý luồng vẽ (SurfaceView Lifecycle) ---

    override fun surfaceCreated(p0: SurfaceHolder) {
        isRunning = true
        drawingThread = Thread(this)
        drawingThread?.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        isRunning = false
        try {
            drawingThread?.join()
        } catch (e: InterruptedException) {
            Log.e("OverlayView", "Thread error: ${e.message}")
        }
    }

    override fun run() {
        while (isRunning) {
            if (!holder.surface.isValid) continue

            val canvas = holder.lockCanvas() ?: continue

            // Xóa Canvas cũ (giữ độ trong suốt)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            // Vẽ dữ liệu
            val result = results
            val lm = smoothedLandmarks

            if (result != null && lm != null) {
                drawPoints(canvas, lm)
                drawSkeleton(canvas, lm)
            }

            holder.unlockCanvasAndPost(canvas)

            // Giới hạn khoảng 60 FPS để không làm nóng máy quá mức
            Thread.sleep(16)
        }
    }

    private fun drawPoints(canvas: Canvas, lm: List<NormalizedLandmark>) {
        for (p in lm) {
            canvas.drawPoint(
                p.x() * imageWidth * scaleFactor,
                p.y() * imageHeight * scaleFactor,
                pointPaint
            )
        }
    }

    private fun drawSkeleton(canvas: Canvas, lm: List<NormalizedLandmark>) {
        for (connection in PoseLandmarker.POSE_LANDMARKS) {
            val startIdx = connection!!.start()
            val endIdx = connection.end()

            val key1 = "$startIdx-$endIdx"
            val key2 = "$endIdx-$startIdx"

            val rawSeverity = wrongBones[key1] ?: wrongBones[key2] ?: 0

            val old = severityCache[key1] ?: rawSeverity.toFloat()
            val smoothValue = old + 0.35f * (rawSeverity - old)
            severityCache[key1] = smoothValue

            val severity = smoothValue.toInt()

            linePaint.apply {
                color = when (severity) {
                    1 -> Color.YELLOW
                    2 -> Color.rgb(255, 165, 0)
                    3 -> Color.RED
                    else -> Color.WHITE
                }
                strokeWidth = when (severity) {
                    1 -> 8f
                    2 -> 12f
                    3 -> 16f
                    else -> 6f
                }
                alpha = if (severity == 0) 120 else 255
            }

            val start = lm[startIdx]
            val end = lm[endIdx]

            canvas.drawLine(
                start.x() * imageWidth * scaleFactor,
                start.y() * imageHeight * scaleFactor,
                end.x() * imageWidth * scaleFactor,
                end.y() * imageHeight * scaleFactor,
                linePaint
            )
        }
    }
}