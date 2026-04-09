package com.hadat.aiyoga.singleyoga

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.speech.tts.TextToSpeech
import android.util.Size
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.hadat.aiyoga.databinding.FragmentSingleYogaBinding
import com.hadat.aiyoga.utils.ModelDownloader
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.yogautils.YogaCoachUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SingleYogaFragment : BaseFragment<FragmentSingleYogaBinding, SingleYogaViewModel>() {

    private val args by navArgs<SingleYogaFragmentArgs>()
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val aiExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private var poseLandmarker: PoseLandmarker? = null
    private var tts: TextToSpeech? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private var lastSpeakTime = 0L
    private var lastCoachTime = 0L
    private val COACH_INTERVAL = 2000L
    private var progressAnimator: ObjectAnimator? = null

    override fun initView() {
        checkAndStartCamera()
        initTextToSpeech()

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
        aiExecutor.execute {
            try {
                val taskFile = File(requireContext().filesDir, "pose_landmarker_lite.task")
                if (taskFile.exists()) {
                    setupPoseLandmarker(taskFile)
                    YogaCoachUtils.loadReferenceData(requireContext())

                    activity?.runOnUiThread {
                        viewModel.fetchYogaPoses()
                        viewModel.startSinglePoseTracking(args.yogaPoseItem.id)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun setupPoseLandmarker(taskFile: File) {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(taskFile.absolutePath).build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    binding.overlayView.setResults(result, binding.viewFinder.height, binding.viewFinder.width)

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastCoachTime >= COACH_INTERVAL) {
                        viewModel.processCoachLogic(result, args.yogaPoseItem.id)
                        lastCoachTime = currentTime
                    }
                }
            }
            .build()
        poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (poseLandmarker == null) {
            imageProxy.close()
            return
        }
        try {
            val bitmap = imageProxy.toBitmap() ?: return
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            poseLandmarker?.detectAsync(mpImage, System.currentTimeMillis())
        } finally {
            imageProxy.close()
        }
    }

    override fun initData() {
        viewModel.currentGuideText.observe(viewLifecycleOwner) { binding.tvGuide.text = it }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }
        viewModel.speakCommand.observe(viewLifecycleOwner) { speak(it) }

        viewModel.isTrackingStarted.observe(viewLifecycleOwner) { started ->
            binding.lottieStatus.visibility = if (started) View.VISIBLE else View.GONE
        }

        viewModel.yogaPoseDataList.observe(viewLifecycleOwner) { list ->
            list.find { it.id == args.yogaPoseItem.id }?.let {
                binding.ivYogaSample.visibility = View.VISIBLE
                binding.ivYogaSample.loadImageFromNetwork(it.photo_url)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.progressAround.max = 1000
        binding.progressAround.progress = 0

        // 2. Cấu hình Animator
        progressAnimator = ObjectAnimator.ofInt(binding.progressAround, "progress", 0, 1000).apply {
            duration = 1500 // 1.5 giây để xoay hết 1 vòng
            interpolator = android.view.animation.LinearInterpolator()

            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (binding.progressAround.progress >= 1000) {
                        // Xử lý chuyển Fragment khi hoàn thành vòng xoay
                        navigateToNextFragment()
                    }
                }
            })
        }

        // 3. Xử lý sự kiện Touch
        binding.layoutAction.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    progressAnimator?.start()
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    progressAnimator?.cancel()
                    binding.progressAround.progress = 0
                    true
                }
                else -> false
            }
        }
        binding.ivFlip.singleClick {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            startCamera()
        }
    }
    private fun navigateToNextFragment() {
        popBackStack()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
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

    private fun speak(text: String) {
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastSpeakTime < 2500) return
        lastSpeakTime = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale("vi", "VN")
        }
    }

    private fun checkAndStartCamera() {
        if (allPermissionsGranted()) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startCamera() }

    override fun onDestroyView() {
        progressAnimator?.cancel() // Thêm dòng này
        cameraExecutor.shutdown()
        aiExecutor.shutdown()
        poseLandmarker?.close()
        tts?.shutdown()
        super.onDestroyView()
    }
}