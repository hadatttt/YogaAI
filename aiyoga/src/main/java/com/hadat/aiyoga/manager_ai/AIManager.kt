package com.hadat.aiyoga.manager_ai

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.utils.yogautils.PoseLandmarkerHelper
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.util.concurrent.Executors

object AIManager {
    private var classifierInterpreter: Interpreter? = null
    @SuppressLint("StaticFieldLeak")
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var isInitialized = false

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context, onComplete: () -> Unit) {
        if (isInitialized) {
            onComplete()
            return
        }

        executor.execute {
            try {
                val classifierFile = File(context.filesDir, "yoga_model.tflite")
                if (classifierFile.exists()) {
                    val options = Interpreter.Options().apply {
                        addDelegate(GpuDelegate())
                        setNumThreads(4)
                    }
                    classifierInterpreter = Interpreter(classifierFile, options)
                }
                initPoseLandmarker(context)
                isInitialized = true
                mainHandler.post { onComplete() }

            } catch (e: Exception) {
                Log.e("AIManager", "Init Error: ${e.message}")
                mainHandler.post { onComplete() }
            }
        }
    }

    private fun initPoseLandmarker(context: Context) {
        try {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = context.applicationContext,
                runningMode = RunningMode.LIVE_STREAM,
                currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                currentDelegate = PoseLandmarkerHelper.DELEGATE_GPU,
                poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                    override fun onError(error: String, errorCode: Int) {}
                    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {}
                }
            )
        } catch (e: Exception) {
            Log.e("AIManager", "MediaPipe Init Error: ${e.message}")
        }
    }

    fun getLandmarker() = poseLandmarkerHelper

    fun getClassifier() = classifierInterpreter

    fun setListener(listener: PoseLandmarkerHelper.LandmarkerListener?) {
        poseLandmarkerHelper?.poseLandmarkerHelperListener = listener
    }
}