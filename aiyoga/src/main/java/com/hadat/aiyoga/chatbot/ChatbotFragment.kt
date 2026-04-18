package com.hadat.aiyoga.chatbot

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.databinding.FragmentChatbotBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.base.adapter.animation.AnimationType
import hoang.dqm.codebase.utils.singleClick

class ChatbotFragment : BaseFragment<FragmentChatbotBinding, ChatbotViewModel>() {

    private val chatAdapter by lazy { ChatAdapter() }
    private val suggestionAdapter by lazy { SuggestionAdapter() }

    override fun initView() {
        setupChatList()
        setupSuggestionList()
        setupKeyboardListener()
    }

    override fun initListener() {
        binding.apply {
            ivBack.singleClick { popBackStack() }

            btnSend.singleClick {
                val text = edtMessage.text.toString().trim()
                if (text.isNotEmpty()) performChatAction(text)
            }

            suggestionAdapter.setOnClickItemRecyclerView { suggestion, _ ->
                if (suggestion.isNotEmpty()) performChatAction(suggestion)
            }
        }
    }

    override fun initData() {
        suggestionAdapter.setList(viewModel.getSuggestions())
        viewModel.chatMessages.observe(viewLifecycleOwner) { messages ->
            updateChatUI(messages)
        }
    }

    private fun performChatAction(text: String) {
        viewModel.sendMessage(text)
        binding.edtMessage.text?.clear()

        showChatAndHideSuggestions()
    }

    private fun updateChatUI(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        showChatAndHideSuggestions()
        val isBotProcessing = messages.lastOrNull()?.type == MessageType.TYPING
        binding.btnSend.isEnabled = !isBotProcessing
        binding.btnSend.alpha = if (isBotProcessing) 0.5f else 1.0f
        chatAdapter.setList(messages.toList())
        binding.rvChat.post {
            if (chatAdapter.itemCount > 0) {
                binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun showChatAndHideSuggestions() {
        if (binding.llSuggestionsCenter.isVisible) {
            binding.llSuggestionsCenter.visibility = View.GONE
            binding.rvChat.visibility = View.VISIBLE
        }
    }

    private fun setupChatList() {
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter.apply {
                setItemAnimation(AnimationType.SlideInBottom)
            }
        }
    }

    private fun setupSuggestionList() {
        binding.rvSuggestions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = suggestionAdapter.apply {
                setItemAnimation(AnimationType.SlideInRight)
            }
        }
    }

    private fun setupKeyboardListener() {
        binding.rvChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom && chatAdapter.itemCount > 0) {
                binding.rvChat.postDelayed({
                    binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }, 100)
            }
        }
    }
}