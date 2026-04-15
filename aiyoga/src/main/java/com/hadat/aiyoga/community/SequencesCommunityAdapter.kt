package com.hadat.aiyoga.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
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
        binding.root.singleClick { onItemClick(item) }

        binding.ivSequenceThumb.loadImageFromNetwork(item.coverImageUrl)
        binding.tvSequenceName.text = item.title
        binding.tvDuration.text = formatDurationToMin(item.totalDuration)
        binding.tvPosesCount.text = "${item.poses.size} Poses"

        binding.tvLevelBadge.text = when (item.level) {
            1 -> "Beginner"
            2 -> "Intermediate"
            3 -> "Advanced"
            else -> "All Levels"
        }

        binding.tvAuthorName.isVisible = item.userId.isNotBlank()
        if (binding.tvAuthorName.isVisible) {
            binding.tvAuthorName.text = "by ${item.userId}"
        }

        binding.tvLikeCount.text = item.likeCount.toString()
        binding.tvViewCount.text = item.viewCount.toString()
    }

    private fun formatDurationToMin(duration: String): String {
        return try {
            val parts = duration.split(":")
            val minutes = parts.getOrNull(0)?.toIntOrNull() ?: 0
            "$minutes min"
        } catch (_: Exception) {
            "0 min"
        }
    }
}

