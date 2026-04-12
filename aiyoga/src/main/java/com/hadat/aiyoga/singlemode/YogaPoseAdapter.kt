package com.hadat.aiyoga.singlemode

import androidx.core.content.ContextCompat
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.ItemTemplateBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.yogamain.YogaPoseModel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.singleClick

class YogaPoseAdapter : BaseRecyclerViewAdapter<YogaPoseModel, ItemTemplateBinding>() {
    private var onFavoriteClick: ((YogaPoseModel) -> Unit)? = null
    override fun bindData(binding: ItemTemplateBinding, item: YogaPoseModel, position: Int) {
        val context = binding.root.context
        val heartRes = if (item.isFavorite) R.drawable.ic_on_heart else R.drawable.ic_un_heart
        binding.ivFavorite.setImageResource(heartRes)
        binding.ivFavorite.singleClick {
            item.isFavorite = !item.isFavorite
            notifyItemChanged(position)
            onFavoriteClick?.invoke(item)
        }
        binding.tvPoseName.text = item.name

        when (item.expertise_level) {
            1 -> {
                binding.tvComplexity.text = "Beginner"
                binding.llComplexity.background = ContextCompat.getDrawable(context, R.drawable.shape_level_easy)
            }
            2 -> {
                binding.tvComplexity.text = "Intermediate"
                binding.llComplexity.background = ContextCompat.getDrawable(context, R.drawable.shape_level_medium)
            }
            3 -> {
                binding.tvComplexity.text = "Advanced"
                binding.llComplexity.background = ContextCompat.getDrawable(context, R.drawable.shape_level_hard)
            }
            else -> {
                binding.tvComplexity.text = "Unknown"
                binding.llComplexity.background = ContextCompat.getDrawable(context, R.drawable.shape_level_easy)
            }
        }
        binding.ivThump.loadImageFromNetwork(item.getDisplayPhoto())

    }
    fun setOnFavoriteClick(listener: (YogaPoseModel) -> Unit) {
        onFavoriteClick = listener
    }
}