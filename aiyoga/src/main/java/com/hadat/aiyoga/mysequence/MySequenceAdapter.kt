package com.hadat.aiyoga.mysequence

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.hadat.aiyoga.databinding.ItemAddMySequenceBinding
import com.hadat.aiyoga.databinding.ItemMySequenceBinding
import com.hadat.aiyoga.R
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick

class MySequenceAdapter(
    private val onAddClick: () -> Unit,
    private val onItemClick: (WorkoutSequenceModel) -> Unit
) : BaseRecyclerViewAdapter<WorkoutSequenceModel, ViewBinding>() {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int, list: List<WorkoutSequenceModel>): Int {
        return if (position == dataList.size) TYPE_ADD else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ADD) {
            BaseViewHolder(ItemAddMySequenceBinding.inflate(inflater, parent, false))
        } else {
            BaseViewHolder(ItemMySequenceBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int = dataList.size + 1

    override fun bindData(binding: ViewBinding, item: WorkoutSequenceModel, position: Int) = Unit

    override fun onBindViewHolder(holder: BaseViewHolder<ViewBinding>, position: Int) {
        if (getItemViewType(position) == TYPE_ADD) {
            val binding = holder.binding as ItemAddMySequenceBinding
            binding.root.singleClick { onAddClick() }
            return
        }

        val item = dataList[position]
        val binding = holder.binding as ItemMySequenceBinding

        binding.root.singleClick { onItemClick(item) }
        binding.tvSequenceName.text = item.title
        binding.ivSequenceThumb.loadImageFromNetwork(item.coverImageUrl)
        binding.ivVisibility.setImageResource(if (item.isPublic) R.drawable.ic_public else R.drawable.ic_lock)
        binding.tvLevelValue.text = when (item.level) {
            1 -> "Beginner"
            2 -> "Intermediate"
            3 -> "Advanced"
            else -> "All Levels"
        }
        binding.tvDuration.text = formatDurationToMin(item.totalDuration)
    }

    private fun formatDurationToMin(duration: String): String {
        return try {
            val minutes = duration.split(":").firstOrNull()?.toIntOrNull() ?: 0
            "$minutes min"
        } catch (_: Exception) {
            "0 min"
        }
    }
}
