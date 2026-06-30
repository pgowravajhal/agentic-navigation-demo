package com.naviapp.agent.ui.insights

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
import com.naviapp.agent.data.AgentEntryDto
import com.naviapp.agent.data.AgentTraceDto
import com.naviapp.agent.ui.ResultCache

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentInsightsScreen(
    requestId: String,
    onBack: () -> Unit
) {
    val response = ResultCache.get(requestId)
    val trace = response?.agentTrace

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF263238),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (trace == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No trace data available for $requestId")
            }
        } else {
            TraceContent(trace = trace, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun TraceContent(trace: AgentTraceDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header card
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F))) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Request: ${trace.requestId}", color = Color(0xFF80CBC4), style = MaterialTheme.typography.labelMedium)
                Text("Total: ${trace.totalDurationMs}ms", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Decision: ${trace.finalDecision.recommended ?: "none"} (${(trace.finalDecision.confidence * 100).toInt()}%)",
                    color = Color(0xFFA5D6A7)
                )
                Text("Method: ${trace.finalDecision.method}", color = Color(0xFFB0BEC5), style = MaterialTheme.typography.bodySmall)
            }
        }

        // Agent cards
        trace.agents.forEach { agent ->
            AgentCard(agent)
        }
    }
}

@Composable
private fun AgentCard(agent: AgentEntryDto) {
    val icon = when (agent.name) {
        "routing" -> "\uD83D\uDEE3\uFE0F"
        "traffic" -> "\uD83D\uDEA6"
        "weather" -> "\uD83C\uDF27\uFE0F"
        "poi" -> "\uD83D\uDCCD"
        "recommendation" -> "\uD83E\uDD16"
        else -> "\uD83D\uDCCA"
    }
    val statusIcon = if (agent.status == "success") "\u2705" else "\u274C"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "$icon ${agent.name.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${agent.durationMs}ms", color = Color(0xFFFFCC02), style = MaterialTheme.typography.labelSmall)
                    Text(statusIcon)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Details
            Text("Input: ${agent.inputSummary}", color = Color(0xFFB0BEC5), style = MaterialTheme.typography.bodySmall)
            Text("Output: ${agent.outputSummary}", color = Color(0xFFCFD8DC), style = MaterialTheme.typography.bodySmall)

            if (agent.score != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Score: ${String.format("%.2f", agent.score)}", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold)
            }

            if (agent.toolCalls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                agent.toolCalls.forEach { tool ->
                    Text(
                        "  \u21B3 Tool: ${tool.tool} [${tool.status}] ${tool.durationMs}ms",
                        color = Color(0xFF90A4AE),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
