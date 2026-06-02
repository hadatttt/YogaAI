package com.hadat.aiyoga.choosemode

import android.os.Bundle
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentChooseModeBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ChooseModeFragment : BaseFragment<FragmentChooseModeBinding, ChooseModeViewModel>() {

    private val args by navArgs<ChooseModeFragmentArgs>()

    override fun initView() = Unit

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.cardNormal.singleClick { navigateByMode(isAiMode = false) }
        binding.cardAi.singleClick { navigateByMode(isAiMode = true) }
    }

    override fun initData() = Unit

    private fun navigateByMode(isAiMode: Boolean) {
        args.yogaPoseItem?.let { pose ->
            val bundle = Bundle().apply { putParcelable("yogaPoseItem", pose) }
            val destination = if (isAiMode) R.id.singleYogaFragment else R.id.singleNormalYogaFragment
            if (isAiMode) {
                navigateToAiDestination(destination, bundle)
            } else {
                navigate(destination, bundle, isPop = true)
            }
            return
        }

        args.detailSequence?.let { sequence ->
            val bundle = Bundle().apply { putParcelable("detail_sequence", sequence) }
            val destination = if (isAiMode) R.id.multiModeYogaFragment else R.id.multiNormalYogaFragment
            if (isAiMode) {
                navigateToAiDestination(destination, bundle)
            } else {
                navigate(destination, bundle, isPop = true)
            }
        }
    }

    private fun navigateToAiDestination(destination: Int, targetArgs: Bundle) {
        if (AppPreferences.hasSeenAiCameraGuide(requireContext())) {
            navigate(destination, targetArgs, isPop = true)
            return
        }

        val guideArgs = Bundle(targetArgs).apply {
            putInt("target_destination", destination)
        }
        navigate(R.id.aiCameraGuideFragment, guideArgs, isPop = true)
    }
}
