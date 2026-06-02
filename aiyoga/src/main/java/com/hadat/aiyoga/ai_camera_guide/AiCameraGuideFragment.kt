package com.hadat.aiyoga.ai_camera_guide

import android.os.Bundle
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.databinding.FragmentAiCameraGuideBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class AiCameraGuideFragment : BaseFragment<FragmentAiCameraGuideBinding, AiCameraGuideViewModel>() {

    private val args by navArgs<AiCameraGuideFragmentArgs>()

    override fun initView() {
        if (AppPreferences.hasSeenAiCameraGuide(requireContext())) {
            navigateToTarget()
        }
    }

    override fun initListener() {
        binding.btnNext.singleClick {
            AppPreferences.setAiCameraGuideShown(requireContext())
            navigateToTarget()
        }
    }

    override fun initData() = Unit

    private fun navigateToTarget() {
        val bundle = Bundle().apply {
            args.yogaPoseItem?.let { putParcelable("yogaPoseItem", it) }
            args.detailSequence?.let { putParcelable("detail_sequence", it) }
        }
        navigate(args.targetDestination, bundle, isPop = true)
    }
}
