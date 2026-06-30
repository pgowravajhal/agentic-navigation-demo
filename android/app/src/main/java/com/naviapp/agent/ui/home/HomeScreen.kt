package com.naviapp.agent.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naviapp.agent.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settingsStore: SettingsStore,
    onRouteRecommended: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(settingsStore))
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when request completes
    LaunchedEffect(uiState.requestId) {
        uiState.requestId?.let { requestId ->
            onRouteRecommended(requestId)
            viewModel.clearNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\uD83E\uDDED Agentic Navigator") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Origin section
            Text("Origin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = uiState.originLabel,
                onValueChange = viewModel::setOriginLabel,
                label = { Text("Enter origin") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { viewModel.setPreset("Berlin", isOrigin = true) }, label = { Text("Berlin") })
                AssistChip(onClick = { viewModel.setPreset("Munich", isOrigin = true) }, label = { Text("Munich") })
                AssistChip(onClick = { viewModel.setPreset("Stuttgart", isOrigin = true) }, label = { Text("Stuttgart") })
            }

            // Destination section
            Text("Destination", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = uiState.destinationLabel,
                onValueChange = viewModel::setDestinationLabel,
                label = { Text("Enter destination") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { viewModel.setPreset("Paris", isOrigin = false) }, label = { Text("Paris") })
                AssistChip(onClick = { viewModel.setPreset("Hamburg", isOrigin = false) }, label = { Text("Hamburg") })
                AssistChip(onClick = { viewModel.setPreset("Zurich", isOrigin = false) }, label = { Text("Zurich") })
            }

            // Demo mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Demo Mode (mock data)")
                Switch(
                    checked = uiState.demoMode,
                    onCheckedChange = viewModel::setDemoMode
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recommend button
            Button(
                onClick = viewModel::recommendRoute,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading
                        && uiState.originLabel.isNotBlank()
                        && uiState.destinationLabel.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Consulting Agents...")
                } else {
                    Text("\uD83D\uDE80 Recommend Route", fontWeight = FontWeight.Bold)
                }
            }

            // Error display
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Text(
                text = "Multi-agent navigation \u2022 AWS Hackathon Demo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
