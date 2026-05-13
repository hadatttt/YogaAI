package com.hadat.aiyoga.manager_ai

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.utils.yogautils.PoseLandmarkerHelper
import org.tensorflow.lite.Interpreter
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
                initClassifier(context)
                initPoseLandmarker(context)
                isInitialized = true
                mainHandler.post { onComplete() }
            } catch (e: Exception) {
                Log.e("AIManager", "Init Error: ${e.message}")
                mainHandler.post { onComplete() }
            }
        }
    }

    private fun initClassifier(context: Context) {
        val file = File(context.filesDir, "yoga_model.tflite")
        if (!file.exists()) return

        val options = Interpreter.Options().apply {
            setNumThreads(4) // 2–4 tuỳ máy, 4 là ổn
        }

        Log.d("AIManager", "Classifier using CPU")

        classifierInterpreter = Interpreter(file, options)
    }

    private fun initPoseLandmarker(context: Context) {
        val delegate = try {
            PoseLandmarkerHelper.DELEGATE_GPU
            Log.d("AIManager", "PoseLandmarker using GPU")
        } catch (e: Exception) {
            PoseLandmarkerHelper.DELEGATE_CPU
            Log.d("AIManager", "PoseLandmarker using CPU")
        }

        poseLandmarkerHelper = PoseLandmarkerHelper(
            context = context.applicationContext,
            runningMode = RunningMode.LIVE_STREAM,
            currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
            currentDelegate = delegate,
            poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {}
                override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {}
            }
        )
    }

    fun getLandmarker() = poseLandmarkerHelper

    fun getClassifier() = classifierInterpreter

    fun setListener(listener: PoseLandmarkerHelper.LandmarkerListener?) {
        poseLandmarkerHelper?.poseLandmarkerHelperListener = listener
    }
}