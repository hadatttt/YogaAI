package com.hadat.aiyoga.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.hadat.aiyoga.databinding.ItemAddSequencesBinding
import com.hadat.aiyoga.databinding.ItemSequencesBinding
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick

class RecentSequencesAdapter(
    private val onAddClick: () -> Unit,
    private val onItemClick: (WorkoutSequenceModel) -> Unit
) : BaseRecyclerViewAdapter<WorkoutSequenceModel, ViewBinding>() {

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int, list: List<WorkoutSequenceModel>): Int {
        return if (position == 0) TYPE_ADD else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ADD) {
            val binding = ItemAddSequencesBinding.inflate(inflater, parent, false)
            BaseViewHolder(binding)
        } else {
            val binding = ItemSequencesBinding.inflate(inflater, parent, false)
            BaseViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size + 1
    }

    override fun bindData(binding: ViewBinding, item: WorkoutSequenceModel, position: Int) {

    }

    override fun onBindViewHolder(holder: BaseViewHolder<ViewBinding>, position: Int) {
        if (getItemViewType(position) == TYPE_ADD) {
            val binding = holder.binding as ItemAddSequencesBinding
            binding.root.setOnClickListener { onAddClick() }
        } else {
            val data = dataList[position - 1]
            val binding = holder.binding as ItemSequencesBinding

            binding.apply {
                root.singleClick { onItemClick(data) }

                tvSequenceName.text = data.title

                ivSequenceThumb.loadImageFromNetwork(data.coverImageUrl)

                tvLevelValue.text = when (data.level) {
                    1 -> "Beginner"
                    2 -> "Intermediate"
                    3 -> "Advanced"
                    else -> "All Levels"
                }

                tvDuration.text = formatDurationToMin(data.totalDuration)
            }
        }
    }

    private fun formatDurationToMin(duration: String): String {
        return try {
            val parts = duration.split(":")
            if (parts.isNotEmpty()) {
                val minutes = parts[0].toIntOrNull() ?: 0
                "$minutes min"
            } else { "0 min" }
        } catch (e: Exception) { "0 min" }
    }
}