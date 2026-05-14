package com.hadat.aiyoga.single_normalyoga

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentSingleNormalYogaBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.view.loadYogaPoseGithubImage
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SingleNormalYogaFragment : BaseFragment<FragmentSingleNormalYogaBinding, SingleNormalYogaViewModel>() {

    private val args by navArgs<SingleNormalYogaFragmentArgs>()
    private var progressAnimator: ObjectAnimator? = null
    private var downTime = 0L

    override fun initView() {
        binding.progressAround.apply {
            progressMax = 100f
            progress = 0f
        }
        setupProgressAnimator()
    }

    override fun initData() {
        viewModel.resetTracking()
        val pose = args.yogaPoseItem
        binding.tvPoseNameTop.text = pose.name
        if (pose.id in 0..81) {
            binding.ivYogaBackground.loadYogaPoseGithubImage(pose.id)
        }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }
        viewModel.isTrackingStarted.observe(viewLifecycleOwner) { started ->
            binding.lottieStatus.visibility = if (started) View.VISIBLE else View.GONE
            binding.tvGuide.text = getString(if (started) R.string.hold_to_finish else R.string.tap_to_start)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        binding.layoutAction.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.currentTimeMillis()
                    if (viewModel.isTrackingStarted.value == true) {
                        progressAnimator?.start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (viewModel.isTrackingStarted.value == true) {
                        progressAnimator?.cancel()
                        binding.progressAround.progress = 0f
                    } else if (System.currentTimeMillis() - downTime < 300) {
                        viewModel.startTracking()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    progressAnimator?.cancel()
                    binding.progressAround.progress = 0f
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        viewModel.stopTracking()
        progressAnimator?.cancel()
        progressAnimator = null
        super.onDestroyView()
    }

    private fun setupProgressAnimator() {
        progressAnimator = ObjectAnimator.ofFloat(binding.progressAround, "progress", 0f, 100f).apply {
            duration = 1500
            interpolator = android.view.animation.LinearInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (binding.progressAround.progress >= 100f) showPoseDoneAnimation()
                }
            })
        }
    }

    private fun showPoseDoneAnimation() {
        binding.lottiePoseDone.apply {
            visibility = View.VISIBLE
            playAnimation()
            addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) = Unit
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = View.GONE
                    removeAllAnimatorListeners()
                    navigateToResult()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) = Unit
                override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
            })
        }
    }

    private fun navigateToResult() {
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        val pose = args.yogaPoseItem
        val result = WorkoutResultModel(
            userId = userId,
            poseId = pose.id,
            poseUrl = pose.photo_url,
            poseName = pose.name,
            durationInSeconds = viewModel.getTotalTimeStudied(),
            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            errorCount = 0,
            workoutTimestamp = System.currentTimeMillis(),
            isAiMode = false
        )

        val bundle = Bundle().apply {
            putParcelableArray("workout_result_list", arrayOf(result))
            putStringArray("captured_images_list", emptyArray())
        }
        navigate(R.id.resultFragment, bundle, isPop = true)
    }
}
