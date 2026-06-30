package com.naviapp.agent.ui.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naviapp.agent.data.*
import com.naviapp.agent.ui.ResultCache
import com.naviapp.agent.ui.map.HereMapComposable
import com.naviapp.agent.ui.map.MapRouteData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteResultsScreen(
    requestId: String,
    onBack: () -> Unit,
    onShowInsights: () -> Unit
) {
    val response = ResultCache.get(requestId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route Recommendation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onShowInsights) {
                        Text("Agent Insights", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (response == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No result found for $requestId", color = MaterialTheme.colorScheme.error)
            }
        } else {
            ResultContent(response = response, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun ResultContent(response: RouteResponseDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // HERE Map — at the top of the results
        val mapData = buildMapData(response)
        if (mapData != null) {
            HereMapComposable(
                routeData = mapData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }

        // Content cards below the map
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recommended Route Card
            RecommendedCard(response.recommendedRoute)

            // Traffic & Weather Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip("\uD83D\uDEA6 ${response.trafficSummary.level.uppercase()}", Modifier.weight(1f))
                SummaryChip("\u2601\uFE0F ${response.weatherSummary.conditions.take(20)}", Modifier.weight(1f))
            }

            // Weather alerts
            if (response.weatherSummary.alerts.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("\u26A0\uFE0F Weather Alerts", fontWeight = FontWeight.Bold)
                        response.weatherSummary.alerts.forEach { alert ->
                            Text("\u2022 $alert", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // POIs
            if (response.pois.isNotEmpty()) {
                Text("Points of Interest", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                response.pois.take(3).forEach { poi ->
                    val icon = when (poi.category) {
                        "fuel-station" -> "\u26FD"
                        "rest-area" -> "\uD83C\uDFD5\uFE0F"
                        "ev-charging" -> "\uD83D\uDD0C"
                        "restaurant" -> "\uD83C\uDF54"
                        else -> "\uD83D\uDCCD"
                    }
                    Text("$icon ${poi.name} (${poi.distanceFromRouteKm} km from route)", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Explanation
            ExplanationCard(response.explanation)

            // Alternatives
            if (response.alternatives.isNotEmpty()) {
                Text("Alternatives", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                response.alternatives.forEach { alt ->
                    AlternativeCard(alt)
                }
            }

            // Metadata footer
            Text(
                "Processed in ${response.metadata.processingTimeMs}ms | Mode: ${response.metadata.mode} | ${response.metadata.agentsConsulted.size} agents",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Builds map data from the route response.
 * Uses origin/destination from the request and POI locations as hints.
 * Returns null if coordinates are not available.
 */
private fun buildMapData(response: RouteResponseDto): MapRouteData? {
    // Extract origin/destination from POIs or use label-based lookup
    // The backend response doesn't directly include origin/dest coords in the top-level,
    // but we can infer from the request that was cached.
    // For now, use a city lookup from the route label.
    val originCoords = extractCoordsFromLabel(response.recommendedRoute.label, isOrigin = true)
    val destCoords = extractCoordsFromLabel(response.recommendedRoute.label, isOrigin = false)

    if (originCoords == null || destCoords == null) return null

    // Build polyline from POI locations as waypoints (simplified visualization)
    val polylinePoints = mutableListOf<Pair<Double, Double>>()
    polylinePoints.add(originCoords)
    response.pois.forEach { poi ->
        polylinePoints.add(poi.location.latitude to poi.location.longitude)
    }
    polylinePoints.add(destCoords)

    return MapRouteData(
        originLat = originCoords.first,
        originLng = originCoords.second,
        originLabel = extractCity(response.recommendedRoute.label, isOrigin = true),
        destLat = destCoords.first,
        destLng = destCoords.second,
        destLabel = extractCity(response.recommendedRoute.label, isOrigin = false),
        polylinePoints = if (polylinePoints.size >= 2) polylinePoints else null
    )
}

// Known city coordinates for map display
private val CITY_COORDS = mapOf(
    "berlin" to (52.5200 to 13.4050),
    "paris" to (48.8566 to 2.3522),
    "munich" to (48.1351 to 11.5820),
    "hamburg" to (53.5511 to 9.9937),
    "zurich" to (47.3769 to 8.5417),
    "stuttgart" to (48.7758 to 9.1829),
    "frankfurt" to (50.1109 to 8.6821),
    "cologne" to (50.9375 to 6.9603),
)

private fun extractCoordsFromLabel(label: String, isOrigin: Boolean): Pair<Double, Double>? {
    val lower = label.lowercase()
    // Route labels like "Via A2 / A4 (Berlin → Paris)"
    val parts = lower.split("→", "->", " to ")
    if (parts.size >= 2) {
        val city = if (isOrigin) parts[0] else parts[1]
        return CITY_COORDS.entries.firstOrNull { city.contains(it.key) }?.value
    }
    // Fallback: check whole label for city names
    return if (isOrigin) {
        CITY_COORDS.entries.firstOrNull { lower.contains(it.key) }?.value
    } else {
        // Return the second city found
        val found = CITY_COORDS.entries.filter { lower.contains(it.key) }
        found.getOrNull(1)?.value ?: found.firstOrNull()?.value
    }
}

private fun extractCity(label: String, isOrigin: Boolean): String {
    val parts = label.split("→", "->", " to ")
    if (parts.size >= 2) {
        val segment = if (isOrigin) parts[0] else parts[1]
        // Find city name in the segment
        CITY_COORDS.keys.forEach { city ->
            if (segment.lowercase().contains(city)) {
                return city.replaceFirstChar { it.uppercase() }
            }
        }
    }
    return if (isOrigin) "Origin" else "Destination"
}

@Composable
private fun RecommendedCard(route: RouteInfoDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("\u2B50 RECOMMENDED", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1565C0))
            Text(route.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("\u23F1 ${route.summary.durationMinutes} min")
                Text("\uD83D\uDCCF ${route.summary.distanceKm} km")
                Text("\uD83D\uDD52 ETA ${route.summary.eta}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { route.scores.overall.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF43A047),
                trackColor = Color(0xFFE0E0E0),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Confidence: ${(route.scores.overall * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Scores — Route: ${(route.scores.routing * 100).toInt()}% | Traffic: ${(route.scores.traffic * 100).toInt()}% | Weather: ${(route.scores.weather * 100).toInt()}% | POI: ${(route.scores.poi * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExplanationCard(explanation: ExplanationDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Why this route?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(explanation.summary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            explanation.factors.forEach { factor ->
                val icon = when (factor.factor) {
                    "traffic" -> "\uD83D\uDEA6"
                    "weather" -> "\uD83C\uDF27\uFE0F"
                    "duration" -> "\u23F1"
                    else -> "\uD83D\uDCCA"
                }
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("$icon ", style = MaterialTheme.typography.bodyMedium)
                    Column {
                        Text(
                            "${factor.factor.uppercase()} (${factor.influence})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(factor.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (explanation.tradeOffs.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Trade-offs: ${explanation.tradeOffs}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlternativeCard(route: RouteInfoDto) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(route.label, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("\u23F1 ${route.summary.durationMinutes} min")
                Text("\uD83D\uDCCF ${route.summary.distanceKm} km")
                Text("Score: ${(route.scores.overall * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}
