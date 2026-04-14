package com.hadat.aiyoga.sequence

import com.hadat.aiyoga.databinding.ItemPoseRecommendBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.yogamain.YogaPoseModel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class RecommendPoseAdapter : BaseRecyclerViewAdapter<YogaPoseModel, ItemPoseRecommendBinding>() {
    override fun bindData(binding: ItemPoseRecommendBinding, item: YogaPoseModel, position: Int) {
        binding.apply {
            tvPoseName.text = item.name
            ivThumb.loadImageFromNetwork(item.photo_url)
        }
    }
}