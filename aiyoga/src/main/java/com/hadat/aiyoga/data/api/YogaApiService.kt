package com.hadat.aiyoga.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface YogaApiService {
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse

    @POST("recommend")
    suspend fun recommend(@Body request: RecommendRequest): RecommendResponse

    @POST("ingest")
    suspend fun ingestData(@Body data: Any): Any
}
