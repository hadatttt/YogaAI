package com.hadat.aiyoga.createposes

import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.ItemMiniPoseBinding
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.yoga_ai.YogaPoseModel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.singleClick

class AllPoseAdapter : BaseRecyclerViewAdapter<YogaPoseModel, ItemMiniPoseBinding>() {

    private var onAddClick: ((YogaPoseModel) -> Unit)? = null
    private var onItemClick: ((YogaPoseModel) -> Unit)? = null

    fun setOnAddClick(listener: (YogaPoseModel) -> Unit) {
        onAddClick = listener
    }

    fun setOnItemClick(listener: (YogaPoseModel) -> Unit) {
        onItemClick = listener
    }

    override fun bindData(binding: ItemMiniPoseBinding, item: YogaPoseModel, position: Int) {
        binding.apply {
            tvPoseName.text = item.name
            ivThumb.loadImageFromNetwork(item.photo_url)

            ivIcon.setImageResource(R.drawable.ic_add_pose)
            ivIcon.singleClick {
                onAddClick?.invoke(item)
            }
            ivThumb.singleClick {
                onItemClick?.invoke(item)
            }
            root.setOnClickListener(null)
        }
    }
}