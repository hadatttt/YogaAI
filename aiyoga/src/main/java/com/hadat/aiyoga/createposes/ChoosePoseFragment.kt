package com.hadat.aiyoga.createposes


import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentChoosePosesBinding
import com.hadat.aiyoga.home.CategoryAdapter
import com.hadat.aiyoga.utils.view.ViewUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
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
        viewModel.selectedPoses.observe(viewLifecycleOwner) { list ->
            selectedAdapter.setList(list)
            binding.tvSelectedTitle.text = getString(R.string.selected_poses_count, list.size)

            val isEnable = list.isNotEmpty()

            binding.btnCreate.isEnabled = isEnable
            binding.btnCreate.isSelected = isEnable
        }
        viewModel.fetchData(requireContext())
        viewModel.categoryList.observe(viewLifecycleOwner) { categoryAdapter.setList(it) }
        viewModel.yogaPoseList.observe(viewLifecycleOwner) { allPoseAdapter.setList(it) }
    }

    override fun initListener() {
        binding.edtSearch.addTextChangedListener {
            viewModel.setSearchQuery(it.toString().trim())
        }
        binding.ivBack.singleClick {
            popBackStack(R.id.homeFragment)
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
        binding.btnCreate.singleClick {
            val selectedList = viewModel.selectedPoses.value

            if (selectedList.isNullOrEmpty()) return@singleClick

            val bundle = Bundle().apply {
                putParcelableArray("selected_poses_list", selectedList.toTypedArray())
            }

            navigate(R.id.sequencesFragment, bundle)
        }
        onBackPressed {
            viewModel.resetSelected()
            popBackStack()
        }
    }
    private fun navigateToDetail(pose: com.hadat.aiyoga.yoga_ai.YogaPoseModel) {
        val bundle = Bundle().apply { putParcelable("yogaPoseItem", pose) }
        navigate(R.id.detailYogaFragment, bundle)
    }
    override fun initData() {

    }
}