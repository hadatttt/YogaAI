package com.hadat.aiyoga.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface YogaApiService {
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse

    // Nếu sau này Đạt muốn gửi thêm dữ liệu tập luyện vào hệ thống RAG
    @POST("ingest")
    suspend fun ingestData(@Body data: Any): Any
}