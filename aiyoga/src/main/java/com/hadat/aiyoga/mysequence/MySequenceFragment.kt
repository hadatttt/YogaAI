package com.hadat.aiyoga.mysequence

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentMySequenceBinding
import com.hadat.aiyoga.service.AppPreferences
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class MySequenceFragment : BaseFragment<FragmentMySequenceBinding, MySequenceViewModel>() {

    private val mySequenceAdapter by lazy {
        MySequenceAdapter(
            onAddClick = { navigate(R.id.choosePoseFragment) },
            onItemClick = { sequence ->
                val bundle = Bundle().apply {
                    putParcelable("detail_sequence", sequence)
                }
                navigate(R.id.detailSequenceFragment, bundle)
            }
        )
    }

    override fun initView() {
        binding.rvMySequences.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mySequenceAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        viewModel.fetchMySequences(userId)
    }

    override fun initData() {
        viewModel.mySequences.observe(viewLifecycleOwner) {
            mySequenceAdapter.setList(it)
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
    }
}
