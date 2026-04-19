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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.databinding.FragmentSingleYogaBinding
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import com.hadat.aiyoga.utils.ModelDownloader
import com.hadat.aiyoga.utils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class YogaFragment : BaseFragment<FragmentSingleYogaBinding, YogaViewModel>(),
    PoseLandmarkerHelper.LandmarkerListener {
    private var gpuDelegate: GpuDelegate? = null
    private val TAG = "YogaAI_Debug"
    private lateinit var backgroundExecutor: ExecutorService
    private val classifierExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private var classifierInterpreter: Interpreter? = null
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var tts: TextToSpeech? = null
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
        initTextToSpeech()
        checkAndStartCamera()

        ModelDownloader.downloadAllModels(requireContext(),
            onProgress = { progress ->
                activity?.runOnUiThread { binding.tvGuide.text = "Loading: $progress%" }
            },
            onComplete = { success ->
                if (success) initializeAiResources()
            }
        )
    }

    private fun initializeAiResources() {
        backgroundExecutor.execute {
            try {
                val context = context ?: return@execute
                val classifierFile = File(context.filesDir, "yoga_model.tflite")

                if (classifierFile.exists()) {
                    val options = Interpreter.Options().apply {
                        setNumThreads(4)

                        try {
                            gpuDelegate = GpuDelegate()
                            addDelegate(gpuDelegate)
                            Log.d(TAG, "TFLite using GPU Delegate")
                        } catch (e: Exception) {
                            setUseNNAPI(true)
                            Log.d(TAG, "GPU not available, fallback NNAPI")
                        }
                    }

                    synchronized(tfliteLock) {
                        classifierInterpreter = Interpreter(classifierFile, options)
                    }

                    Log.d(TAG, "TFLite model loaded")
                }

                poseLandmarkerHelper = PoseLandmarkerHelper(
                    context = context,
                    runningMode = RunningMode.LIVE_STREAM,
                    currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_HEAVY,
                    currentDelegate = PoseLandmarkerHelper.DELEGATE_GPU,
                    poseLandmarkerHelperListener = this
                )

                YogaCoachUtils.loadReferenceData { isSuccess ->
                    activity?.runOnUiThread {
                        viewModel.fetchYogaPoses()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing AI resources: ${e.message}")
            }
        }
    }
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (view == null) return@runOnUiThread
            binding.overlayView.visibility = View.VISIBLE
            binding.overlayView.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            binding.overlayView.invalidate()
            val now = System.currentTimeMillis()
            if (now - lastCoachTime >= COACH_INTERVAL) {
                viewModel.processCoachLogic(resultBundle.results.first())
                lastCoachTime = now
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
            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(backgroundExecutor) { proxy -> processFrame(proxy) } }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lensFacing).build(), preview, analysis)
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    postScale(-1f, 1f, bitmap!!.width / 2f, bitmap.height / 2f)
                }
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)

            poseLandmarkerHelper?.detectLiveStream(imageProxy, lensFacing == CameraSelector.LENS_FACING_FRONT)
            frameCounter++
            if (frameCounter % 5 == 0 && classifierInterpreter != null) {
                classifierExecutor.execute { runPoseClassifier(rotatedBitmap) }
            }
        } catch (e: Exception) { imageProxy.close() }
    }

    private fun runPoseClassifier(bitmap: Bitmap) {
        val interpreter = classifierInterpreter ?: return
        val scaled = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val buffer = prepareByteBuffer(scaled)

        val out82 = Array(1) { FloatArray(82) }
        val outputs: MutableMap<Int, Any> = mutableMapOf(2 to out82)

        synchronized(tfliteLock) {
            try { interpreter.runForMultipleInputsOutputs(arrayOf(buffer), outputs) } catch (e: Exception) {}
        }

        val probabilities = out82[0]
        val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[maxIdx]

        if (confidence > 0.65f) {
            if (maxIdx == lastPendingPoseId) poseCounter++ else { lastPendingPoseId = maxIdx; poseCounter = 0 }
            if (poseCounter >= STABLE_THRESHOLD) {
                activity?.runOnUiThread { viewModel.handlePoseInference(maxIdx) }
            }
        }
        bitmap.recycle(); scaled.recycle()
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
        viewModel.currentPoseName.observe(viewLifecycleOwner) { name ->
            binding.tvYogaName.text = name
        }
        viewModel.isWaitingForCapture.observe(viewLifecycleOwner) { isWaiting ->
            if (isWaiting) {
                binding.ivPhoto.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                binding.ivPhoto.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                binding.tvGuide.text = "Giữ đúng tư thế để chụp ảnh!"
            } else {
                binding.ivPhoto.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E4E3F3"))
                binding.ivPhoto.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), com.hadat.aiyoga.R.color.primary))
            }
        }

        viewModel.captureTrigger.observe(viewLifecycleOwner) {
            animatePhotoCapture()
        }
        viewModel.currentGuideText.observe(viewLifecycleOwner) { binding.tvGuide.text = it }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }

        viewModel.speakCommand.observe(viewLifecycleOwner) { text ->
            if (text.isNotEmpty() && !isMuted) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
        viewModel.previewPoseId.observe(viewLifecycleOwner) { id ->
            if (id != -1) {
                viewModel.yogaPoseDataList.value?.find { it.id == id }?.let { pose ->
                    binding.ivYogaSample.visibility = View.VISIBLE
                    binding.ivYogaSample.loadImageFromNetwork(pose.photo_url)
                }
            } else {
                binding.ivYogaSample.visibility = View.GONE
            }
        }
    }

    override fun initListener() {
        binding.ivPhoto.singleClick {
            viewModel.toggleCaptureWait()
        }
        binding.ivBack.singleClick {
            viewModel.clearData()
            popBackStack()
        }
        onBackPressed {
            viewModel.clearData()
            popBackStack()
        }
        binding.ivFlip.singleClick {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            startCamera()
        }
        binding.ivVoice.singleClick {
            isMuted = !isMuted
            binding.ivVoice.setImageResource(if (isMuted) com.hadat.aiyoga.R.drawable.ic_mute else com.hadat.aiyoga.R.drawable.ic_volume)
        }
        progressAnimator = ObjectAnimator.ofFloat(binding.progressAround, "progress", 0f, 100f).apply {
            duration = 1500
            interpolator = LinearInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (binding.progressAround.progress >= 100f) {
                        navigateToResult()
                    }
                }
            })
        }
        binding.layoutAction.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    progressAnimator?.start()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    progressAnimator?.cancel()
                    binding.progressAround.progress = 0f
                    true
                }
                else -> false
            }
        }
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale("vi", "VN")
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

        synchronized(tfliteLock) {
            classifierInterpreter?.close()
            classifierInterpreter = null
        }
        gpuDelegate?.close()
        gpuDelegate = null
        poseLandmarkerHelper?.clearPoseLandmarker()
        tts?.shutdown()
        super.onDestroyView()
    }
    private fun navigateToResult() {
        val results = viewModel.getFinalSequenceList().map { sequence ->
            WorkoutResultModel(
                userId = com.hadat.aiyoga.service.AppPreferences.getUserId(requireContext()) ?: "guest",
                poseUrl = sequence.photoUrl,
                poseId = sequence.id.toIntOrNull() ?: -1,
                poseName = sequence.name,
                durationInSeconds = timeStringToSeconds(sequence.duration),
                date = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                capturedImages = viewModel.getCapturedImages(),
                errorCount = 0,
                workoutTimestamp = System.currentTimeMillis()
            )
        }.toTypedArray()

        val bundle = Bundle().apply {
            putParcelableArray("workout_result_list", results)
        }
        navigate(com.hadat.aiyoga.R.id.resultFragment, bundle)
        viewModel.clearData()
    }

    private fun timeStringToSeconds(time: String): Int {
        val parts = time.split(":")
        return if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
    }
    private fun animatePhotoCapture() {
        triggerFlashEffect()
        val bitmap = binding.viewFinder.bitmap ?: return

        val photoView = androidx.appcompat.widget.AppCompatImageView(requireContext()).apply {
            setImageBitmap(bitmap)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                binding.viewFinder.width / 3, binding.viewFinder.height / 3
            )
            elevation = 50f
            translationX = (binding.viewFinder.width / 3).toFloat()
            translationY = (binding.viewFinder.height / 4).toFloat()
        }

        (binding.root as android.view.ViewGroup).addView(photoView)

        photoView.animate()
            .translationY(binding.root.height.toFloat())
            .scaleX(0.2f).scaleY(0.2f).alpha(0f)
            .setDuration(800)
            .withEndAction { (binding.root as android.view.ViewGroup).removeView(photoView) }
            .start()

        saveScreenshot(bitmap)
    }
    private fun saveScreenshot(bitmap: Bitmap) {
        val filename = "Yoga_Sequence_${System.currentTimeMillis()}.jpg"
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/AI_Yoga")
        }

        val uri = requireContext().contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        )

        uri?.let {
            requireContext().contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            viewModel.addCapturedImage(it.toString())
            Toast.makeText(
                requireContext(),
                getString(com.hadat.aiyoga.R.string.save_photo),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun triggerFlashEffect() {
        binding.viewFlash.visibility = View.VISIBLE
        binding.viewFlash.alpha = 1f
        binding.viewFlash.animate().alpha(0f).setDuration(200)
            .withEndAction { binding.viewFlash.visibility = View.GONE }
            .start()
    }
}