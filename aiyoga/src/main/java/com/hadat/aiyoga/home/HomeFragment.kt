package com.hadat.aiyoga.home

import android.os.Bundle
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentHomeBinding
import com.hadat.aiyoga.utils.ViewUtils.getGreeting
import com.hadat.aiyoga.utils.ViewUtils.removeVietnameseAccents
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override fun initView() {
        binding.tvGreeting.text = getGreeting(requireContext())

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvUsername.text = it.displayName.removeVietnameseAccents()
                binding.imgAvatar.loadImageFromNetwork(it.photoUrl)
            }
        }

        viewModel.todayPickPose.observe(viewLifecycleOwner) { pose ->
            pose?.let {
                binding.tvPoseName.text = it.name
                binding.poseToday.loadImageFromNetwork(it.photo_url)
            }
        }
    }

    override fun initListener() {
        binding.cvTodayPick.singleClick {
            viewModel.todayPickPose.value?.let { pose ->
                val bundle = Bundle().apply { putParcelable("yogaPoseItem", pose) }
                navigate(R.id.detailYogaFragment, bundle)
            }
        }

        binding.cvBrowsePoses.singleClick {
            navigate(R.id.singleModeFragment)
        }

        binding.cvYoai.singleClick {
            navigate(R.id.choosePoseFragment)
        }

        binding.llChatbotWrapper.setDraggableWithClick {
        }
    }

    override fun initData() {
        viewModel.fetchData()
    }
}