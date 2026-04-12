package com.hadat.aiyoga.yogamain

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.speech.tts.TextToSpeech
import android.util.Size
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.hadat.aiyoga.databinding.FragmentYogaBinding
import com.hadat.aiyoga.utils.ModelDownloader
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
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

class YogaFragment : BaseFragment<FragmentYogaBinding, YogaViewModel>() {

    private var classifierInterpreter: Interpreter? = null
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val classifierExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val tfliteLock = Any()

    private var isFragmentDestroyed = false
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private val yogaPoses = mutableListOf<String>()

    private var lastPendingPoseId = -1
    private var poseCounter = 0
    private var currentStablePoseId = -1
    private val STABLE_THRESHOLD = 3

    private var tts: TextToSpeech? = null
    private var poseLandmarker: PoseLandmarker? = null

    private var lastSpokenText = ""
    private var lastSpeakTime = 0L
    private var frameCounter = 0
    private var lastCoachTime = 0L
    private val COACH_INTERVAL = 2000L

    override fun initView() {
        checkAndStartCamera()
        binding.ivFlipCamera.isEnabled = false
        ModelDownloader.downloadAllModels(requireContext(),
            onProgress = { progress ->
                activity?.runOnUiThread {
                    if (isAdded && !isFragmentDestroyed) binding.tvGuide.text = "Downloading: $progress%"
                }
            },
            onComplete = { success ->
                activity?.runOnUiThread {
                    if (!isAdded || isFragmentDestroyed) return@runOnUiThread
                    if (success) {
                        binding.root.postDelayed({ initializeAiResources() }, 300)
                    }
                }
            }
        )
    }

    private fun initializeAiResources() {
        classifierExecutor.execute {
            try {
                val classifierFile = File(requireContext().filesDir, "yoga_model.tflite")
                val taskFile = File(requireContext().filesDir, "pose_landmarker_heavy.task")

                if (classifierFile.exists() && taskFile.exists()) {
                    setupClassifier(classifierFile)
                    setupPoseLandmarker(taskFile)
                    YogaCoachUtils.loadReferenceData(requireContext())

                    val labels = YogaCoachUtils.getPoseLabels()
                    activity?.runOnUiThread {
                        if (isFragmentDestroyed || !isAdded) return@runOnUiThread
                        yogaPoses.clear()
                        yogaPoses.addAll(labels)
                        initTextToSpeech()
                        viewModel.fetchYogaPoses()
                        binding.ivFlipCamera.isEnabled = true
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun checkAndStartCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupClassifier(file: File) {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(true)
        }
        classifierInterpreter = Interpreter(file, options)
    }

    private fun setupPoseLandmarker(taskFile: File) {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(taskFile.absolutePath).build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                activity?.runOnUiThread {
                    if (isAdded && !isFragmentDestroyed) {
//                        binding.overlayView.setResults(
//                            result,
//                            result.inputImageHeight(),
//                            result.inputImageWidth()
//                        )

                        val currentTime = System.currentTimeMillis()
                        val currentId = viewModel.detectedPoseId.value ?: -1
                        val started = viewModel.isTrackingStarted.value ?: false

                        if (currentId != -1 && started) {
                            if (currentTime - lastCoachTime >= COACH_INTERVAL) {
                                viewModel.processCoachLogic(result)
                                lastCoachTime = currentTime
                            }
                        } else {
                            lastCoachTime = 0L
                        }
                    }
                }
            }
            .build()
        poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (isFragmentDestroyed || poseLandmarker == null) {
            imageProxy.close()
            return
        }
        try {
            val bitmap = imageProxy.toBitmap() ?: return
            val rotation = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()

            // Xử lý MediaPipe Pose
            poseLandmarker?.detectAsync(mpImage, System.currentTimeMillis())

            // Xử lý Classifier (Giảm tần suất)
            frameCounter++
            if (frameCounter % 5 == 0) {
                classifierExecutor.execute { performPoseInference(rotatedBitmap) }
            }
        } catch (e: Exception) {
            imageProxy.close()
        } finally {
            imageProxy.close()
        }
    }

    private fun performPoseInference(bitmap: Bitmap) {
        val classifierInput = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val buffer = prepareByteBuffer(classifierInput)
        classifierInput.recycle()

        val outL1 = Array(1) { FloatArray(6) }
        val outL2 = Array(1) { FloatArray(20) }
        val outL3 = Array(1) { FloatArray(82) }
        val outputs = mutableMapOf<Int, Any>()

        val outputCount = classifierInterpreter?.outputTensorCount ?: 0
        for (i in 0 until outputCount) {
            val size = classifierInterpreter?.getOutputTensor(i)?.shape()?.get(1) ?: 0
            when (size) {
                6 -> outputs[i] = outL1
                20 -> outputs[i] = outL2
                82 -> outputs[i] = outL3
            }
        }

        synchronized(tfliteLock) {
            if (!isFragmentDestroyed && classifierInterpreter != null) {
                classifierInterpreter?.runForMultipleInputsOutputs(arrayOf(buffer), outputs)
            }
        }

        val probabilities = outL3[0]
        val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[maxIdx]

        if (confidence > 0.70f) {
            if (maxIdx == lastPendingPoseId) poseCounter++ else { lastPendingPoseId = maxIdx; poseCounter = 0 }
            if (poseCounter >= STABLE_THRESHOLD && maxIdx != currentStablePoseId) {
                currentStablePoseId = maxIdx
                activity?.runOnUiThread { viewModel.handlePoseInference(yogaPoses.getOrNull(maxIdx) ?: "Unknown", maxIdx) }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            if (isFragmentDestroyed) return@addListener
            val cameraProvider = cameraProviderFuture.get()
            val targetSize = Size(720, 1280)
            val preview = Preview.Builder().setTargetResolution(targetSize).build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(targetSize)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExecutor) { img -> processImage(img) } }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.Builder().requireLensFacing(lensFacing).build(), preview, analysis)
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(requireContext()))
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

    private fun speak(text: String) {
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        if (text == lastSpokenText && now - lastSpeakTime < 2500) return
        lastSpokenText = text; lastSpeakTime = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun initData() {
        viewModel.currentGuideText.observe(viewLifecycleOwner) { binding.tvGuide.text = it }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }
        viewModel.speakCommand.observe(viewLifecycleOwner) { speak(it) }
        viewModel.isTrackingStarted.observe(viewLifecycleOwner) { started ->
            binding.lottieStatus.visibility = if (started) View.VISIBLE else View.GONE
            if (started) binding.lottieStatus.playAnimation() else binding.lottieStatus.pauseAnimation()
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
        binding.ivFlipCamera.singleClick {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            startCamera()
        }
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(requireContext()) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startCamera() }

    override fun onDestroyView() {
        isFragmentDestroyed = true
        cameraExecutor.shutdownNow()
        classifierExecutor.shutdownNow()
        synchronized(tfliteLock) { classifierInterpreter?.close(); classifierInterpreter = null }
        poseLandmarker?.close(); tts?.shutdown()
        super.onDestroyView()
    }
}