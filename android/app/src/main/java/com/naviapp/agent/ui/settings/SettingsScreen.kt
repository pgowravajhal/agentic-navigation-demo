package com.naviapp.agent.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naviapp.agent.data.ApiClient
import com.naviapp.agent.data.HealthResponseDto
import com.naviapp.agent.data.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var backendUrl by remember { mutableStateOf("") }
    var connectionStatus by remember { mutableStateOf<ConnectionState>(ConnectionState.Unknown) }
    var saved by remember { mutableStateOf(false) }

    // Load saved URL
    LaunchedEffect(Unit) {
        backendUrl = settingsStore.backendUrl.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backend URL
            Text("Backend URL", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = backendUrl,
                onValueChange = { backendUrl = it; saved = false; connectionStatus = ConnectionState.Unknown },
                label = { Text("Backend base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("http://10.0.2.2:8000") }
            )

            // Preset URLs
            Text("Quick presets:", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { backendUrl = "http://3.87.198.126:8000"; saved = false },
                    label = { Text("EC2") }
                )
                AssistChip(
                    onClick = { backendUrl = "http://10.0.2.2:8000"; saved = false },
                    label = { Text("Emulator") }
                )
                AssistChip(
                    onClick = { backendUrl = "http://localhost:8000"; saved = false },
                    label = { Text("Local") }
                )
            }

            // Save button
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            settingsStore.saveBackendUrl(backendUrl)
                            ApiClient.updateBaseUrl(backendUrl)
                            saved = true
                        }
                    }
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            connectionStatus = ConnectionState.Checking
                            try {
                                ApiClient.updateBaseUrl(backendUrl)
                                val health = ApiClient.getApi().getHealth()
                                connectionStatus = ConnectionState.Connected(health)
                            } catch (e: Exception) {
                                connectionStatus = ConnectionState.Failed(e.message ?: "Unknown error")
                            }
                        }
                    }
                ) {
                    Text("Test Connection")
                }
            }

            if (saved) {
                Text("\u2705 Saved!", color = Color(0xFF43A047))
            }

            // Connection status
            when (val status = connectionStatus) {
                is ConnectionState.Unknown -> {}
                is ConnectionState.Checking -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                is ConnectionState.Connected -> {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("\u2705 Connected", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("Status: ${status.health.status}", style = MaterialTheme.typography.bodySmall)
                            Text("Mode: ${status.health.mode}", style = MaterialTheme.typography.bodySmall)
                            Text("Version: ${status.health.version}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is ConnectionState.Failed -> {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("\u274C Connection Failed", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text(status.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // App info
            Text("Agentic Navigator v0.1.0", style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("AWS Hackathon — V-Cycle Acceleration", style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

private sealed class ConnectionState {
    object Unknown : ConnectionState()
    object Checking : ConnectionState()
    data class Connected(val health: HealthResponseDto) : ConnectionState()
    data class Failed(val error: String) : ConnectionState()
}
