package com.hadat.aiyoga.community_mysequence

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hadat.aiyoga.R
import com.hadat.aiyoga.community.CommunityCategory
import com.hadat.aiyoga.community.CommunityCategoryAdapter
import com.hadat.aiyoga.community.SequencesCommunityAdapter
import com.hadat.aiyoga.databinding.FragmentCommunityMySequenceBinding
import com.hadat.aiyoga.community_mysequence.MySequenceAdapter
import com.hadat.aiyoga.databinding.DialogDeleteBinding
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import com.hadat.aiyoga.service.AppPreferences
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class CommunityMySequenceFragment : BaseFragment<FragmentCommunityMySequenceBinding, CommunityMySequenceViewModel>() {

    private val categoryAdapter by lazy { CommunityCategoryAdapter() }

    private val communitySequencesAdapter by lazy {
        SequencesCommunityAdapter { sequence ->
            val bundle = Bundle().apply {
                putParcelable("detail_sequence", sequence)
            }
            navigate(R.id.sequenceCommunityDetailFragment, bundle)
        }
    }

    private val mySequenceAdapter by lazy {
        MySequenceAdapter(
            onAddClick = { navigate(R.id.choosePoseFragment) },
            onItemClick = { sequence ->
                val bundle = Bundle().apply {
                    putParcelable("detail_sequence", sequence)
                }
                navigate(R.id.detailSequenceFragment, bundle)
            },
            onDeleteClick = { sequence, position ->
                showDeleteDialog(sequence, position)
            }
        )
    }

    private inner class PagerAdapter : RecyclerView.Adapter<PagerViewHolder>() {
        override fun getItemCount(): Int = 2

        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val layoutRes = if (viewType == 0) {
                R.layout.page_community_my_sequence_community
            } else {
                R.layout.page_community_my_sequence_my
            }
            val view = inflater.inflate(layoutRes, parent, false)
            return PagerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
            holder.bind(position)
        }
    }

    private inner class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            if (position == 0) {
                itemView.findViewById<RecyclerView>(R.id.rv_category).apply {
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = categoryAdapter
                }
                itemView.findViewById<RecyclerView>(R.id.rv_sequences).apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = communitySequencesAdapter
                }
            } else {
                itemView.findViewById<RecyclerView>(R.id.rv_my_sequences).apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = mySequenceAdapter
                }
            }
        }
    }

    private val pagerAdapter by lazy { PagerAdapter() }

    override fun initView() {
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"

        categoryAdapter.setList(CommunityCategory.entries)
        categoryAdapter.setOnClickItemListener = { category, _ ->
            viewModel.selectCategory(category, userId)
        }

        viewModel.selectedCategory.observe(viewLifecycleOwner) { selected ->
            val index = CommunityCategory.entries.indexOf(selected)
            if (index != -1) categoryAdapter.setSelectedPosition(index)
        }

        viewModel.communitySequences.observe(viewLifecycleOwner) { list ->
            communitySequencesAdapter.setList(list)
        }

        viewModel.mySequences.observe(viewLifecycleOwner) { list ->
            mySequenceAdapter.setList(list)
        }

        viewModel.fetchAll(userId = userId)

        binding.vpCommunityMySequences.adapter = pagerAdapter
        binding.vpCommunityMySequences.offscreenPageLimit = 1
        binding.vpCommunityMySequences.isUserInputEnabled = false
        val initialTab = arguments?.getInt(ARG_INITIAL_TAB) ?: TAB_COMMUNITY
        binding.vpCommunityMySequences.setCurrentItem(initialTab, false)
        updateTabUI(initialTab)
    }

    override fun initData() {

    }

    override fun initListener() {
        binding.tvTabCommunity.singleClick { binding.vpCommunityMySequences.setCurrentItem(TAB_COMMUNITY, true) }
        binding.tvTabMySequences.singleClick { binding.vpCommunityMySequences.setCurrentItem(TAB_MY_SEQUENCES, true) }

        binding.vpCommunityMySequences.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabUI(position)
            }
        })

        binding.vpCommunityMySequences.getChildAt(0)?.let {
        }
    }
    private fun showDeleteDialog(item: WorkoutSequenceModel, position: Int) {
        val dialog = Dialog(requireContext())
        val bindingDialog = DialogDeleteBinding.inflate(layoutInflater)

        dialog.apply {
            setContentView(bindingDialog.root)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.85).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        bindingDialog.apply {

            ivClose.singleClick { dialog.dismiss() }

            btnConfirm.singleClick {
                val userId = AppPreferences.getUserId(requireContext()) ?: ""
                viewModel.deleteSequence(item.id, userId)
                mySequenceAdapter.notifyItemRemoved(position)

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateTabUI(selectedTab: Int) {
        val gray = requireContext().getColor(R.color.text_gray)

        if (selectedTab == TAB_COMMUNITY) {
            binding.layoutTabCommunity.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.layoutTabMySequences.setBackgroundResource(android.R.color.transparent)

            binding.tvTabCommunity.setTextColor(requireContext().getColor(android.R.color.white))
            binding.tvTabMySequences.setTextColor(gray)
        } else {
            binding.layoutTabCommunity.setBackgroundResource(android.R.color.transparent)
            binding.layoutTabMySequences.setBackgroundResource(R.drawable.bg_tab_selected)

            binding.tvTabCommunity.setTextColor(gray)
            binding.tvTabMySequences.setTextColor(requireContext().getColor(android.R.color.white))
        }
    }

    companion object {
        private const val ARG_INITIAL_TAB = "initial_tab"
        private const val TAB_COMMUNITY = 0
        private const val TAB_MY_SEQUENCES = 1
    }
}

