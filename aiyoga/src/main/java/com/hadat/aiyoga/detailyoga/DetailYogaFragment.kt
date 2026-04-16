package com.hadat.aiyoga.detailyoga

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentDetailYogaBinding
import com.hadat.aiyoga.utils.PoseLandmarkerHelper
import com.hadat.aiyoga.utils.ViewUtils
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.hadat.aiyoga.utils.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.concurrent.Executors
import kotlin.getValue

class DetailYogaFragment : BaseFragment<FragmentDetailYogaBinding, DetailYogaViewModel>(),PoseLandmarkerHelper.LandmarkerListener {
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            analyzeAndDrawSkeleton(it)
        }
    }
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val categoryAdapter by lazy {
        CategoryDetailAdapter()
    }
    private val args by navArgs<DetailYogaFragmentArgs>()
    private val benefitAdapter by lazy { BenefitAdapter() }
    private val stepAdapter by lazy { StepAdapter() }
    private val contraAdapter by lazy { ContraindicationAdapter() }
    private val descriptionAdapter by lazy { DescriptionAdapter() }
    private var yogaDetail: YogaPoseDetailModel? = null

    override fun initView() {
        poseLandmarkerHelper = PoseLandmarkerHelper(
            context = requireContext(),
            runningMode = RunningMode.IMAGE,
            currentDelegate = PoseLandmarkerHelper.DELEGATE_GPU,
            poseLandmarkerHelperListener = this
        )
        viewModel.isFavorite.observe(viewLifecycleOwner) { favorite ->
            binding.ivFavorite.setImageResource(
                if (favorite) R.drawable.ic_heart else R.drawable.ic_un_heart
            )
        }
        viewModel.customPhotoPath.observe(viewLifecycleOwner) { path ->
            args.yogaPoseItem.user_photo_url = path
            binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())
        }
         val menuDetail = listOf(
            CategoryDetailModel(0, "Description", R.drawable.ic_benefit),
             CategoryDetailModel(2, "Steps", R.drawable.ic_practice),
            CategoryDetailModel(1, "Benefits", R.drawable.ic_benefit),
            CategoryDetailModel(3, "Caution", R.drawable.ic_warning)
        )
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        binding.rvYogaContent.apply {
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }

        categoryAdapter.setList(menuDetail)
        val yogaPoseItem = args.yogaPoseItem
        binding.apply {
            tvPoseName.text = yogaPoseItem.name
            tvSanskritName.text = yogaPoseItem.name
            val levelText = when (yogaPoseItem.expertise_level) {
                1 -> "Beginner"
                2 -> "Intermediate"
                3 -> "Advanced"
                else -> "Unknown"
            }
            tvLevelValue.text = levelText
        }
        YogaDataUtils.getRemoteYogaDetail(yogaPoseItem.id) { detail ->
            detail?.let {
                yogaDetail = it
                updateContentByCategoryId(0)
            }
        }
    }

    override fun initData() {
        viewModel.checkFavoriteStatus(requireContext(), args.yogaPoseItem.id)
    }

    private fun updateContentByCategoryId(id: Int) {
        val detail = yogaDetail ?: return

        when (id) {
            0 -> {
                binding.rvYogaContent.adapter = descriptionAdapter
                descriptionAdapter.setList(listOf(detail.description))
            }
            1 -> {
                binding.rvYogaContent.adapter = benefitAdapter
                benefitAdapter.setList(detail.benefits)
            }
            2 -> {
                binding.rvYogaContent.adapter = stepAdapter
                stepAdapter.setList(detail.steps)
            }
            3 -> {
                binding.rvYogaContent.adapter = contraAdapter
                contraAdapter.setList(detail.contraindications)
            }
        }

        binding.rvYogaContent.scrollToPosition(0)
    }

    override fun initListener() {
        binding.ivBack.singleClick {
            popBackStack()
        }
        binding.ivCameraCapture.singleClick {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.ivFavorite.singleClick {
            viewModel.toggleFavorite(requireContext(), args.yogaPoseItem)
        }
        categoryAdapter.setOnClickItemRecyclerView { category, position ->
            categoryAdapter.setSelectedPosition(position)
            ViewUtils.scrollToCenter(binding.rvCategory, position)
            updateContentByCategoryId(category.id)
        }
        binding.layoutNext.singleClick {
            val bundle = Bundle().apply {
                putParcelable("yogaPoseItem", args.yogaPoseItem)
            }
            navigate(R.id.singleYogaFragment, bundle)
        }
    }
    private fun analyzeAndDrawSkeleton(uri: android.net.Uri) {

        binding.imgYogaPose.setImageURI(uri)

        binding.overlayResult.clear()
        binding.lottieAi.visibility = View.VISIBLE
        binding.tvAiLoading.visibility = View.VISIBLE
        binding.tvAiLoading.text = "AI is analyzing..."
        backgroundExecutor.execute {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                }.copy(Bitmap.Config.ARGB_8888, true)

                val resultBundle = poseLandmarkerHelper.detectImage(bitmap)

                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    if (resultBundle != null && resultBundle.results[0].landmarks().isNotEmpty()) {
                        val results = resultBundle.results[0]

                        binding.overlayResult.setResults(
                            results,
                            bitmap.height,
                            bitmap.width,
                            RunningMode.IMAGE
                        )
                        binding.overlayResult.invalidate()

                        val isCorrect = YogaCoachUtils.checkPoseAccuracy(results.landmarks()[0], args.yogaPoseItem.id)

                        if (isCorrect) {
                            try {
                                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                            } catch (e: Exception) { e.printStackTrace() }

                            viewModel.updateCustomPhoto(requireContext(), args.yogaPoseItem.id, uri.toString())
                            binding.tvAiLoading.text = "Correct Pose Image"
                        } else {
                            binding.tvAiLoading.text = "Incorrect Pose! Checking skeleton..."
                        }

                        binding.root.postDelayed({
                            binding.lottieAi.visibility = View.GONE
                            binding.tvAiLoading.visibility = View.GONE
                            binding.overlayResult.clear()
                            if (!isCorrect) {
                                binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())
                            }
                        }, 5000)

                    } else {
                        binding.lottieAi.visibility = View.GONE
                        binding.tvAiLoading.text = "No human detected!"
                        binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())
                        binding.root.postDelayed({ binding.tvAiLoading.visibility = View.GONE }, 1500)
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    binding.lottieAi.visibility = View.GONE
                    binding.tvAiLoading.text = "Error processing image"
                    binding.imgYogaPose.loadImageFromNetwork(args.yogaPoseItem.getDisplayPhoto())
                }
            }
        }
    }
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {}
    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            binding.lottieAi.visibility = View.GONE
            binding.tvAiLoading.text = error
        }
    }
    override fun onDestroyView() {
        binding.overlayResult.clear()

        if (::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.clearPoseLandmarker()
        }

        backgroundExecutor.shutdownNow()

        binding.rvCategory.adapter = null
        binding.rvYogaContent.adapter = null

        super.onDestroyView()
    }
}