package com.hadat.aiyoga.home

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
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
    private val recentAdapter by lazy {
        RecentSequencesAdapter(
            onAddClick = {
                navigate(R.id.choosePoseFragment)
            },
            onItemClick = { sequence ->
                val bundle = Bundle().apply {
                    putParcelable("detail_sequence", sequence)
                }
                navigate(R.id.detailSequenceFragment,bundle)
            }
        )
    }
    override fun initView() {
        viewModel.fetchData()
        binding.tvGreeting.text = getGreeting(requireContext())
        binding.rvSequences.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }
        viewModel.recentSequences.observe(viewLifecycleOwner) { list ->
            recentAdapter.setList(list)
        }
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
        binding.tvViewAll.singleClick {
            val bundle = Bundle().apply { putInt("initial_tab", 1) }
            navigate(R.id.communityMySequenceFragment, bundle)
        }

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
            navigate(R.id.yogaFragment)
        }
        binding.ivProfileEdit.singleClick {
            navigate(R.id.profileFragment)
        }

        binding.llChatbotWrapper.setDraggableWithClick {
        }
    }

    override fun initData() {
    }
}