package com.hadat.aiyoga.yoga_single

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Surface
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.databinding.FragmentSingleYogaBinding
import com.hadat.aiyoga.manager_ai.AIManager
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.yogautils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.hadat.aiyoga.yoga_ai.DialogZoom
import com.takusemba.spotlight.OnSpotlightListener
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.RoundedRectangle
import com.takusemba.spotlight.Target as SpotlightTarget

class SingleYogaFragment : BaseFragment<FragmentSingleYogaBinding, SingleYogaViewModel>(),
    PoseLandmarkerHelper.LandmarkerListener {

    private val args by navArgs<SingleYogaFragmentArgs>()
    private lateinit var spotlight: Spotlight
    private lateinit var backgroundExecutor: ExecutorService
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var tts: TextToSpeech? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastSpeakTime = 0L
    private var lastCoachTime = 0L
    private val COACH_INTERVAL = 2000L
    private var progressAnimator: ObjectAnimator? = null
    private var isMuted = false

    override fun initView() {
        backgroundExecutor = Executors.newSingleThreadExecutor()
        initTextToSpeech()
        initializePoseLandmarkerHelper()
    }
    private fun initializePoseLandmarkerHelper() {
        binding.loadingView.root.visibility = View.VISIBLE
        poseLandmarkerHelper = AIManager.getLandmarker()
        AIManager.setListener(this)

        YogaCoachUtils.loadReferenceData { isSuccess ->
            if (isSuccess && isAdded) {
                activity?.runOnUiThread {
                    viewModel.fetchYogaPoses(requireContext().applicationContext)
                    viewModel.startSinglePoseTracking(requireContext(), args.yogaPoseItem.id)
                    binding.loadingView.root.visibility = View.GONE
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        checkAndStartCamera()
        val landmarker = AIManager.getLandmarker()
        if (landmarker != null) {
            AIManager.setListener(this)
            backgroundExecutor.execute {
                if (landmarker.isClose()) {
                    landmarker.setupPoseLandmarker()
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
        cameraProvider = null
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

        AIManager.getLandmarker()?.poseLandmarkerHelperListener = null
        poseLandmarkerHelper = null
        if (::backgroundExecutor.isInitialized) {
            backgroundExecutor.shutdownNow()
        }
        super.onDestroyView()
    }
    private fun checkAndStartCamera() {
        if (allPermissionsGranted()) setUpCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

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

        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

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
                it.setAnalyzer(backgroundExecutor) { imageProxy ->
                    detectPose(imageProxy)
                }
            }

        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (e: Exception) { }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        AIManager.getLandmarker()?.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (view == null) return@runOnUiThread

            val result = resultBundle.results.first()

            binding.overlayView.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )

            val rays = YogaCoachUtils.getCorrectionRays(
                args.yogaPoseItem.id,
                result
            )
            binding.overlayView.setCorrectionRays(rays)

            val now = System.currentTimeMillis()
            if (now - lastCoachTime >= COACH_INTERVAL) {
                viewModel.processCoachLogic(
                    requireContext(),
                    result,
                    args.yogaPoseItem.id
                )
                lastCoachTime = now
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun initData() {
        viewModel.resetData()
        val initialPose = args.yogaPoseItem
        if (!initialPose.photo_url.isNullOrEmpty()) {
            binding.ivYogaSample.visibility = View.VISIBLE
            binding.ivYogaSample.loadImageFromNetwork(initialPose.photo_url)
        }
        binding.tvYogaName.text = initialPose.name
        viewModel.currentGuideText.observe(viewLifecycleOwner) { binding.tvGuide.text = it }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }
        viewModel.speakCommand.observe(viewLifecycleOwner) { speak(it) }
        viewModel.isWaitingForCapture.observe(viewLifecycleOwner) { isWaiting ->
            if (isWaiting) {
                binding.ivPhoto.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                binding.ivPhoto.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                binding.tvGuide.text = getString(com.hadat.aiyoga.R.string.guide_hold_to_capture)
            } else {
                binding.ivPhoto.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E4E3F3"))
                binding.ivPhoto.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), com.hadat.aiyoga.R.color.primary))
            }
        }
        viewModel.captureTrigger.observe(viewLifecycleOwner) { animatePhotoCapture() }
        viewModel.isTrackingStarted.observe(viewLifecycleOwner) { started ->
            binding.lottieStatus.visibility = if (started) View.VISIBLE else View.GONE
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {
        binding.ivYogaSample.singleClick {
            val imageUrl = args.yogaPoseItem.photo_url
            if (!imageUrl.isNullOrEmpty()) {
                DialogZoom.newInstance(imageUrl)
                    .show(parentFragmentManager, "DialogZoom")
            }
        }
        binding.ivHelp.singleClick {
            startYogaTutorial()

        }
        binding.ivBack.singleClick { popBackStack() }
        binding.ivPhoto.singleClick { viewModel.toggleCaptureWait() }
        binding.progressAround.apply {
            progressMax = 100f
            progress = 0f
        }

        binding.ivVoice.singleClick {
            isMuted = !isMuted
            if (isMuted) {
                binding.ivVoice.setImageResource(com.hadat.aiyoga.R.drawable.ic_mute)
                tts?.stop()
            } else {
                binding.ivVoice.setImageResource(com.hadat.aiyoga.R.drawable.ic_volume)
            }
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
                android.view.MotionEvent.ACTION_DOWN -> { progressAnimator?.start(); true }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    progressAnimator?.cancel()
                    binding.progressAround.progress = 0f
                    true
                }
                else -> false
            }
        }

        binding.ivFlip.singleClick {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            bindCameraUseCases()
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
        val initContext = context ?: return
        tts = TextToSpeech(initContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val safeContext = context ?: return@TextToSpeech
                val langCode = AppPreferences.getLanguageCode(safeContext)
                val locale = if (langCode == "vi") Locale("vi", "VN") else Locale.ENGLISH
                tts?.setLanguage(locale)
            }
        }
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
    private fun navigateToResult() {
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        val date = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val result = WorkoutResultModel(
            userId = userId,
            poseId = args.yogaPoseItem.id,
            poseUrl = args.yogaPoseItem.photo_url,
            poseName = args.yogaPoseItem.name,
            durationInSeconds = viewModel.getTotalTimeStudied(),
            date = date,
            errorCount = viewModel.getErrorCount(),
            workoutTimestamp = System.currentTimeMillis()
        )
        val imagesList = viewModel.getCapturedImages()

        val imagesArray: Array<String> = imagesList.toTypedArray()

        val bundle = Bundle().apply {
            putParcelableArray("workout_result_list", arrayOf(result))
            putStringArray("captured_images_list", imagesArray)
        }

        navigate(com.hadat.aiyoga.R.id.resultFragment, bundle)
    }
    private fun startYogaTutorial() {
        val targets = ArrayList<SpotlightTarget>()

        targets.add(createYogaTarget(
            binding.cardPreview,
            getString(com.hadat.aiyoga.R.string.target_pose_sample_title),
            getString(com.hadat.aiyoga.R.string.target_pose_sample_desc),
            true
        ))
        targets.add(createYogaTarget(
            binding.ivPhoto,
            getString(com.hadat.aiyoga.R.string.target_capture_title),
            getString(com.hadat.aiyoga.R.string.target_capture_desc),
            false
        ))
        targets.add(createYogaTarget(
            binding.ivVoice,
            getString(com.hadat.aiyoga.R.string.target_voice_title),
            getString(com.hadat.aiyoga.R.string.target_voice_desc),
            false
        ))
        targets.add(createYogaTarget(
            binding.progressAround,
            getString(com.hadat.aiyoga.R.string.target_progress_title),
            getString(com.hadat.aiyoga.R.string.target_progress_desc),
            false
        ))

        spotlight = Spotlight.Builder(requireActivity())
            .setTargets(*targets.toTypedArray())
            .setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
            .setDuration(400L)
            .setAnimation(DecelerateInterpolator())
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() {}
                override fun onEnded() {
                    showLegendDialog()
                }
            })
            .build()

        spotlight.start()
    }

    private fun showLegendDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(com.hadat.aiyoga.R.layout.layout_legend, null)

        val alertDialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<android.view.View>(com.hadat.aiyoga.R.id.btn_done_legend)?.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    private fun createYogaTarget(
        targetView: View,
        title: String,
        description: String,
        isRectangle: Boolean = false
    ): SpotlightTarget {
        val overlayView = LayoutInflater.from(requireContext()).inflate(com.hadat.aiyoga.R.layout.layout_help, null)
        val contentContainer = overlayView.findViewById<View>(com.hadat.aiyoga.R.id.content_container)
        val tvTitle = overlayView.findViewById<android.widget.TextView>(com.hadat.aiyoga.R.id.tv_title)
        val tvDesc = overlayView.findViewById<android.widget.TextView>(com.hadat.aiyoga.R.id.tv_desc)
        val btnNext = overlayView.findViewById<android.view.View>(com.hadat.aiyoga.R.id.btn_next_step)
        val btnClose = overlayView.findViewById<android.view.View>(com.hadat.aiyoga.R.id.btn_close)

        tvTitle.text = title
        tvDesc.text = description

        btnNext.setOnClickListener { if (::spotlight.isInitialized) spotlight.next() }
        btnClose.setOnClickListener { if (::spotlight.isInitialized) spotlight.finish() }

        contentContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                contentContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val location = IntArray(2)
                targetView.getLocationOnScreen(location)
                val screenW = resources.displayMetrics.widthPixels
                val screenH = resources.displayMetrics.heightPixels

                var finalX = location[0].toFloat() + (targetView.width / 2) - (contentContainer.width / 2)
                if (finalX < 40) finalX = 40f
                if (finalX + contentContainer.width > screenW - 40) finalX = (screenW - contentContainer.width - 40).toFloat()

                var finalY = location[1].toFloat() + targetView.height + 60f
                if (finalY + contentContainer.height > screenH - 100) finalY = location[1].toFloat() - contentContainer.height - 60f

                contentContainer.x = finalX
                contentContainer.y = finalY
            }
        })

        val shape = if (isRectangle) {
            RoundedRectangle((targetView.height + 30).toFloat(), (targetView.width + 30).toFloat(), 24f)
        } else {
            Circle((kotlin.math.max(targetView.width, targetView.height) / 1.2f) + 20f)
        }

        return SpotlightTarget.Builder()
            .setAnchor(targetView)
            .setShape(shape)
            .setOverlay(overlayView)
            .build()
    }
}