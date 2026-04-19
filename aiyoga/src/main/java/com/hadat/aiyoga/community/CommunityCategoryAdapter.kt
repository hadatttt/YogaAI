package com.hadat.aiyoga.community

import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.ItemCategoryBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.animation.AnimationType

class CommunityCategoryAdapter : BaseRecyclerViewAdapter<CommunityCategory, ItemCategoryBinding>() {

    private var selectedPosition = 0

    init {
        animationEnable = true
        setItemAnimation(AnimationType.ScaleIn)
        isAnimationFirstOnly = false
    }

    fun setSelectedPosition(newPosition: Int) {
        if (newPosition == selectedPosition) return
        val previousPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(previousPosition)
        notifyItemChanged(newPosition)
    }

    override fun bindData(binding: ItemCategoryBinding, item: CommunityCategory, position: Int) {
        binding.txtCategory.setText(item.titleRes)
        if (position == selectedPosition) {
            binding.txtCategory.setBackgroundResource(R.drawable.ic_category_selected)
            binding.txtCategory.setTextColor(context.getColor(R.color.white))
        } else {
            binding.txtCategory.setBackgroundResource(R.drawable.ic_category_normal)
            binding.txtCategory.setTextColor(context.getColor(R.color.primary))
        }
    }

    override fun bindViewClickListener(
        viewHolder: hoang.dqm.codebase.base.adapter.BaseViewHolder<ItemCategoryBinding>,
        _viewType: Int
    ) {
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            if (position != -1) {
                val item = getItem(position)
                setSelectedPosition(position)
                setOnClickItemListener?.invoke(item, position)
            }
        }
    }
}