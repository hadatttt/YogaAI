package com.hadat.aiyoga.detailsequence

import com.hadat.aiyoga.databinding.ItemPoseDetailBinding
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class PoseDetailAdapter : BaseRecyclerViewAdapter<SequenceModel, ItemPoseDetailBinding>() {

    override fun bindData(binding: ItemPoseDetailBinding, item: SequenceModel, position: Int) {
        binding.apply {
            tvIndex.text = (position + 1).toString()

            tvName.text = item.name
            tvCategory.text = YogaDataUtils.getLocalizedCategory(binding.root.context, item.category)
            tvDuration.text = item.duration
            ivThump.loadImageFromNetwork(item.photoUrl)
        }
    }
}