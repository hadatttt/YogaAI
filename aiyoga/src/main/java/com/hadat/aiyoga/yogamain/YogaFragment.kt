package com.hadat.aiyoga.yogamain

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.databinding.FragmentYogaBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.yogautils.YogaCoachUtils
import com.hadat.aiyoga.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class YogaFragment : BaseFragment<FragmentYogaBinding, YogaViewModel>() {

    private var classifierInterpreter: Interpreter? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val tfliteLock = Any()
    private var isFragmentDestroyed = false

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private val yogaPoses = mutableListOf<String>()
    private var tts: TextToSpeech? = null
    private var lastSpokenText: String = ""
    private var lastSpeakTime = 0L

    private var poseLandmarker: PoseLandmarker? = null
    private var detectedPoseId: Int = -1

    private var lastPoseName: String? = null
    private var poseStartTime: Long = 0
    private var isTrackingStarted = false
    private var exerciseTimer: Timer? = null
    private var secondsElapsed = 0

    private val yogaPoseDataList = mutableListOf<YogaPoseModel>()

    override fun initView() {
        YogaDataUtils.getRemoteYogaPoses { poses ->
            activity?.runOnUiThread {
                if (isAdded && !isFragmentDestroyed && poses != null) {
                    yogaPoseDataList.clear()
                    yogaPoseDataList.addAll(poses)
                }
            }
        }

        loadLabels()
        loadClassifierModel()
        setupPoseLandmarker()
        YogaCoachUtils.loadReferenceData(requireContext())

        binding.tvTimer.text = "00:00"
        binding.lottieStatus.visibility = View.GONE
        binding.overlayView.visibility = View.VISIBLE

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("vi", "VN")
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    private fun speak(text: String) {
        if (tts == null || isFragmentDestroyed) return

        val currentTime = System.currentTimeMillis()
        if (text == lastSpokenText && currentTime - lastSpeakTime < 3000) return

        lastSpokenText = text
        lastSpeakTime = currentTime
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun setupPoseLandmarker() {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_heavy.task").build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                activity?.runOnUiThread {
                    if (isAdded && !isFragmentDestroyed) {
                        binding.overlayView.setResults(result, binding.viewFinder.height, binding.viewFinder.width)
                        processYogaCoach(result)
                    }
                }
            }
            .build()
        poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
    }

    private fun loadLabels() {
        try {
            requireContext().assets.open("labels.txt").bufferedReader().useLines { lines ->
                yogaPoses.clear()
                yogaPoses.addAll(lines)
            }
        } catch (e: Exception) {}
    }

    private fun loadClassifierModel() {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            val fd = requireContext().assets.openFd("yoga_model.tflite")
            val stream = FileInputStream(fd.fileDescriptor)
            val buffer = stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            classifierInterpreter = Interpreter(buffer, options)
        } catch (e: Exception) {}
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (isFragmentDestroyed || classifierInterpreter == null) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap() ?: run { imageProxy.close(); return }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            poseLandmarker?.detectAsync(mpImage, System.currentTimeMillis())

            val scaled = Bitmap.createScaledBitmap(rotatedBitmap, 224, 224, true)
            val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
                rewind()
            }
            val intValues = IntArray(224 * 224)
            scaled.getPixels(intValues, 0, 224, 0, 0, 224, 224)
            for (pixel in intValues) {
                byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }

            val out82 = Array(1) { FloatArray(82) }
            val outputs: MutableMap<Int, Any> = mutableMapOf(2 to out82)

            synchronized(tfliteLock) {
                if (!isFragmentDestroyed) {
                    classifierInterpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputs)
                }
            }

            val maxIdx = out82[0].indices.maxByOrNull { out82[0][it] } ?: 0
            val confValue = out82[0][maxIdx]
            val currentPoseName = if (confValue > 0.65f) yogaPoses[maxIdx] else "Unknown"

            handlePoseLogic(currentPoseName, if (currentPoseName != "Unknown") maxIdx else -1)

        } catch (e: Exception) {
        } finally {
            imageProxy.close()
        }
    }

    private fun handlePoseLogic(currentPose: String, poseId: Int) {
        activity?.runOnUiThread {
            if (!isAdded || isFragmentDestroyed) return@runOnUiThread

            if (currentPose != "Unknown" && currentPose != "No Pose") {
                if (currentPose == lastPoseName) {
                    val elapsedTime = System.currentTimeMillis() - poseStartTime

                    if (elapsedTime >= 5000) {
                        if (!isTrackingStarted) {
                            startExerciseTimer()
                            val poseData = yogaPoseDataList.find { it.id == poseId }
                            poseData?.let {
                                binding.ivYogaSample.visibility = View.VISIBLE
                                binding.ivYogaSample.loadImageFromNetwork(it.photo_url)
                            }
                        }
                        detectedPoseId = poseId
                    } else {
                        val countdown = 5 - (elapsedTime / 1000)
                        binding.tvGuide.text = "Giữ nguyên tư thế $currentPose ($countdown s)"
                    }
                } else {
                    lastPoseName = currentPose
                    poseStartTime = System.currentTimeMillis()
                    detectedPoseId = -1
                    if (isTrackingStarted) stopExerciseTimer()
                    binding.tvGuide.text = "Chuẩn bị thực hiện: $currentPose"
                }
            } else {
                lastPoseName = "Unknown"
                detectedPoseId = -1
                binding.tvGuide.text = "Hãy thực hiện tư thế Yoga"
                if (isTrackingStarted) stopExerciseTimer()
            }
        }
    }

    private fun processYogaCoach(result: PoseLandmarkerResult) {
        if (detectedPoseId != -1 && !isFragmentDestroyed) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(detectedPoseId, result)

            activity?.runOnUiThread {
                if (isAdded && !isFragmentDestroyed) {
                    if (!isCorrect) {
                        binding.tvGuide.text = "⚠️ $feedback"
                        speak(feedback)
                    } else {
                        binding.tvGuide.text = "✅ Tư thế đúng, duy trì nhé!"
                        speak("Tư thế đúng, giữ nguyên")
                    }
                }
            }
        }
    }

    private fun startExerciseTimer() {
        isTrackingStarted = true
        secondsElapsed = 0
        binding.lottieStatus.visibility = View.VISIBLE
        binding.lottieStatus.playAnimation()
        exerciseTimer = Timer()
        exerciseTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                secondsElapsed++
                activity?.runOnUiThread {
                    if (isAdded && !isFragmentDestroyed) {
                        val min = secondsElapsed / 60
                        val sec = secondsElapsed % 60
                        binding.tvTimer.text = String.format("%02d:%02d", min, sec)
                    }
                }
            }
        }, 1000, 1000)
    }

    private fun stopExerciseTimer() {
        isTrackingStarted = false
        exerciseTimer?.cancel()
        exerciseTimer = null
        if (isAdded && !isFragmentDestroyed) {
            binding.lottieStatus.visibility = View.GONE
            binding.tvTimer.text = "00:00"
        }
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            if (!isAdded || isFragmentDestroyed) return@addListener

            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExecutor) { img -> processImage(img) } }

            try {
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, analyzer)
            } catch (e: Exception) {}
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.ivFlipCamera.singleClick { toggleCamera() }
    }

    override fun onDestroyView() {
        isFragmentDestroyed = true
        cameraExecutor.shutdownNow()
        stopExerciseTimer()

        synchronized(tfliteLock) {
            classifierInterpreter?.close()
            classifierInterpreter = null
        }

        poseLandmarker?.close()
        poseLandmarker = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onDestroyView()
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startCamera()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun initData() {}
}