package com.hadat.aiyoga.data.api


data class ChatRequest(
    val question: String,
    val weight: Float
)

data class ChatResponse(val answer: String)