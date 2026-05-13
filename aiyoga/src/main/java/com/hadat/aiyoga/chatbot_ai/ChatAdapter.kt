package com.hadat.aiyoga.chatbot_ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.google.gson.Gson
import com.hadat.aiyoga.databinding.ItemChatBotBinding
import com.hadat.aiyoga.databinding.ItemChatTypingBinding
import com.hadat.aiyoga.databinding.ItemChatUserBinding
import com.hadat.aiyoga.databinding.LayoutAiGeneratedBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick

class ChatAdapter(
    private val onSaveClick: (AiSequenceResponse) -> Unit
) : BaseRecyclerViewAdapter<ChatMessage, ViewBinding>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
        private const val TYPE_TYPING = 3
        private const val TYPE_AI_SEQUENCE = 4
    }

    override fun getItemViewType(position: Int, list: List<ChatMessage>): Int {
        val item = list[position]
        return when {
            item.type == MessageType.USER -> TYPE_USER
            item.type == MessageType.TYPING -> TYPE_TYPING
            isJson(item.content) -> TYPE_AI_SEQUENCE
            else -> TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (viewType) {
            TYPE_USER -> ItemChatUserBinding.inflate(inflater, parent, false)
            TYPE_BOT -> ItemChatBotBinding.inflate(inflater, parent, false)
            TYPE_TYPING -> ItemChatTypingBinding.inflate(inflater, parent, false)
            else -> LayoutAiGeneratedBinding.inflate(inflater, parent, false)
        }
        return BaseViewHolder(binding).apply {
            bindViewClickListener(this, viewType)
        }
    }

    override fun bindData(binding: ViewBinding, item: ChatMessage, position: Int) {
        when (binding) {
            is ItemChatUserBinding -> binding.tvMessage.text = item.content
            is ItemChatBotBinding -> binding.tvMessage.text = item.content
            is LayoutAiGeneratedBinding -> bindAiSequence(binding, item.content)
        }
    }

    private fun bindAiSequence(binding: LayoutAiGeneratedBinding, jsonContent: String) {
        try {
            val sequence = Gson().fromJson(jsonContent, AiSequenceResponse::class.java)
            binding.apply {
                tvAiDescription.text = sequence.title
                tvCaloriesValue.text = "Calo: ${String.format("%.1f", sequence.total_calories)} kcal"

                val miniAdapter = AiPoseMiniAdapter()
                rvAiPoses.layoutManager = LinearLayoutManager(root.context)
                rvAiPoses.adapter = miniAdapter
                miniAdapter.setList(sequence.poses)

                btnSaveAiSequence.singleClick {
                    onSaveClick(sequence)
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun isJson(content: String): Boolean {
        return try {
            val obj = Gson().fromJson(content, AiSequenceResponse::class.java)
            obj.poses.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
