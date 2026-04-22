package com.hadat.aiyoga.chatbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.api.ChatRequest
import com.hadat.aiyoga.data.api.RetrofitClient
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotViewModel : BaseViewModel() {

    private val _chatMessages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val chatMessages: LiveData<MutableList<ChatMessage>> get() = _chatMessages

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        updateMessageList(ChatMessage(text, MessageType.USER))
        fetchAiResponse(text)
    }

    private fun fetchAiResponse(userText: String) {
        setTypingStatus(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Gửi "question" lên server
                val result = RetrofitClient.apiService.sendMessage(ChatRequest(userText))
                withContext(Dispatchers.Main) {
                    setTypingStatus(false)
                    // Nhận "answer" từ server trả về
                    updateMessageList(ChatMessage(result.answer, MessageType.BOT))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setTypingStatus(false)
                    updateMessageList(ChatMessage("Lỗi kết nối AI Server!", MessageType.BOT))
                }
            }
        }
    }

    private fun setTypingStatus(isVisible: Boolean) {
        val currentList = _chatMessages.value ?: mutableListOf()
        if (isVisible) {
            currentList.add(ChatMessage(type = MessageType.TYPING))
        } else {
            currentList.removeAll { it.type == MessageType.TYPING }
        }
        _chatMessages.value = currentList
    }

    private fun updateMessageList(message: ChatMessage) {
        val currentList = _chatMessages.value ?: mutableListOf()
        currentList.add(message)
        _chatMessages.value = currentList
    }

    fun getSuggestionIds() = listOf(
        R.string.chatbot_suggestion_warrior1,
        R.string.chatbot_suggestion_back_pain,
        R.string.chatbot_suggestion_crow_pose,
        R.string.chatbot_suggestion_daily_yoga
    )
}