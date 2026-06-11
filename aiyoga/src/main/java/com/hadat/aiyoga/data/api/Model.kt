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

data class RecommendRequest(
    val ids: List<Int>,
    val pose_ids: List<Int> = ids
)

data class RecommendResponse(
    val ids: List<Int> = emptyList()
)
