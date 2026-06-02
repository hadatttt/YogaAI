package com.hadat.aiyoga.manager_ai

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.data.download.ModelDownloader
import com.hadat.aiyoga.utils.yogautils.PoseLandmarkerHelper
import org.tensorflow.lite.Interpreter
import java.io.File
import java.util.concurrent.Executors

object AIManager {

    private var classifierInterpreter: Interpreter? = null

    @SuppressLint("StaticFieldLeak")
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null

    private var isInitialized = false
    private var isInitializing = false
    private var aiConfig: AIConfig = AIDeviceProfile.defaultConfig()
    private val pendingCallbacks = mutableListOf<(Boolean) -> Unit>()

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun ensureInitialized(context: Context, onComplete: (Boolean) -> Unit) {
        if (isInitialized) {
            onComplete(true)
            return
        }

        synchronized(this) {
            pendingCallbacks.add(onComplete)
            if (isInitializing) return
            isInitializing = true
        }

        val appContext = context.applicationContext
        val modelsExist = ModelDownloader.YOGA_MODELS.all {
            val file = File(appContext.filesDir, it)
            file.exists() && file.length() > 0
        }

        if (modelsExist) {
            initialize(appContext) {
                notifyInitializationFinished(isInitialized)
            }
        } else {
            ModelDownloader.downloadAllModels(
                appContext,
                onProgress = { progress -> Log.d("AI_Model", "Progress: $progress%") },
                onComplete = { success ->
                    if (success) {
                        initialize(appContext) {
                            notifyInitializationFinished(isInitialized)
                        }
                    } else {
                        notifyInitializationFinished(false)
                    }
                }
            )
        }
    }

    private fun notifyInitializationFinished(success: Boolean) {
        val callbacks = synchronized(this) {
            isInitializing = false
            val callbacks = pendingCallbacks.toList()
            pendingCallbacks.clear()
            callbacks
        }
        mainHandler.post {
            callbacks.forEach { it(success) }
        }
    }

    fun initialize(context: Context, onComplete: () -> Unit) {
        if (isInitialized) {
            onComplete()
            return
        }

        executor.execute {
            try {
                aiConfig = AIDeviceProfile.detect(context)
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
            setNumThreads(aiConfig.classifierThreads)
        }

        Log.d("AIManager", "Classifier using CPU threads=${aiConfig.classifierThreads}")

        classifierInterpreter = Interpreter(file, options)
    }

    private fun initPoseLandmarker(context: Context) {
        val delegate = PoseLandmarkerHelper.DELEGATE_GPU
        Log.d("AIManager", "PoseLandmarker delegate selected: GPU model=FULL")

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

    fun getConfig() = aiConfig

    fun setListener(listener: PoseLandmarkerHelper.LandmarkerListener?) {
        poseLandmarkerHelper?.poseLandmarkerHelperListener = listener
    }
}
