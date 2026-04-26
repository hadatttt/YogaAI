package com.hadat.aiyoga.chatbot_ai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class MessageType {
    USER, BOT, TYPING
}

@Parcelize
data class ChatMessage(
    val content: String = "",
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable