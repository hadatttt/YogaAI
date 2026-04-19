package com.hadat.aiyoga.multimodeyoga

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentMultiModeYogaBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.utils.ModelDownloader
import com.hadat.aiyoga.utils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MultiModeYogaFragment : BaseFragment<FragmentMultiModeYogaBinding, MultiModeYogaViewModel>(),
    PoseLandmarkerHelper.LandmarkerListener {

    private val args by navArgs<MultiModeYogaFragmentArgs>()

    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private var tts: TextToSpeech? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastSpeakTime = 0L
    private var lastCoachTime = 0L
    private val coachInterval = 2000L
    private var progressAnimator: ObjectAnimator? = null
    private var isMuted = false

    override fun initView() {
        backgroundExecutor = Executors.newSingleThreadExecutor()
        checkAndStartCamera()
        initTextToSpeech()

        ModelDownloader.downloadAllModels(
            requireContext(),
            onProgress = { progress ->
                activity?.runOnUiThread { binding.tvGuide.text = "Loading: $progress%" }
            },
            onComplete = { success ->
                if (success) initializePoseLandmarkerHelper()
            }
        )
    }

    private fun initializePoseLandmarkerHelper() {
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_HEAVY,
                currentDelegate = PoseLandmarkerHelper.DELEGATE_GPU,
                poseLandmarkerHelperListener = this
            )
            YogaCoachUtils.loadReferenceData { isSuccess ->
                if (isSuccess) {
                    activity?.runOnUiThread {
                        args.detailSequence?.let { viewModel.startWorkout(requireContext(),it) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundExecutor.execute {
            if (::poseLandmarkerHelper.isInitialized && poseLandmarkerHelper.isClose()) {
                poseLandmarkerHelper.setupPoseLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::poseLandmarkerHelper.isInitialized) {
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    override fun onDestroyView() {
        binding.overlayView.clear()
        viewModel.stopTracking()
        progressAnimator?.cancel()
        progressAnimator = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        imageAnalyzer?.clearAnalyzer()
        imageAnalyzer = null
        preview = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        if (::poseLandmarkerHelper.isInitialized) poseLandmarkerHelper.clearPoseLandmarker()
        if (::backgroundExecutor.isInitialized) backgroundExecutor.shutdownNow()
        super.onDestroyView()
    }

    override fun initData() {
        viewModel.resetData()
        viewModel.currentGuideText.observe(viewLifecycleOwner) { binding.tvGuide.text = it }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }
        viewModel.poseCountText.observe(viewLifecycleOwner) { binding.tvPoseIndex.text = it }
        viewModel.speakCommand.observe(viewLifecycleOwner) { speak(it) }
        viewModel.currentPose.observe(viewLifecycleOwner) { pose ->
            pose ?: return@observe
            binding.tvYogaName.text = pose.name
            binding.ivYogaSample.loadImageFromNetwork(pose.photoUrl)
        }
        viewModel.isWaitingForCapture.observe(viewLifecycleOwner) { isWaiting ->
            if (isWaiting) {
                binding.ivPhoto.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                binding.ivPhoto.imageTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                binding.tvGuide.text = getString(com.hadat.aiyoga.R.string.guide_hold_to_capture)
            } else {
                binding.ivPhoto.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E4E3F3"))
                binding.ivPhoto.imageTintList =
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        }
        viewModel.captureTrigger.observe(viewLifecycleOwner) { animatePhotoCapture() }
        viewModel.isTrackingStarted.observe(viewLifecycleOwner) { started ->
            binding.lottieStatus.visibility = if (started) View.VISIBLE else View.GONE
        }
        viewModel.sessionCompleted.observe(viewLifecycleOwner) { navigateToResult() }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.ivPhoto.singleClick { viewModel.toggleCaptureWait() }
        binding.ivVoice.singleClick {
            isMuted = !isMuted
            if (isMuted) {
                binding.ivVoice.setImageResource(R.drawable.ic_mute)
                tts?.stop()
            } else {
                binding.ivVoice.setImageResource(R.drawable.ic_volume)
            }
        }
        binding.ivFlip.singleClick {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUseCases()
        }
        binding.btnNextPose.singleClick { viewModel.moveToNextPose(requireContext()) }

        binding.progressAround.apply {
            progressMax = 100f
            progress = 0f
        }

        progressAnimator = ObjectAnimator.ofFloat(binding.progressAround, "progress", 0f, 100f).apply {
            duration = 1500
            interpolator = android.view.animation.LinearInterpolator()
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

    private fun checkAndStartCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            setUpCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) setUpCamera()
        }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            if (!isAdded || view == null) return@addListener
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        if (!isAdded || view == null) return
        val cameraProvider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        val targetRotation = view?.display?.rotation ?: Surface.ROTATION_0

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(targetRotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(targetRotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { imageProxy -> detectPose(imageProxy) }
            }

        cameraProvider.unbindAll()
        try {
            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (_: Exception) {
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (view == null) return@runOnUiThread
            binding.overlayView.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            binding.overlayView.invalidate()

            val now = System.currentTimeMillis()
            if (now - lastCoachTime >= coachInterval) {
                viewModel.processCoachLogic(requireContext(), resultBundle.results.first())
                lastCoachTime = now
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun speak(text: String) {
        if (text.isEmpty() || isMuted) return
        val now = System.currentTimeMillis()
        if (now - lastSpeakTime < 2500) return
        lastSpeakTime = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langCode = AppPreferences.getLanguageCode(requireContext())

                val locale = if (langCode == "vi") {
                    Locale("vi", "VN")
                } else {
                    Locale.ENGLISH
                }

                val result = tts?.setLanguage(locale)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                }
            }
        }
    }

    private fun animatePhotoCapture() {
        triggerFlashEffect()
        val bitmap = binding.viewFinder.bitmap ?: return
        val photoView = androidx.appcompat.widget.AppCompatImageView(requireContext()).apply {
            setImageBitmap(bitmap)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                binding.viewFinder.width / 3,
                binding.viewFinder.height / 3
            )
            elevation = 50f
            translationX = (binding.viewFinder.width / 3).toFloat()
            translationY = (binding.viewFinder.height / 4).toFloat()
        }
        (binding.root as android.view.ViewGroup).addView(photoView)
        photoView.animate()
            .translationY(binding.root.height.toFloat())
            .scaleX(0.2f)
            .scaleY(0.2f)
            .alpha(0f)
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
        binding.viewFlash.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { binding.viewFlash.visibility = View.GONE }
            .start()
    }

    private fun navigateToResult() {
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        val results = viewModel.buildResultList(userId)
        val bundle = Bundle().apply {
            putParcelableArray("workout_result_list", results)
        }
        navigate(R.id.resultFragment, bundle,isPop = true)
    }
}

