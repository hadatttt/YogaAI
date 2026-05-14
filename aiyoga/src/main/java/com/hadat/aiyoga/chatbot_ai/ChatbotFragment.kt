package com.hadat.aiyoga.chatbot_ai

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentChatbotBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.base.adapter.animation.AnimationType
import hoang.dqm.codebase.utils.singleClick

class ChatbotFragment : BaseFragment<FragmentChatbotBinding, ChatbotViewModel>() {

    private val chatAdapter by lazy {
        ChatAdapter { aiSequence ->
            val bundle = Bundle().apply {
                putParcelableArray("ai_poses_list", aiSequence.poses.toTypedArray())
            }
            navigate(R.id.sequencesFragment, bundle)
        }
    }
    private val suggestionAdapter by lazy { SuggestionAdapter() }

    override fun initView() {
        suggestionAdapter.setList(viewModel.getSuggestionIds().map { getString(it) })
        viewModel.chatMessages.observe(viewLifecycleOwner) { messages ->
            updateChatUI(messages)
        }
        setupChatList()
        setupSuggestionList()
        setupKeyboardHandling()
    }

    override fun initListener() {
        binding.apply {
            ivBack.singleClick { popBackStack() }
            ivClearChat.singleClick { viewModel.clearConversation() }

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

    }

    private fun setupKeyboardHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomNavHeight = getBottomNavHeight()

            val finalOffset = if (imeHeight > 0) {
                maxOf(imeHeight - navigationBarHeight - bottomNavHeight, 0)
            } else {
                0
            }
            binding.root.setPadding(0, 0, 0, finalOffset)

            insets
        }
    }
    private fun getBottomNavHeight(): Int {
        val activity = requireActivity()
        val resId = activity.resources.getIdentifier("bottomNavigation", "id", activity.packageName)
        val view = activity.findViewById<View>(resId)
        return view?.height ?: 0
    }

    private fun performChatAction(text: String) {
        viewModel.sendMessage(requireContext(),text)
        binding.edtMessage.text?.clear()

        // 1. Tắt bàn phím
        hideKeyboard()

        // 2. Bỏ focus khỏi EditText để layout ổn định
        binding.edtMessage.clearFocus()

        showChatAndHideSuggestions()
    }

    // Hàm tiện ích để ẩn bàn phím
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.edtMessage.windowToken, 0)
    }

    private fun updateChatUI(messages: List<ChatMessage>) {
        if (messages.isEmpty()) {
            chatAdapter.setList(emptyList())
            binding.rvChat.visibility = View.GONE
            binding.llSuggestionsCenter.visibility = View.VISIBLE
            binding.btnSend.isEnabled = true
            binding.btnSend.alpha = 1.0f
            return
        }
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
                stackFromEnd = false
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
}
