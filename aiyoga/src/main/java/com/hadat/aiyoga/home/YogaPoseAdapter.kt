package com.hadat.aiyoga.home

import android.graphics.Color
import androidx.core.content.ContextCompat
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.ItemTemplateBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.yogamain.YogaPoseModel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class YogaPoseAdapter : BaseRecyclerViewAdapter<YogaPoseModel, ItemTemplateBinding>() {
    override fun bindData(binding: ItemTemplateBinding, item: YogaPoseModel, position: Int) {
        val context = binding.root.context

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

        binding.ivThumb.loadImageFromNetwork(item.photo_url)
    }
}