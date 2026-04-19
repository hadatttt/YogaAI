package com.hadat.aiyoga.mysequence

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.hadat.aiyoga.databinding.ItemAddMySequenceBinding
import com.hadat.aiyoga.databinding.ItemMySequenceBinding
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.ItemSequencesCommunityBinding
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
        val context = binding.root.context

        binding.root.singleClick { onItemClick(item) }

        binding.tvSequenceName.text = item.title
        binding.ivSequenceThumb.loadImageFromNetwork(item.coverImageUrl)

        val visibilityRes = if (item.isPublic) R.drawable.ic_public else R.drawable.ic_lock
        binding.ivVisibility.setImageResource(visibilityRes)

        binding.tvLevelValue.setText(
            when (item.level) {
                1 -> R.string.beginner
                2 -> R.string.intermediate
                3 -> R.string.advanced
                else -> R.string.beginner
            }
        )

        binding.tvDuration.text = formatDurationToMin(context, item.totalDuration)
    }

    private fun formatDurationToMin(context: android.content.Context, duration: String): String {
        return try {
            val parts = duration.split(":")
            val minutes = if (parts.isNotEmpty()) {
                parts[0].toIntOrNull() ?: 0
            } else 0
            context.getString(R.string.format_minutes, minutes)
        } catch (_: Exception) {
            context.getString(R.string.format_minutes, 0)
        }
    }
}
