package com.hadat.aiyoga.result

import android.view.LayoutInflater
import android.view.ViewGroup
import com.hadat.aiyoga.databinding.ItemWorkoutHistoryBinding
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick

class WorkoutHistoryAdapter(
    private val onItemClick: (WorkoutResultModel) -> Unit
) : BaseRecyclerViewAdapter<WorkoutResultModel, ItemWorkoutHistoryBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemWorkoutHistoryBinding> {
        val binding = ItemWorkoutHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(binding)
    }

    override fun bindData(binding: ItemWorkoutHistoryBinding, item: WorkoutResultModel, position: Int) {
        binding.root.singleClick { onItemClick(item) }
        binding.ivThumb.loadImageFromNetwork(item.poseUrl)
        binding.tvPoseName.text = item.poseName
        binding.tvDuration.text = "${item.durationInSeconds}s"
    }
}

