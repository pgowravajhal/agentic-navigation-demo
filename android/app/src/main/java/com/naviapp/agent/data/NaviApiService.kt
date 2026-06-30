package com.naviapp.agent.data

import retrofit2.http.*

interface NaviApiService {

    @GET("health")
    suspend fun getHealth(): HealthResponseDto

    @POST("recommend-route")
    suspend fun recommendRoute(@Body request: RouteRequestDto): RouteResponseDto

    @GET("agent-trace/{requestId}")
    suspend fun getAgentTrace(@Path("requestId") requestId: String): AgentTraceDto
}
