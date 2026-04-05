package com.hadat.aiyoga.detailyoga

import com.hadat.aiyoga.databinding.ItemDesBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class DescriptionAdapter : BaseRecyclerViewAdapter<String, ItemDesBinding>() {
    override fun bindData(
        binding: ItemDesBinding,
        item: String,
        position: Int
    ) {
        binding.apply {
            tvBenefitDesc.text = item
        }
    }
}