package com.hadat.aiyoga.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.ItemSequencesCommunityBinding
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick

class SequencesCommunityAdapter(
    private val onItemClick: (WorkoutSequenceModel) -> Unit
) : BaseRecyclerViewAdapter<WorkoutSequenceModel, ItemSequencesCommunityBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemSequencesCommunityBinding> {
        val binding = ItemSequencesCommunityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(binding)
    }

    override fun bindData(binding: ItemSequencesCommunityBinding, item: WorkoutSequenceModel, position: Int) {
        val context = binding.root.context

        binding.root.singleClick { onItemClick(item) }

        binding.ivSequenceThumb.loadImageFromNetwork(item.coverImageUrl)
        binding.tvSequenceName.text = item.title
        binding.tvDuration.text = formatDurationToMin(context, item.totalDuration)
        binding.tvPosesCount.text = context.getString(R.string.format_poses_count, item.poses.size)

        binding.tvLevelBadge.setText(
            when (item.level) {
                1 -> R.string.beginner
                2 -> R.string.intermediate
                3 -> R.string.advanced
                else -> R.string.beginner
            }
        )

        binding.tvAuthorName.isVisible = item.authorName.isNotBlank()
        if (binding.tvAuthorName.isVisible) {
            binding.tvAuthorName.text = context.getString(R.string.by_author, item.authorName)
        }

        binding.tvLikeCount.text = item.likeCount.toString()
        binding.tvViewCount.text = item.viewCount.toString()
    }

    private fun formatDurationToMin(context: android.content.Context, duration: String): String {
        return try {
            val parts = duration.split(":")
            val minutes = parts.getOrNull(0)?.toIntOrNull() ?: 0
            context.getString(R.string.format_minutes, minutes)
        } catch (_: Exception) {
            context.getString(R.string.format_minutes, 0)
        }
    }
}