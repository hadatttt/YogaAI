package com.hadat.aiyoga.detailsequence

import com.hadat.aiyoga.databinding.ItemPoseDetailBinding
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class PoseDetailAdapter : BaseRecyclerViewAdapter<SequenceModel, ItemPoseDetailBinding>() {

    override fun bindData(binding: ItemPoseDetailBinding, item: SequenceModel, position: Int) {
        binding.apply {
            tvIndex.text = (position + 1).toString()

            tvName.text = item.name
            tvCategory.text = item.category
            tvDuration.text = item.duration
            ivThump.loadImageFromNetwork(item.photoUrl)
        }
    }
}