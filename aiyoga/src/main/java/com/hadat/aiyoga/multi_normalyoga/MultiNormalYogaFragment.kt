package com.hadat.aiyoga.multi_normalyoga

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentMultiNormalYogaBinding
import com.hadat.aiyoga.utils.view.loadYogaPoseGithubImage
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class MultiNormalYogaFragment : BaseFragment<FragmentMultiNormalYogaBinding, MultiNormalYogaViewModel>() {

    private val args by navArgs<MultiNormalYogaFragmentArgs>()
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
        args.detailSequence?.let { viewModel.startWorkout(requireContext(), it) }

        viewModel.currentPose.observe(viewLifecycleOwner) { pose ->
            pose ?: return@observe
            binding.tvPoseNameTop.text = pose.name
            if (pose.id in 0..81) {
                binding.ivYogaBackground.loadYogaPoseGithubImage(pose.id)
            }
        }
        viewModel.poseCountText.observe(viewLifecycleOwner) { binding.tvPoseIndex.text = it }
        viewModel.timerText.observe(viewLifecycleOwner) { binding.tvTimer.text = it }
        viewModel.currentGuideText.observe(viewLifecycleOwner) {
            binding.tvGuide.text = if (it.isBlank()) getString(R.string.tap_to_start) else it
        }
        viewModel.isTrackingStarted.observe(viewLifecycleOwner) { started ->
            binding.lottieStatus.visibility = if (started) View.VISIBLE else View.GONE
            binding.tvGuide.text = getString(if (started) R.string.hold_to_finish else R.string.tap_to_start)
        }
        viewModel.sessionCompleted.observe(viewLifecycleOwner) {
            if (it != null) {
                navigateToResult()
                viewModel.clearSessionCompleted()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {
        binding.ivBack.singleClick {
            viewModel.resetData()
            popBackStack()
        }
        binding.btnNextPose.singleClick { viewModel.nextPose(requireContext()) }

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
                        viewModel.startTracking(requireContext())
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
                    if (binding.progressAround.progress >= 100f) {
                        showPoseDoneAnimation { navigateToResult() }
                    }
                }
            })
        }
    }

    private fun showPoseDoneAnimation(onAnimationFinished: () -> Unit) {
        binding.lottiePoseDone.apply {
            visibility = View.VISIBLE
            playAnimation()
            addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) = Unit
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = View.GONE
                    removeAllAnimatorListeners()
                    onAnimationFinished()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) = Unit
                override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
            })
        }
    }

    private fun navigateToResult() {
        val results = viewModel.getFinalWorkoutResults()
        if (results.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.tap_to_start), Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putParcelableArray("workout_result_list", results)
            putStringArray("captured_images_list", emptyArray())
        }
        navigate(R.id.resultFragment, bundle, isPop = true)
    }
}
