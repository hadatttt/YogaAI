package com.hadat.aiyoga.community

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentCommunityBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class CommunityFragment : BaseFragment<FragmentCommunityBinding, CommunityViewModel>() {

    private val categoryAdapter by lazy { CommunityCategoryAdapter() }

    private val sequencesAdapter by lazy {
        SequencesCommunityAdapter { sequence ->
            val bundle = Bundle().apply {
                putParcelable("detail_sequence", sequence)
            }
            navigate(R.id.sequenceCommunityDetailFragment, bundle)
        }
    }

    override fun initView() {
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        binding.rvSequences.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sequencesAdapter
        }
    }

    override fun initData() {
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"

        categoryAdapter.setList(CommunityCategory.entries)

        viewModel.sequences.observe(viewLifecycleOwner) {
            sequencesAdapter.setList(it)
        }

        viewModel.selectedCategory.observe(viewLifecycleOwner) { selectedEnum ->
            val index = CommunityCategory.entries.indexOf(selectedEnum)
            if (index != -1) {
                categoryAdapter.setSelectedPosition(index)
            }
        }

        viewModel.fetchSequences(userId = userId)
    }

    override fun onResume() {
        super.onResume()
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        viewModel.fetchSequences(userId = userId)
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        categoryAdapter.setOnClickItemListener = { category, position ->
            val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
            viewModel.selectCategory(category, userId)
        }
    }
}