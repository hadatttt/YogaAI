package com.hadat.aiyoga.result

import android.graphics.Color
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentResultBinding
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ResultFragment : BaseFragment<FragmentResultBinding, ResultViewModel>() {

    private val args by navArgs<ResultFragmentArgs>()

    override fun initView() {

    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack(R.id.homeFragment) }
    }

    override fun initData() {
        args.workoutResultList?.firstOrNull()?.let { bindWorkoutData(it) }
    }

    private fun bindWorkoutData(data: WorkoutResultModel) {
        val animationDuration = 1500L

        val caloriesBurned = data.durationInSeconds * 0.15f
        val maxCaloriesGoal = 30f
        binding.progressCalories.apply {
            progressMax = maxCaloriesGoal
            setProgressWithAnimation(caloriesBurned, animationDuration)
        }
        binding.tvCaloriesValue.text = String.format("%.1f", caloriesBurned)
        binding.tvCaloriesLabel.text = "of ${maxCaloriesGoal.toInt()} kcal"

        val targetSeconds = 60f
        binding.progressTime.apply {
            progressMax = targetSeconds
            setProgressWithAnimation(data.durationInSeconds.toFloat(), animationDuration)
        }
        binding.tvTimeValue.text = "${data.durationInSeconds}s"

        val accuracyPercent = (100f - (data.errorCount * 5f)).coerceIn(10f, 100f)
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
}