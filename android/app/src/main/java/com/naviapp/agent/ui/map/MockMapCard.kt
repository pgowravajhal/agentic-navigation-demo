package com.naviapp.agent.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A Compose-based mock map card that visually represents the route
 * without requiring any external map SDK.
 * Designed to be replaced with a real map (HERE, Mapbox, etc.) in the future.
 */
@Composable
fun MockMapCard(
    origin: String,
    destination: String,
    distanceKm: Double,
    durationMinutes: Int,
    eta: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text(
                "\uD83D\uDDFA\uFE0F Map View — Android vECU Ready",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF3949AB),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visual route representation
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Origin marker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFF43A047), CircleShape)
                    )
                    Text(origin, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                }

                // Route line
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    drawLine(
                        color = Color(0xFF1565C0),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                        pathEffect = pathEffect
                    )
                }

                // Destination marker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFFD32F2F), CircleShape)
                    )
                    Text(destination, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricBadge(label = "Distance", value = "${distanceKm} km")
                MetricBadge(label = "Duration", value = "${durationMinutes} min")
                MetricBadge(label = "ETA", value = eta)
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5C6BC0))
    }
}
