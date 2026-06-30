package com.naviapp.agent.data

import com.google.gson.annotations.SerializedName

/**
 * Data models matching the backend /recommend-route response exactly.
 * Uses Gson @SerializedName for snake_case mapping.
 */

data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val label: String? = null
)

data class PreferencesDto(
    @SerializedName("transport_mode") val transportMode: String = "car",
    @SerializedName("avoid_highways") val avoidHighways: Boolean = false,
    val prioritize: String = "fastest"
)

data class RouteRequestDto(
    val origin: LocationDto,
    val destination: LocationDto,
    val preferences: PreferencesDto? = null,
    @SerializedName("demo_mode") val demoMode: Boolean = true
)

// --- Response models ---

data class RouteSummaryDto(
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    val eta: String
)

data class RouteScoresDto(
    val overall: Double,
    val routing: Double,
    val traffic: Double,
    val weather: Double,
    val poi: Double = 0.0
)

data class RouteInfoDto(
    val id: String,
    val label: String,
    val summary: RouteSummaryDto,
    val scores: RouteScoresDto,
    @SerializedName("is_recommended") val isRecommended: Boolean
)

data class ExplanationFactorDto(
    val factor: String,
    val influence: String,
    val detail: String
)

data class ExplanationDto(
    val summary: String,
    val factors: List<ExplanationFactorDto>,
    @SerializedName("trade_offs") val tradeOffs: String,
    val confidence: Double
)

data class TrafficSummaryDto(
    val level: String,
    val summary: String
)

data class WeatherSummaryDto(
    val conditions: String,
    val alerts: List<String>
)

data class PoiDto(
    val id: String,
    val name: String,
    val category: String,
    val location: LocationDto,
    @SerializedName("distance_from_route_km") val distanceFromRouteKm: Double,
    val address: String? = null,
    @SerializedName("open_now") val openNow: Boolean? = null
)

data class ToolCallDto(
    val tool: String,
    @SerializedName("duration_ms") val durationMs: Int = 0,
    val status: String
)

data class AgentEntryDto(
    val name: String,
    val status: String,
    @SerializedName("duration_ms") val durationMs: Int,
    @SerializedName("input_summary") val inputSummary: String,
    @SerializedName("output_summary") val outputSummary: String,
    val score: Double? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCallDto> = emptyList()
)

data class FinalDecisionDto(
    val recommended: String?,
    val confidence: Double,
    val method: String
)

data class AgentTraceDto(
    @SerializedName("request_id") val requestId: String,
    val timestamp: String,
    @SerializedName("total_duration_ms") val totalDurationMs: Int,
    val agents: List<AgentEntryDto>,
    @SerializedName("final_decision") val finalDecision: FinalDecisionDto
)

data class MetadataDto(
    @SerializedName("processing_time_ms") val processingTimeMs: Int,
    @SerializedName("agents_consulted") val agentsConsulted: List<String>,
    @SerializedName("failed_agents") val failedAgents: List<String> = emptyList(),
    val mode: String
)

data class RouteResponseDto(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("recommended_route") val recommendedRoute: RouteInfoDto,
    val alternatives: List<RouteInfoDto>,
    val explanation: ExplanationDto,
    @SerializedName("traffic_summary") val trafficSummary: TrafficSummaryDto,
    @SerializedName("weather_summary") val weatherSummary: WeatherSummaryDto,
    val pois: List<PoiDto> = emptyList(),
    @SerializedName("agent_trace") val agentTrace: AgentTraceDto,
    val metadata: MetadataDto
)

data class HealthResponseDto(
    val status: String,
    val mode: String,
    val version: String
)
