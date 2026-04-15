package com.hadat.aiyoga.yogamain

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.databinding.FragmentSingleYogaBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.hadat.aiyoga.utils.ModelDownloader
import com.hadat.aiyoga.utils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils

class YogaFragment : BaseFragment<FragmentSingleYogaBinding, YogaViewModel>(),
    PoseLandmarkerHelper.LandmarkerListener {

    private val TAG = "YogaAI_Debug"
    private lateinit var backgroundExecutor: ExecutorService
    private val classifierExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private var classifierInterpreter: Interpreter? = null
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var tts: TextToSpeech? = null // Biến TTS
    private val tfliteLock = Any()

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var frameCounter = 0
    private var lastCoachTime = 0L
    private val COACH_INTERVAL = 2000L

    private var progressAnimator: ObjectAnimator? = null
    private var isMuted = false
    private var lastPendingPoseId = -1
    private var poseCounter = 0
    private val STABLE_THRESHOLD = 3

    override fun initView() {
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // 1. Khởi tạo TTS ngay từ đầu
        initTextToSpeech()

        // 2. Kiểm tra quyền và mở Camera
        checkAndStartCamera()

        // 3. Tải model
        ModelDownloader.downloadAllModels(requireContext(),
            onProgress = { progress ->
                activity?.runOnUiThread { binding.tvGuide.text = "Loading: $progress%" }
            },
            onComplete = { success ->
                if (success) {
                    Log.d(TAG, "Download success. Initializing AI...")
                    initializeAiResources()
                }
            }
        )
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Ưu tiên tiếng Việt, nếu không có thì dùng tiếng Anh
                val result = tts?.setLanguage(Locale("vi", "VN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.US
                    Log.e(TAG, "Ngôn ngữ tiếng Việt không được hỗ trợ, chuyển sang tiếng Anh.")
                }
                Log.d(TAG, "TTS Initialized thành công.")
            } else {
                Log.e(TAG, "TTS Initialization thất bại!")
            }
        }
    }

    private fun initializeAiResources() {
        backgroundExecutor.execute {
            try {
                val context = context ?: return@execute
                val classifierFile = File(context.filesDir, "yoga_model.tflite")
                if (classifierFile.exists()) {
                    val options = Interpreter.Options().setNumThreads(4).setUseNNAPI(true)
                    synchronized(tfliteLock) {
                        classifierInterpreter = Interpreter(classifierFile, options)
                    }
                }

                poseLandmarkerHelper = PoseLandmarkerHelper(
                    context = context,
                    runningMode = RunningMode.LIVE_STREAM,
                    currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_HEAVY,
                    poseLandmarkerHelperListener = this
                )

                YogaCoachUtils.loadReferenceData { isSuccess ->
                    Log.d(TAG, "Load Reference Data: $isSuccess")
                    activity?.runOnUiThread { viewModel.fetchYogaPoses() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init AI Error: ${e.message}")
            }
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (view == null) return@runOnUiThread
            val detectedId = viewModel.detectedPoseId.value ?: -1

            if (detectedId != -1) {
                binding.overlayView.visibility = View.VISIBLE
                binding.overlayView.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )
                binding.overlayView.invalidate()

                val now = System.currentTimeMillis()
                if (now - lastCoachTime >= COACH_INTERVAL && viewModel.isTrackingStarted.value == true) {
                    viewModel.processCoachLogic(resultBundle.results.first())
                    lastCoachTime = now
                }
            } else {
                binding.overlayView.clear()
                binding.overlayView.visibility = View.GONE
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        Log.e(TAG, "MediaPipe Error: $error")
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            if (!isAdded || view == null) return@addListener
            val cameraProvider = providerFuture.get()
            val previewUseCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            val analysisUseCase = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(backgroundExecutor) { proxy -> processFrame(proxy) } }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lensFacing).build(), previewUseCase, analysisUseCase)
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val shouldRunClassifier = (frameCounter % 5 == 0 && classifierInterpreter != null)
            var bitmap: Bitmap? = null
            if (shouldRunClassifier) bitmap = imageProxy.toBitmap()

            poseLandmarkerHelper?.detectLiveStream(imageProxy, lensFacing == CameraSelector.LENS_FACING_FRONT)

            frameCounter++
            if (shouldRunClassifier && bitmap != null) {
                classifierExecutor.execute { runPoseClassifier(bitmap) }
            }
        } catch (e: Exception) { imageProxy.close() }
    }

    private fun runPoseClassifier(bitmap: Bitmap) {
        val interpreter = classifierInterpreter ?: return
        val matrix = Matrix().apply {
            postScale(224f / bitmap.width, 224f / bitmap.height)
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) postScale(-1f, 1f, 112f, 112f)
        }
        val classifierInput = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val buffer = prepareByteBuffer(classifierInput)

        val outL3 = Array(1) { FloatArray(82) }
        val outputs = mutableMapOf<Int, Any>()
        for (i in 0 until interpreter.outputTensorCount) {
            if (interpreter.getOutputTensor(i).shape()[1] == 82) outputs[i] = outL3
        }

        synchronized(tfliteLock) {
            try { interpreter.runForMultipleInputsOutputs(arrayOf(buffer), outputs) } catch (e: Exception) {}
        }

        val probabilities = outL3[0]
        val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[maxIdx]

        Log.d(TAG, "AI -> ID: $maxIdx | Conf: $confidence")

        if (confidence > 0.70f) {
            if (maxIdx == lastPendingPoseId) poseCounter++ else { lastPendingPoseId = maxIdx; poseCounter = 0 }
            if (poseCounter >= STABLE_THRESHOLD) {
                activity?.runOnUiThread { viewModel.handlePoseInference(maxIdx) }
            }
        }
        classifierInput.recycle(); bitmap.recycle()
    }

    private fun prepareByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply { order(ByteOrder.nativeOrder()); rewind() }
        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)
        for (pixel in intValues) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }
        return byteBuffer
    }

    override fun initData() {
        viewModel.currentPoseName.observe(viewLifecycleOwner) { binding.tvYogaName.text = it }
        viewModel.currentGuideText.observe(viewLifecycleOwner) { binding.tvGuide.text = it }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }

        // Nhận lệnh nói từ ViewModel
        viewModel.speakCommand.observe(viewLifecycleOwner) { text ->
            if (text.isNotEmpty() && !isMuted) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        viewModel.previewPoseId.observe(viewLifecycleOwner) { id ->
            if (id != -1) {
                viewModel.yogaPoseDataList.value?.find { it.id == id }?.let {
                    binding.ivYogaSample.visibility = View.VISIBLE
                    binding.ivYogaSample.loadImageFromNetwork(it.photo_url)
                }
            } else binding.ivYogaSample.visibility = View.GONE
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.ivFlip.singleClick {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            startCamera()
        }

        // Nút loa để Mute/Unmute
        binding.ivVoice.singleClick {
            isMuted = !isMuted
            binding.ivVoice.setImageResource(if (isMuted) com.hadat.aiyoga.R.drawable.ic_mute else com.hadat.aiyoga.R.drawable.ic_volume)
            if (isMuted) tts?.stop()
        }

        // Logic giữ nút để thoát (Finish workout)
        binding.layoutAction.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                progressAnimator = ObjectAnimator.ofFloat(binding.progressAround, "progress", 0f, 100f).apply {
                    duration = 1500; interpolator = LinearInterpolator()
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            if (binding.progressAround.progress >= 100f) popBackStack()
                        }
                    })
                }; progressAnimator?.start()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                progressAnimator?.cancel(); binding.progressAround.progress = 0f
            }
            true
        }
    }

    private fun checkAndStartCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startCamera() }

    override fun onDestroyView() {
        viewModel.stopExerciseTimer()
        backgroundExecutor.shutdown()
        synchronized(tfliteLock) { classifierInterpreter?.close(); classifierInterpreter = null }
        poseLandmarkerHelper?.clearPoseLandmarker()
        tts?.shutdown() // Tắt loa khi thoát
        super.onDestroyView()
    }
}