package com.hadat.aiyoga.createposes

import com.hadat.aiyoga.databinding.ItemMiniPoseHorizontalBinding
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.yoga_ai.YogaPoseModel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.singleClick

class SelectedPoseAdapter : BaseRecyclerViewAdapter<YogaPoseModel, ItemMiniPoseHorizontalBinding>() {

    private var onRemoveClick: ((YogaPoseModel) -> Unit)? = null

    fun setOnRemoveClick(listener: (YogaPoseModel) -> Unit) {
        onRemoveClick = listener
    }
    override fun bindData(binding: ItemMiniPoseHorizontalBinding, item: YogaPoseModel, position: Int) {
        binding.apply {
            tvPoseName.text = item.name
            ivThumb.loadImageFromNetwork(item.photo_url)
            ivIcon.singleClick {
                onRemoveClick?.invoke(item)
            }
            root.setOnClickListener(null)
        }
    }
}