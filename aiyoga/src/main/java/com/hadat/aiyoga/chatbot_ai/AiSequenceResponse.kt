package com.hadat.aiyoga.chatbot_ai

import com.hadat.aiyoga.sequence.SequenceModel

data class AiSequenceResponse(
    val title: String = "",
    val total_calories: Float = 0f,
    val poses: List<SequenceModel> = emptyList()
)