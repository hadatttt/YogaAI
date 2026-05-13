package com.hadat.aiyoga.chatbot_ai

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.api.ChatRequest
import com.hadat.aiyoga.data.api.RetrofitClient
import com.hadat.aiyoga.data.firestore.repository.HealthProfileRepository
import com.hadat.aiyoga.utils.service.AppPreferences
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotViewModel : BaseViewModel() {

    private val healthProfileRepository = HealthProfileRepository()
    private val _chatMessages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val chatMessages: LiveData<MutableList<ChatMessage>> get() = _chatMessages

    fun sendMessage(context: Context, text: String) {
        if (text.isBlank()) return
        updateMessageList(ChatMessage(text, MessageType.USER))
        fetchAiResponse(context, text)
    }

    private fun fetchAiResponse(context: Context, userText: String) {
        setTypingStatus(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = AppPreferences.getUserId(context) ?: ""
                val profile = if (userId.isNotEmpty()) {
                    healthProfileRepository.getProfile(userId)
                } else null
                val userWeight = profile?.weight ?: 60f
                val result = RetrofitClient.apiService.sendMessage(
                    ChatRequest(userText, userWeight)
                )
                withContext(Dispatchers.Main) {
                    setTypingStatus(false)
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
        _chatMessages.postValue(currentList)
    }

    private fun updateMessageList(message: ChatMessage) {
        val currentList = _chatMessages.value ?: mutableListOf()
        currentList.add(message)
        _chatMessages.postValue(currentList)
    }

    fun getSuggestionIds() = listOf(
        R.string.chatbot_suggestion_1,
        R.string.chatbot_suggestion_2,
        R.string.chatbot_suggestion_3,
        R.string.chatbot_suggestion_4
    )
}