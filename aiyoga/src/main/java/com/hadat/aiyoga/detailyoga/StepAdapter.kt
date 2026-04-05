package com.hadat.aiyoga.detailyoga

import com.hadat.aiyoga.databinding.ItemStepBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class StepAdapter : BaseRecyclerViewAdapter<StepModel, ItemStepBinding>() {

    override fun bindData(binding: ItemStepBinding, item: StepModel, position: Int) {
        binding.apply {
            tvStepNumber.text = item.step_number.toString()
            tvStepDesc.text = item.description
            tvStepTitle.text= item.title
        }
    }
}