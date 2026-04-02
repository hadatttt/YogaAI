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
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
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

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private val yogaPoses = mutableListOf<String>()
    private var tts: TextToSpeech? = null
    private var lastSpokenText: String = ""
    private var lastSpeakTime = 0L

    // MediaPipe Landmarker
    private var poseLandmarker: PoseLandmarker? = null
    private var detectedPoseId: Int = -1 // ID dùng để Coach chấm điểm sau khi giữ 5s

    // Logic Đếm thời gian
    private var lastPoseName: String? = null
    private var poseStartTime: Long = 0
    private var isTrackingStarted = false
    private var exerciseTimer: Timer? = null
    private var secondsElapsed = 0
    private val yogaPoseDataList = mutableListOf<YogaPoseModel>()
    override fun initView() {
        loadYogaPoseJson()
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
    private fun loadYogaPoseJson() {
        try {
            val jsonString = requireContext().assets.open("yoga_link.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                yogaPoseDataList.add(
                    YogaPoseModel(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        photo_url = obj.getString("photo_url")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("YogaAI", "❌ Error loading Yoga JSON: ${e.message}")
        }
    }
    private fun initTextToSpeech() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("vi", "VN") // tiếng Việt
                tts?.setSpeechRate(1.0f)
            }
        }
    }
    private fun speak(text: String) {
        val currentTime = System.currentTimeMillis()

        // Không đọc lại nếu giống câu trước và chưa đủ 3 giây
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
                    if (isAdded) {
                        // Gửi kết quả vẽ khung xương lên màn hình
                        binding.overlayView.setResults(result, binding.viewFinder.height, binding.viewFinder.width)
                        // Chạy logic sửa lỗi tư thế (Coach)
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
        } catch (e: Exception) {
            Log.e("YogaAI", "❌ Error loading labels")
        }
    }

    private fun loadClassifierModel() {
        try {
            val options = Interpreter.Options()
            try {
                options.addDelegate(GpuDelegate())
            } catch (e: Throwable) {
                Log.w("YogaAI", "GPU fallback to CPU")
            }
            val fd = requireContext().assets.openFd("yoga_model.tflite")
            val stream = FileInputStream(fd.fileDescriptor)
            val buffer = stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            classifierInterpreter = Interpreter(buffer, options)
        } catch (e: Exception) {
            Log.e("YogaAI", "❌ Model Load Error")
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (classifierInterpreter == null) { imageProxy.close(); return }

        try {
            val bitmap = imageProxy.toBitmap() ?: run { imageProxy.close(); return }

            // Xoay ảnh để đồng bộ giữa MediaPipe Landmarker và hiển thị
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // 1. Chạy MediaPipe Landmarker
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            poseLandmarker?.detectAsync(mpImage, System.currentTimeMillis())

            // 2. Chạy TFLite Classifier
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
            // FIX: Ép kiểu explicitly sang Map<Int, Any> để tránh Java type mismatch
            val outputs: MutableMap<Int, Any> = mutableMapOf(2 to out82)

            classifierInterpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputs)

            val maxIdx = out82[0].indices.maxByOrNull { out82[0][it] } ?: 0
            val confValue = out82[0][maxIdx]
            val currentPoseName = if (confValue > 0.65f) yogaPoses[maxIdx] else "Unknown"

            handlePoseLogic(currentPoseName, if (currentPoseName != "Unknown") maxIdx else -1)

        } catch (e: Exception) {
            Log.e("YogaAI", "Inference error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun handlePoseLogic(currentPose: String, poseId: Int) {
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread

            if (currentPose != "Unknown" && currentPose != "No Pose") {
                if (currentPose == lastPoseName) {
                    val elapsedTime = System.currentTimeMillis() - poseStartTime

                    // Bước 1: Giữ 5 giây để xác nhận tư thế chuẩn
                    if (elapsedTime >= 5000) {
                        if (!isTrackingStarted) {
                            startExerciseTimer()
                            val poseData = yogaPoseDataList.find { it.id == poseId }
                            poseData?.let {
                                binding.ivYogaSample.visibility = View.VISIBLE
                                // Gọi hàm Glide Until bạn mới viết
                                binding.ivYogaSample.loadImageFromNetwork(it.photo_url)
                            }
                        }
                        detectedPoseId = poseId // Bắt đầu cho phép Coach chấm điểm kĩ thuật
                    } else {
                        val countdown = 5 - (elapsedTime / 1000)
                        binding.tvGuide.text = "Giữ nguyên tư thế $currentPose ($countdown s)"
                    }
                } else {
                    // Reset khi đổi tư thế hoặc mới bắt đầu
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
        if (detectedPoseId != -1) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(detectedPoseId, result)

            activity?.runOnUiThread {
                if (!isCorrect) {
                    val text = "⚠️ $feedback"
                    binding.tvGuide.text = text
                    speak(feedback) // 🔊 đọc lỗi
                } else {
                    val text = "✅ Tư thế rất tốt, duy trì nhé!"
                    binding.tvGuide.text = text
                    speak("Tư thế đúng, giữ nguyên") // 🔊 đọc đúng
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
                    val min = secondsElapsed / 60
                    val sec = secondsElapsed % 60
                    binding.tvTimer.text = String.format("%02d:%02d", min, sec)
                }
            }
        }, 1000, 1000)
    }

    private fun stopExerciseTimer() {
        isTrackingStarted = false
        exerciseTimer?.cancel()
        exerciseTimer = null
        binding.lottieStatus.visibility = View.GONE
        binding.tvTimer.text = "00:00"
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
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExecutor) { img -> processImage(img) } }

            try {
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)
            } catch (e: Exception) {
                Log.e("YogaAI", "Camera Bind Error")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.ivFlipCamera.singleClick { toggleCamera() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopExerciseTimer()
        classifierInterpreter?.close()
        poseLandmarker?.close()
        cameraExecutor.shutdown()
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startCamera()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun initData() {}
}