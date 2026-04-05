package com.hadat.aiyoga.detailyoga

import androidx.core.content.ContextCompat
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.ItemCategoryDetailBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.animation.AnimationType

class CategoryDetailAdapter() : BaseRecyclerViewAdapter<CategoryDetailModel, ItemCategoryDetailBinding>() {
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
    override fun bindData(binding: ItemCategoryDetailBinding, item: CategoryDetailModel, position: Int) {
        val context = binding.root.context
        binding.txtCategory.text = item.title
        val drawable = ContextCompat.getDrawable(context, item.iconRes)
        drawable?.setBounds(0, 0, context.resources.getDimensionPixelSize(hoang.dqm.codebase.R.dimen._18sdp),
            context.resources.getDimensionPixelSize(hoang.dqm.codebase.R.dimen._18sdp))
        if (position == selectedPosition) {
            binding.txtCategory.setBackgroundResource(R.drawable.ic_category_selected)

            drawable?.setTint(context.getColor(R.color.white))
        } else {
            binding.txtCategory.setBackgroundResource(R.drawable.ic_category_normal)
            drawable?.setTint(context.getColor(R.color.white))
        }
        binding.txtCategory.setCompoundDrawables(drawable, null, null, null)
        binding.txtCategory.compoundDrawablePadding = 16

    }
    override fun bindViewClickListener(viewHolder: hoang.dqm.codebase.base.adapter.BaseViewHolder<ItemCategoryDetailBinding>, _viewType: Int) {
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