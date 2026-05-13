package com.hadat.aiyoga.data.api

import com.hadat.aiyoga.chatbot_ai.AiSequencePlan


data class ChatRequest(
    val question: String,
    val weight: Float
)

data class ChatResponse(
    val success: Boolean = false,
    val mode: String = "",
    val answer: String = "",
    val data: AiSequencePlan? = null
)
