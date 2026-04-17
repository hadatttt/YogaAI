package com.hadat.aiyoga.detailyoga

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentDetailYogaBinding
import com.hadat.aiyoga.utils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.ViewUtils
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailYogaFragment : BaseFragment<FragmentDetailYogaBinding, DetailYogaViewModel>(), PoseLandmarkerHelper.LandmarkerListener {

    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private val args by navArgs<DetailYogaFragmentArgs>()

    private val categoryAdapter by lazy { CategoryDetailAdapter() }
    private val descriptionAdapter by lazy { DescriptionAdapter() }
    private val benefitAdapter by lazy { BenefitAdapter() }
    private val stepAdapter by lazy { StepAdapter() }
    private val contraAdapter by lazy { ContraindicationAdapter() }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { analyzeAndDrawSkeleton(it) }
    }

    override fun initView() {
        setupStaticUI()
        setupRecyclerViews()
        setupObservers()
        initPoseLandmarker()
    }

    private fun setupStaticUI() {
        val item = args.yogaPoseItem
        binding.apply {
            tvPoseName.text = item.name
            tvSanskritName.text = item.name
            tvLevelValue.text = when (item.expertise_level) {
                1 -> "Beginner"
                2 -> "Intermediate"
                3 -> "Advanced"
                else -> "Unknown"
            }
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
            CategoryDetailModel(0, "Description", R.drawable.ic_help),
            CategoryDetailModel(1, "Benefits", R.drawable.ic_benefit),
            CategoryDetailModel(2, "Step", R.drawable.ic_practice),
            CategoryDetailModel(3, "Caution", R.drawable.ic_warning)
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
        val poseId = args.yogaPoseItem.id
        viewModel.checkFavoriteStatus(requireContext(), poseId)
        viewModel.fetchYogaDetail(poseId)
    }

    override fun initListener() {
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
            navigate(R.id.singleYogaFragment, bundle)
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
        binding.tvAiLoading.text = message
        binding.lottieAi.visibility = View.GONE
        binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())
        binding.root.postDelayed({ binding.tvAiLoading.visibility = View.GONE }, 1500)
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
}