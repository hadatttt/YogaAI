package com.hadat.aiyoga.detailyoga

import com.hadat.aiyoga.databinding.ItemContraindicationBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class ContraindicationAdapter : BaseRecyclerViewAdapter<YogaNoteModel, ItemContraindicationBinding>() {

    override fun bindData(binding: ItemContraindicationBinding, item: YogaNoteModel, position: Int) {
        binding.apply {
            tvContraTitle.text = item.title
            tvContraDesc.text = item.description
        }
    }
}