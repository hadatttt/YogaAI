package com.hadat.aiyoga.chatbot

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.hadat.aiyoga.databinding.ItemChatBotBinding
import com.hadat.aiyoga.databinding.ItemChatTypingBinding
import com.hadat.aiyoga.databinding.ItemChatUserBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder

class ChatAdapter : BaseRecyclerViewAdapter<ChatMessage, ViewBinding>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
        private const val TYPE_TYPING = 3
    }

    override fun getItemViewType(position: Int, list: List<ChatMessage>): Int {
        return when (list[position].type) {
            MessageType.USER -> TYPE_USER
            MessageType.BOT -> TYPE_BOT
            MessageType.TYPING -> TYPE_TYPING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (viewType) {
            TYPE_USER -> ItemChatUserBinding.inflate(inflater, parent, false)
            TYPE_BOT -> ItemChatBotBinding.inflate(inflater, parent, false)
            else -> ItemChatTypingBinding.inflate(inflater, parent, false)
        }
        return BaseViewHolder(binding).apply {
            bindViewClickListener(this, viewType)
        }
    }

    override fun bindData(binding: ViewBinding, item: ChatMessage, position: Int) {
        when (binding) {
            is ItemChatUserBinding -> binding.tvMessage.text = item.content
            is ItemChatBotBinding -> binding.tvMessage.text = item.content
        }
    }
}