package com.hadat.aiyoga.chatbot_ai

import com.hadat.aiyoga.databinding.ItemAiPoseMiniBinding
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class AiPoseMiniAdapter : BaseRecyclerViewAdapter<SequenceModel, ItemAiPoseMiniBinding>() {
    override fun bindData(binding: ItemAiPoseMiniBinding, item: SequenceModel, position: Int) {
        binding.apply {
            tvName.text = item.name
            tvCategory.text = item.category
            tvDuration.text = item.duration
            ivThump.loadImageFromNetwork(item.photoUrl)
        }
    }
}