package com.hadat.aiyoga.chatbot_ai

import com.hadat.aiyoga.sequence.SequenceModel

data class AiSequencePlan(
    val poses: List<AiSequencePose> = emptyList()
)

data class AiSequencePose(
    val id: Int = 0,
    val minutes: Float = 0f
)

data class AiSequenceResponse(
    val title: String = "",
    val total_calories: Float = 0f,
    val poses: List<SequenceModel> = emptyList()
)
