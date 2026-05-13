package com.hadat.aiyoga.chatbot_ai

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.api.ChatRequest
import com.hadat.aiyoga.data.api.RetrofitClient
import com.hadat.aiyoga.data.firestore.repository.HealthProfileRepository
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.yogautils.HealthCalculatorUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ChatbotViewModel : BaseViewModel() {

    private val healthProfileRepository = HealthProfileRepository()
    private val gson = Gson()
    private val _chatMessages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val chatMessages: LiveData<MutableList<ChatMessage>> get() = _chatMessages

    fun sendMessage(context: Context, text: String) {
        if (text.isBlank()) return
        updateMessageList(ChatMessage(text, MessageType.USER))
        fetchAiResponse(context, text)
    }

    fun clearConversation() {
        _chatMessages.value = mutableListOf()
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
                    val aiSequence = if (result.mode == "sequence") {
                        buildAiSequenceResponse(context, result.data, result.answer, userWeight)
                    } else {
                        null
                    }

                    if (aiSequence != null) {
                        updateMessageList(ChatMessage(gson.toJson(aiSequence), MessageType.BOT))
                    } else if (result.mode == "sequence") {
                        updateMessageList(ChatMessage(context.getString(R.string.ai_sequence_no_data), MessageType.BOT))
                    } else {
                        updateMessageList(ChatMessage(result.answer, MessageType.BOT))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setTypingStatus(false)
                    updateMessageList(ChatMessage(context.getString(R.string.ai_server_connection_error), MessageType.BOT))
                }
            }
        }
    }

    private fun buildAiSequenceResponse(
        context: Context,
        data: AiSequencePlan?,
        answer: String,
        weight: Float
    ): AiSequenceResponse? {
        val plan = data ?: parsePlanFromAnswer(answer)
        val poses = plan?.poses
            ?.filter { it.id >= 0 && it.minutes > 0f }
            ?.take(10)
            .orEmpty()

        if (poses.isEmpty()) return null

        var totalCalories = 0f
        val sequencePoses = poses.mapNotNull { aiPose ->
            val pose = YogaDataUtils.getPoseById(aiPose.id) ?: return@mapNotNull null
            val durationSeconds = (aiPose.minutes * 60).roundToInt().coerceAtLeast(1)
            val met = YogaDataUtils.getMetValue(aiPose.id)
            totalCalories += HealthCalculatorUtils.calculateWorkoutCaloriesByMet(
                met = met,
                weight = weight,
                durationSec = durationSeconds
            )

            SequenceModel(
                id = pose.id,
                name = pose.name,
                category = YogaDataUtils.getLocalizedCategory(context, pose.category),
                duration = formatDuration(durationSeconds),
                photoUrl = pose.photo_url
            )
        }

        if (sequencePoses.isEmpty()) return null

        return AiSequenceResponse(
            title = context.getString(R.string.ai_sequence_generated_title),
            total_calories = totalCalories,
            poses = sequencePoses
        )
    }

    private fun parsePlanFromAnswer(answer: String): AiSequencePlan? {
        if (answer.isBlank()) return null
        return runCatching { gson.fromJson(answer, AiSequencePlan::class.java) }.getOrNull()
    }

    private fun formatDuration(durationSeconds: Int): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
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
