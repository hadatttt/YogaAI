package com.hadat.aiyoga.detailyoga

import com.hadat.aiyoga.databinding.ItemBenefitBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class BenefitAdapter : BaseRecyclerViewAdapter<YogaNoteModel, ItemBenefitBinding>() {

    override fun bindData(binding: ItemBenefitBinding, item: YogaNoteModel, position: Int) {
        binding.apply {
            tvBenefitTitle.text = item.title
            tvBenefitDesc.text = item.description

        }
    }
}