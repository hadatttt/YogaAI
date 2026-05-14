package com.hadat.aiyoga.choosemode

import android.os.Bundle
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentChooseModeBinding
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
            navigate(if (isAiMode) R.id.singleYogaFragment else R.id.singleNormalYogaFragment, bundle, isPop = true)
            return
        }

        args.detailSequence?.let { sequence ->
            val bundle = Bundle().apply { putParcelable("detail_sequence", sequence) }
            navigate(if (isAiMode) R.id.multiModeYogaFragment else R.id.multiNormalYogaFragment, bundle, isPop = true)
        }
    }
}
