package com.hadat.aiyoga.chatbot

import com.hadat.aiyoga.databinding.ItemSuggestionChipBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class SuggestionAdapter : BaseRecyclerViewAdapter<String, ItemSuggestionChipBinding>() {

    override fun bindData(binding: ItemSuggestionChipBinding, item: String, position: Int) {
        binding.tvSuggestion.text = item
    }
}