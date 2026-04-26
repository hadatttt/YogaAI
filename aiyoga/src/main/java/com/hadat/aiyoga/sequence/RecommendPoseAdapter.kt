package com.hadat.aiyoga.sequence

import com.hadat.aiyoga.databinding.ItemPoseRecommendBinding
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.yoga_ai.YogaPoseModel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class RecommendPoseAdapter : BaseRecyclerViewAdapter<YogaPoseModel, ItemPoseRecommendBinding>() {
    override fun bindData(binding: ItemPoseRecommendBinding, item: YogaPoseModel, position: Int) {
        binding.apply {
            tvPoseName.text = item.name
            ivThumb.loadImageFromNetwork(item.photo_url)
        }
    }
}