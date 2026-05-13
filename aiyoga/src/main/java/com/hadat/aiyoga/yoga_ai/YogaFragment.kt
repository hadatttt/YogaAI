package com.hadat.aiyoga.yoga_ai

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.databinding.FragmentSingleYogaBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import com.hadat.aiyoga.utils.yogautils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.takusemba.spotlight.OnSpotlightListener
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.RoundedRectangle
import com.takusemba.spotlight.Target as SpotlightTarget
import android.view.ViewTreeObserver
import com.hadat.aiyoga.manager_ai.AIManager

class YogaFragment : BaseFragment<FragmentSingleYogaBinding, YogaViewModel>(),
    PoseLandmarkerHelper.LandmarkerListener {
    private lateinit var spotlight: Spotlight
    private var cameraProvider: ProcessCameraProvider? = null
    private var lastSpeakTime = 0L
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
    private val STABLE_THRESHOLD = 7

    override fun initView() {
        backgroundExecutor = Executors.newSingleThreadExecutor()
        initTextToSpeech()
        initializeAiResources()
    }

    private fun initializeAiResources() {
        binding.loadingView.root.visibility = View.VISIBLE

        poseLandmarkerHelper = AIManager.getLandmarker()
        classifierInterpreter = AIManager.getClassifier()
        AIManager.setListener(this)

        val isSuccess = YogaCoachUtils.loadReferenceData()

        if (isSuccess && isAdded) {
            viewModel.fetchYogaPoses()
            binding.loadingView.root.visibility = View.GONE
        } else if (isAdded) {
            binding.loadingView.root.visibility = View.GONE
        }
    }
    override fun onResume() {
        super.onResume()
        checkAndStartCamera()
        val landmarker = AIManager.getLandmarker()
        if (landmarker != null) {
            AIManager.setListener(this)
            if (landmarker.isClose()) {
                backgroundExecutor.execute {
                    landmarker.setupPoseLandmarker()
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
        }
        cameraProvider = null
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        val result = resultBundle.results.firstOrNull() ?: return

        activity?.runOnUiThread {
            if (view == null) return@runOnUiThread
            binding.overlayView.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
        }

        backgroundExecutor.execute {
            val currentPoseId = viewModel.previewPoseId.value ?: -1
            val bones = if (currentPoseId != -1) {
                YogaCoachUtils.getBoneErrors(currentPoseId, result)
            } else emptyMap()
            viewModel.processCoachLogic(requireContext(), result)
            activity?.runOnUiThread {
                if (view != null) {
                    binding.overlayView.setWrongBones(bones)
                    binding.overlayView.invalidate()
                }
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
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
            frameCounter++
            if (frameCounter % 5 == 0 && classifierInterpreter != null) {
                val bitmap = imageProxy.toBitmap()
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                classifierExecutor.execute {
                    runPoseClassifier(bitmap, rotationDegrees)
                }
            }
            val currentLandmarker = AIManager.getLandmarker()
            if (currentLandmarker != null && !currentLandmarker.isClose()) {
                currentLandmarker.detectLiveStream(
                    imageProxy,
                    lensFacing == CameraSelector.LENS_FACING_FRONT
                )
            } else {
                imageProxy.close()
            }
        } catch (e: Exception) {
            imageProxy.close()
        }
    }
    private fun runPoseClassifier(bitmap: Bitmap, rotationDegrees: Int) {
        try {
            val interpreter = classifierInterpreter ?: return

            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                }
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val scaled = Bitmap.createScaledBitmap(rotatedBitmap, 224, 224, true)

            val buffer = prepareByteBuffer(scaled)
            val out82 = Array(1) { FloatArray(82) }
            val outputs: MutableMap<Int, Any> = mutableMapOf(2 to out82)

            synchronized(tfliteLock) {
                try {
                    interpreter.runForMultipleInputsOutputs(arrayOf(buffer), outputs)
                } catch (e: Exception) {
                }
            }

            val probabilities = out82[0]
            val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIdx]

            if (confidence > 0.7f) {
                if (maxIdx == lastPendingPoseId) {
                    poseCounter++
                } else {
                    lastPendingPoseId = maxIdx
                    poseCounter = 0
                }

                if (poseCounter >= STABLE_THRESHOLD) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null && view != null) {
                            viewModel.handlePoseInference(requireContext(), maxIdx)
                        } else {
                            return@runOnUiThread
                        }
                    }
                }
            }

            bitmap.recycle()
            rotatedBitmap.recycle()
            scaled.recycle()

        } catch (e: Exception) {
        }
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
                binding.tvGuide.text = getString(com.hadat.aiyoga.R.string.guide_hold_to_capture)
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

        viewModel.speakCommand.observe(viewLifecycleOwner) { speak(it) }
        viewModel.previewPoseId.observe(viewLifecycleOwner) { id ->
            val poseList = viewModel.yogaPoseDataList.value
            val currentPose = poseList?.find { it.id == id }

            if (id != -1 && currentPose != null && !currentPose.photo_url.isNullOrEmpty()) {
                binding.cardPreview.visibility = View.VISIBLE
                binding.ivZoomSample.visibility = View.VISIBLE
                binding.tvYogaName.visibility = View.VISIBLE

                binding.ivYogaSample.loadImageFromNetwork(currentPose.photo_url)
                binding.tvYogaName.text = currentPose.name
            } else {
                binding.cardPreview.visibility = View.GONE
                binding.ivZoomSample.visibility = View.GONE
                binding.tvYogaName.text = ""
            }
        }
    }

    override fun initListener() {
        binding.ivHelp.singleClick {
            startYogaTutorial()
        }
        binding.ivYogaSample.singleClick {
            val poseId = viewModel.previewPoseId.value
            val poseList = viewModel.yogaPoseDataList.value
            val currentPose = poseList?.find { it.id == poseId }

            currentPose?.photo_url?.let { imageUrl ->
                DialogZoom.newInstance(imageUrl)
                    .show(parentFragmentManager, "DialogZoom")
            }
        }
        binding.ivPhoto.singleClick {
            viewModel.toggleCaptureWait()
        }
        binding.ivBack.singleClick {
            viewModel.resetData()
            popBackStack()
        }
        onBackPressed {
            viewModel.resetData()
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
                        showPoseDoneAnimation()
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

    private fun checkAndStartCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startCamera() }

    override fun onDestroyView() {
        viewModel.stopExerciseTimer()
        classifierExecutor.shutdownNow()
        backgroundExecutor.shutdownNow()
        AIManager.getLandmarker()?.poseLandmarkerHelperListener = null
        poseLandmarkerHelper = null
        classifierInterpreter = null
        tts?.stop()
        tts?.shutdown()
        super.onDestroyView()
    }
    private fun navigateToResult() {
        val results = viewModel.getFinalWorkoutResults()

        val capturedImagesArray = viewModel.getCapturedImages().toTypedArray()

        if (results.isEmpty()) {
            Toast.makeText(requireContext(), getString(com.hadat.aiyoga.R.string.no_image_to_share), Toast.LENGTH_SHORT).show()
            popBackStack()
            return
        }

        val bundle = Bundle().apply {
            putParcelableArray("workout_result_list", results)
            putStringArray("captured_images_list", capturedImagesArray)
        }

        navigate(com.hadat.aiyoga.R.id.resultFragment, bundle, isPop = true)
        viewModel.resetData()
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
    private fun speak(text: String) {
        if (text.isEmpty() || isMuted) return
        val now = System.currentTimeMillis()
        if (now - lastSpeakTime < 2500) return
        lastSpeakTime = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun triggerFlashEffect() {
        binding.viewFlash.visibility = View.VISIBLE
        binding.viewFlash.alpha = 1f
        binding.viewFlash.animate().alpha(0f).setDuration(200)
            .withEndAction { binding.viewFlash.visibility = View.GONE }
            .start()
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
    private fun showPoseDoneAnimation() {
        binding.lottiePoseDone.apply {
            visibility = View.VISIBLE
            playAnimation()

            addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = View.GONE
                    removeAllAnimatorListeners()
                    navigateToResult()
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
        }
    }
}