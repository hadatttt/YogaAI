package com.hadat.aiyoga.result

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.firestore.model.HealthProfileModel
import com.hadat.aiyoga.databinding.FragmentResultBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import com.hadat.aiyoga.utils.yogautils.HealthCalculatorUtils
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ResultFragment : BaseFragment<FragmentResultBinding, ResultViewModel>() {

    private val args by navArgs<ResultFragmentArgs>()
    private val historyAdapter by lazy { WorkoutHistoryAdapter { } }
    private val capturedAdapter by lazy { CapturedImagesAdapter(selectable = false) }

    private var healthProfile: HealthProfileModel? = null


    override fun initView() {
        binding.rvWorkoutHistory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = historyAdapter
        }
        binding.rvCapturedImages.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = capturedAdapter
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack(R.id.homeFragment) }
        binding.btnSharePlace.singleClick {
            val captured = args.capturedImagesList?.toList().orEmpty().filter { it.isNotBlank() }

            if (captured.isEmpty()) {
                showToast(getString(R.string.no_image_to_share))
                return@singleClick
            }
            val bundle = Bundle().apply {
                putStringArray("captured_images_list", captured.toTypedArray())
            }
            navigate(R.id.sharePlaceFragment, bundle)
        }
    }

    override fun initData() {
        val resultList = args.workoutResultList?.toList().orEmpty()
        if (resultList.isNotEmpty()) {
            historyAdapter.setList(resultList)
        }
        val capturedImages = args.capturedImagesList?.toList().orEmpty()
        if (capturedImages.isNotEmpty()) {
            capturedAdapter.setList(capturedImages)
        }

        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"

        if (resultList.isNotEmpty()) {
            viewModel.saveWorkoutResults(resultList, userId)
        }

        viewModel.loadHealthProfile(userId)
        viewModel.fetchWorkoutHistory(userId)
        viewModel.loadCurrentUser(userId)

        viewModel.healthProfile.observe(viewLifecycleOwner) { profile ->
            healthProfile = profile

            if (resultList.isNotEmpty()) {
                bindWorkoutSummary(resultList)
            }
        }

        viewModel.workoutHistory.observe(viewLifecycleOwner) {
            if (resultList.isEmpty()) {
                historyAdapter.setList(it)
            }
        }

        viewModel.shareStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            showToast(
                if (ok) getString(R.string.shared_to_map_successfully)
                else getString(R.string.share_failed)
            )
            viewModel.resetShareStatus()
        }
    }

    private fun bindWorkoutSummary(list: List<WorkoutResultModel>) {

        val animationDuration = 1500L

        val totalSeconds = list.sumOf { it.durationInSeconds }
        val totalError = list.sumOf { it.errorCount }

        val weight = healthProfile?.weight ?: 60f

        HealthCalculatorUtils.calculateTotalCalories(
            workouts = list,
            weight = weight,
            onMet = { id, callback ->
                YogaDataUtils.getRemoteYogaMet(id, callback)
            }
        ) { totalCalories ->

            binding.progressCalories.apply {
                progressMax = healthProfile?.tdee ?: 2000f
                setProgressWithAnimation(totalCalories, 1500L)
            }

            binding.tvCaloriesValue.text =
                String.format("%.1f", totalCalories)
            binding.tvCaloriesLabel.text =
                "of ${healthProfile?.tdee?.toInt() ?: 2000} kcal "
        }
        val targetSeconds = 60f

        binding.progressTime.apply {
            progressMax = targetSeconds
            setProgressWithAnimation(totalSeconds.toFloat(), animationDuration)
        }

        binding.tvTimeValue.text = "${totalSeconds}s"



        val accuracyPercent = HealthCalculatorUtils.calculateAccuracy(totalSeconds,totalError)

        binding.progressAccuracy.apply {
            progressMax = 100f
            progressBarColor = when {
                accuracyPercent >= 85 -> Color.parseColor("#00C853")
                accuracyPercent >= 60 -> Color.parseColor("#FFD600")
                else -> Color.parseColor("#FF6A00")
            }
            setProgressWithAnimation(accuracyPercent, animationDuration)
        }

        binding.tvAccuracyValue.text = "${accuracyPercent.toInt()}%"
    }




    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}