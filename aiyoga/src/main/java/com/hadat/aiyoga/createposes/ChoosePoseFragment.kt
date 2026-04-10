package com.hadat.aiyoga.createposes


import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentChoosePosesBinding
import com.hadat.aiyoga.home.CategoryAdapter
import com.hadat.aiyoga.utils.ViewUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.adapter.animation.AnimationType
import hoang.dqm.codebase.utils.singleClick

class ChoosePoseFragment : BaseFragment<FragmentChoosePosesBinding, ChoosePoseViewModel>() {

    private val categoryAdapter by lazy { CategoryAdapter() }
    private val allPoseAdapter by lazy { AllPoseAdapter() }
    private val selectedAdapter by lazy { SelectedPoseAdapter() }

    override fun initView() {
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        binding.rvTemplates.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = allPoseAdapter.apply {
                setItemAnimation(AnimationType.SlideInBottom)
                isAnimationFirstOnly = false
            }
        }

        binding.rvSelected.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedAdapter
        }
    }

    override fun initListener() {
        binding.edtSearch.addTextChangedListener {
            viewModel.setSearchQuery(it.toString().trim())
        }

        categoryAdapter.setOnClickItemRecyclerView { category, position ->
            categoryAdapter.setSelectedPosition(position)
            ViewUtils.scrollToCenter(binding.rvCategory, position)
            viewModel.setCategory(category.value)
        }

        allPoseAdapter.setOnItemClick { pose ->
            navigateToDetail(pose)
        }
        allPoseAdapter.setOnAddClick { pose ->
            viewModel.addPose(pose)
        }

        selectedAdapter.setOnRemoveClick { pose ->
            viewModel.removePose(pose)
        }

        binding.btnCreate.singleClick {
            val finalIds = viewModel.selectedIds.value

            if (finalIds.isNullOrEmpty()) return@singleClick

        }
    }
    private fun navigateToDetail(pose: com.hadat.aiyoga.yogamain.YogaPoseModel) {
        val bundle = Bundle().apply { putParcelable("yogaPoseItem", pose) }
        navigate(R.id.detailYogaFragment, bundle)
    }
    override fun initData() {
        viewModel.selectedPoses.observe(viewLifecycleOwner) { list ->
            selectedAdapter.setList(list)
            binding.tvSelectedTitle.text = "Selected Poses (${list.size})"

            val isEnable = list.isNotEmpty()

            binding.btnCreate.isEnabled = isEnable
            binding.btnCreate.isSelected = isEnable
        }
        viewModel.resetSelected()
        viewModel.fetchData()
        viewModel.categoryList.observe(viewLifecycleOwner) { categoryAdapter.setList(it) }
        viewModel.yogaPoseList.observe(viewLifecycleOwner) { allPoseAdapter.setList(it) }

    }
}