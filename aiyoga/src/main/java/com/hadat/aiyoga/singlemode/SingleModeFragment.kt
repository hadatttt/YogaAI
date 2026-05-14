package com.hadat.aiyoga.singlemode

import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentSingleModeBinding
import com.hadat.aiyoga.home.CategoryAdapter
import com.hadat.aiyoga.utils.view.ViewUtils
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.base.adapter.animation.AnimationType
import hoang.dqm.codebase.utils.singleClick

class SingleModeFragment : BaseFragment<FragmentSingleModeBinding, SingleModeViewModel>() {

    private val categoryAdapter by lazy { CategoryAdapter() }
    private val yogaPoseAdapter by lazy { YogaPoseAdapter() }

    override fun initView() {
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        binding.rvTemplates.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = yogaPoseAdapter.apply {
                setItemAnimation(AnimationType.SlideInBottom)
                isAnimationFirstOnly = false
            }
        }

        viewModel.categoryList.observe(viewLifecycleOwner) { categoryAdapter.setList(it) }
        viewModel.yogaPoseList.observe(viewLifecycleOwner) { yogaPoseAdapter.setList(it) }

        viewModel.todayPickPose.observe(viewLifecycleOwner) { pose ->
            pose?.let {
                binding.tvPoseTodayName.text = it.name
                binding.poseToday.loadImageFromNetwork(it.getDisplayPhoto())
                handleExpertiseLevel(it.expertise_level)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        viewModel.fetchData(requireContext())
        val currentSearch = binding.edtSearch.text.toString().trim()
        viewModel.setSearchQuery(requireContext(), currentSearch)
        viewModel.updateTodayPickFromLocal(requireContext())
    }
    override fun initListener() {
        binding.ivBack.singleClick {
            popBackStack()
        }
        categoryAdapter.setOnClickItemRecyclerView { category, position ->
            categoryAdapter.setSelectedPosition(position)
            ViewUtils.scrollToCenter(binding.rvCategory, position)
            viewModel.setCategory(requireContext(),category.value)
            binding.rvTemplates.scrollToPosition(0)
        }

        yogaPoseAdapter.setOnClickItemRecyclerView { pose, _ ->
            viewModel.trackPoseInteraction(pose)
            navigateToDetail(pose)
        }
        yogaPoseAdapter.setOnFavoriteClick { pose ->
            viewModel.toggleFavorite(requireContext(),pose)
        }
        binding.edtSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(requireContext(),text.toString().trim())
        }

        binding.cvTodayPick.singleClick {
            viewModel.todayPickPose.value?.let { navigateToDetail(it) }
        }
    }

    private fun navigateToDetail(pose: com.hadat.aiyoga.yoga_ai.YogaPoseModel) {
        val bundle = Bundle().apply { putParcelable("yogaPoseItem", pose) }
        navigate(R.id.detailYogaFragment, bundle)
    }
    private fun handleExpertiseLevel(level: Int) {
        val levelResId = when (level) {
            1 -> R.string.beginner
            2 -> R.string.intermediate
            3 -> R.string.advanced
            else -> R.string.beginner
        }

        binding.tvLevel.text = getString(levelResId)
    }
    override fun initData() {
        viewModel.fetchData(requireContext())
    }


}
