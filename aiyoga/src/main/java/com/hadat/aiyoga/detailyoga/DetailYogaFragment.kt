package com.hadat.aiyoga.detailyoga

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.databinding.FragmentDetailYogaBinding
import com.hadat.aiyoga.utils.yogautils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.view.ViewUtils
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.RoundedRectangle
import com.takusemba.spotlight.Target as SpotlightTarget
import android.view.animation.DecelerateInterpolator
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yalantis.ucrop.UCrop

class DetailYogaFragment : BaseFragment<FragmentDetailYogaBinding, DetailYogaViewModel>(), PoseLandmarkerHelper.LandmarkerListener {
    private lateinit var spotlight: Spotlight
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private val args by navArgs<DetailYogaFragmentArgs>()

    private val categoryAdapter by lazy { CategoryDetailAdapter() }
    private val descriptionAdapter by lazy { DescriptionAdapter() }
    private val benefitAdapter by lazy { BenefitAdapter() }
    private val stepAdapter by lazy { StepAdapter() }
    private val contraAdapter by lazy { ContraindicationAdapter() }
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                analyzeAndDrawSkeleton(it)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(result.data!!)
            error?.printStackTrace()
        }
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { startCrop(it) }
    }

    override fun initView() {
        setupStaticUI()
        setupRecyclerViews()
        setupObservers()
        initPoseLandmarker()
    }
    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(
            java.io.File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .getIntent(requireContext())

        cropImageLauncher.launch(intent)
    }
    private fun setupStaticUI() {
        val item = YogaDataUtils.getPoseById(args.yogaPoseItem.id) ?: args.yogaPoseItem
        binding.apply {
            tvPoseName.text = item.name
            tvSanskritName.text = item.name
            tvLevelValue.setText(
                when (item.expertise_level) {
                    1 -> R.string.beginner
                    2 -> R.string.intermediate
                    3 -> R.string.advanced
                    else -> R.string.beginner
                }
            )
            imgYogaPose.loadImageFromNetwork(item.getDisplayPhoto())
        }
    }

    private fun setupRecyclerViews() {
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
        binding.rvYogaContent.apply {
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }

        categoryAdapter.setList(listOf(
            CategoryDetailModel(0, R.string.cat_description, R.drawable.ic_help),
            CategoryDetailModel(1, R.string.cat_benefits, R.drawable.ic_benefit),
            CategoryDetailModel(2, R.string.cat_steps, R.drawable.ic_practice),
            CategoryDetailModel(3, R.string.cat_caution, R.drawable.ic_warning)
        ))
    }

    private fun setupObservers() {
        viewModel.yogaDetail.observe(viewLifecycleOwner) { detail ->
            detail?.let { updateContentUI(it, viewModel.selectedCategoryId.value ?: 0) }
        }

        viewModel.selectedCategoryId.observe(viewLifecycleOwner) { id ->
            categoryAdapter.setSelectedPosition(id)
            viewModel.yogaDetail.value?.let { updateContentUI(it, id) }
        }

        viewModel.isFavorite.observe(viewLifecycleOwner) { favorite ->
            binding.ivFavorite.setImageResource(if (favorite) R.drawable.ic_heart else R.drawable.ic_un_heart)
        }

        viewModel.customPhotoPath.observe(viewLifecycleOwner) { path ->
            args.yogaPoseItem.user_photo_url = path
            binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())
        }
    }

    private fun updateContentUI(detail: YogaPoseDetailModel, id: Int) {
        binding.rvYogaContent.adapter = when (id) {
            0 -> descriptionAdapter.apply {
                setList(listOf(detail.description))
            }

            1 -> benefitAdapter.apply {
                setList(detail.benefits)
            }

            2 -> stepAdapter.apply {
                setList(detail.steps)
            }

            3 -> contraAdapter.apply {
                setList(detail.contraindications)
            }

            else -> descriptionAdapter
        }

        binding.rvYogaContent.scrollToPosition(0)
    }

    private fun initPoseLandmarker() {
        lifecycleScope.launch(Dispatchers.Default) {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.IMAGE,
                currentDelegate = PoseLandmarkerHelper.DELEGATE_GPU,
                poseLandmarkerHelperListener = this@DetailYogaFragment
            )
        }
    }

    override fun initData() {
        setupStaticUI()
        val poseId = args.yogaPoseItem.id
        viewModel.checkFavoriteStatus(requireContext(), poseId)
        viewModel.fetchYogaDetail( poseId)
    }

    override fun initListener() {
        binding.ivHelp.singleClick {
            startYogaTutorial()
        }
        binding.ivBack.singleClick { popBackStack() }
        binding.ivFavorite.singleClick { viewModel.toggleFavorite(requireContext(), args.yogaPoseItem) }
        binding.ivCameraCapture.singleClick {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        categoryAdapter.setOnClickItemRecyclerView { category, position ->
            viewModel.selectCategory(category.id)
            ViewUtils.scrollToCenter(binding.rvCategory, position)
        }
        binding.layoutNext.singleClick {
            val bundle = Bundle().apply { putParcelable("yogaPoseItem", args.yogaPoseItem) }
            navigate(R.id.chooseModeFragment, bundle)
        }
    }

    private fun analyzeAndDrawSkeleton(uri: Uri) {
        binding.apply {
            imgYogaPose.setImageURI(uri)
            overlayResult.clear()
            lottieAi.visibility = View.VISIBLE
            tvAiLoading.visibility = View.VISIBLE
            tvAiLoading.text = "AI is analyzing..."
        }

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val bitmap = loadBitmapFromUri(uri)
                val resultBundle = poseLandmarkerHelper?.detectImage(bitmap)

                withContext(Dispatchers.Main) {
                    handleAiResult(resultBundle, bitmap, uri)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { resetAiLoading("Error processing image") }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
        }.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun handleAiResult(resultBundle: PoseLandmarkerHelper.ResultBundle?, bitmap: Bitmap, uri: Uri) {
        if (_binding == null || resultBundle == null || resultBundle.results[0].landmarks().isEmpty()) {
            resetAiLoading("No human detected!")
            return
        }

        val results = resultBundle.results[0]
        binding.overlayResult.setResults(results, bitmap.height, bitmap.width, RunningMode.IMAGE)

        val isCorrect = YogaCoachUtils.checkPoseAccuracy(results.landmarks()[0], args.yogaPoseItem.id)
        if (isCorrect) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            viewModel.updateCustomPhoto(requireContext(), args.yogaPoseItem.id, uri.toString())
            binding.tvAiLoading.text = "Correct Pose Image"
        } else {
            binding.tvAiLoading.text = "Incorrect Pose!"
        }

        binding.root.postDelayed({
            if (_binding == null) return@postDelayed
            binding.lottieAi.visibility = View.GONE
            binding.tvAiLoading.visibility = View.GONE
            binding.overlayResult.clear()
            if (!isCorrect) binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())
        }, 3000)
    }

    private fun resetAiLoading(message: String) {
        if (!isAdded) return

        binding.tvAiLoading.text = message
        binding.lottieAi.visibility = View.GONE
        binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())

        binding.root.postDelayed({
            if (isAdded) {
                binding.tvAiLoading.visibility = View.GONE
            }
        }, 1500)
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {}
    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread { resetAiLoading(error) }
    }

    override fun onDestroyView() {
        poseLandmarkerHelper?.clearPoseLandmarker()
        poseLandmarkerHelper = null
        super.onDestroyView()
    }
    private fun startYogaTutorial() {
        val targets = ArrayList<SpotlightTarget>()

        targets.add(createYogaTarget(
            binding.ivCameraCapture,
            getString(R.string.target_ai_replace_title),
            getString(R.string.target_ai_replace_desc),
            false
        ))

        spotlight = Spotlight.Builder(requireActivity())
            .setTargets(*targets.toTypedArray())
            .setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
            .setDuration(400L)
            .setAnimation(DecelerateInterpolator())
            .build()

        spotlight.start()
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

        btnNext.setOnClickListener { if (::spotlight.isInitialized) spotlight.finish() }
        btnClose.setOnClickListener { if (::spotlight.isInitialized) spotlight.finish() }

        contentContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                contentContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val location = IntArray(2)
                targetView.getLocationOnScreen(location)

                var finalX = location[0].toFloat() + (targetView.width / 2) - (contentContainer.width / 2)
                if (finalX < 40) finalX = 40f
                if (finalX + contentContainer.width > resources.displayMetrics.widthPixels - 40) {
                    finalX = (resources.displayMetrics.widthPixels - contentContainer.width - 40).toFloat()
                }

                var finalY = location[1].toFloat() + targetView.height + 60f
                if (finalY + contentContainer.height > resources.displayMetrics.heightPixels - 100) {
                    finalY = location[1].toFloat() - contentContainer.height - 60f
                }

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
