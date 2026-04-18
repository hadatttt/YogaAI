package com.hadat.aiyoga.chatbot

import android.view.ViewGroup
import com.hadat.aiyoga.databinding.ItemSuggestionChipBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.base.adapter.createBindingViewHolder

class SuggestionAdapter : BaseRecyclerViewAdapter<String, ItemSuggestionChipBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemSuggestionChipBinding> {
        return createBindingViewHolder(parent)
    }

    override fun bindData(binding: ItemSuggestionChipBinding, item: String, position: Int) {
        binding.tvSuggestion.text = item
        binding.root.isClickable = true
        binding.root.isFocusable = true
    }
}