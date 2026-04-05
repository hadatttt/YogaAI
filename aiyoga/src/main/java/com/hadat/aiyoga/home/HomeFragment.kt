package com.hadat.aiyoga.home

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentHomeBinding
import com.hadat.aiyoga.utils.ViewUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    private val categoryAdapter by lazy { CategoryAdapter() }
    private val yogaPoseAdapter by lazy { YogaPoseAdapter() }

    override fun initView() {
        binding.rvTemplates.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = yogaPoseAdapter.apply {
                setItemAnimation(hoang.dqm.codebase.base.adapter.animation.AnimationType.SlideInBottom)
                isAnimationFirstOnly = false
            }
        }
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            clipToPadding = false
        }
        viewModel.categoryList.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.setList(categories)
        }

        viewModel.yogaPoseList.observe(viewLifecycleOwner) { poses ->
            yogaPoseAdapter.setList(poses)
        }
    }

    override fun initListener() {
        categoryAdapter.setOnClickItemRecyclerView { category, position ->
            categoryAdapter.setSelectedPosition(position)
            ViewUtils.scrollToCenter(binding.rvCategory, position)
            viewModel.filterPoses(category.value)
            binding.rvTemplates.scrollToPosition(0)
        }

        yogaPoseAdapter.setOnClickItemRecyclerView { pose, _ ->
            val bundle = Bundle().apply {
                putParcelable("yogaPoseItem", pose)
            }
            navigate(R.id.detailYogaFragment, bundle)
        }

        binding.btnNewProject.singleClick {
            navigate(R.id.yogaFragment)
        }

        binding.llChatbotWrapper.setDraggableWithClick { }
    }

    override fun initData() {
        viewModel.fetchData()
    }
}