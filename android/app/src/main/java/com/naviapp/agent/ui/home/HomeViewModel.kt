package com.naviapp.agent.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naviapp.agent.data.*
import com.naviapp.agent.ui.ResultCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val originLabel: String = "Berlin",
    val destinationLabel: String = "",
    val demoMode: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val requestId: String? = null
)

// Preset coordinates for demo cities
private val CITY_COORDS = mapOf(
    "Berlin" to (52.5200 to 13.4050),
    "Paris" to (48.8566 to 2.3522),
    "Munich" to (48.1351 to 11.5820),
    "Hamburg" to (53.5511 to 9.9937),
    "Zurich" to (47.3769 to 8.5417),
    "Stuttgart" to (48.7758 to 9.1829),
)

class HomeViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Initialize API client from saved settings
        viewModelScope.launch {
            val url = settingsStore.backendUrl.first()
            ApiClient.updateBaseUrl(url)
        }
    }

    fun setOriginLabel(label: String) {
        _uiState.update { it.copy(originLabel = label, error = null) }
    }

    fun setDestinationLabel(label: String) {
        _uiState.update { it.copy(destinationLabel = label, error = null) }
    }

    fun setDemoMode(enabled: Boolean) {
        _uiState.update { it.copy(demoMode = enabled) }
    }

    fun setPreset(city: String, isOrigin: Boolean) {
        if (isOrigin) {
            _uiState.update { it.copy(originLabel = city, error = null) }
        } else {
            _uiState.update { it.copy(destinationLabel = city, error = null) }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(requestId = null) }
    }

    fun recommendRoute() {
        val state = _uiState.value
        val originCoords = CITY_COORDS[state.originLabel] ?: (52.52 to 13.405)
        val destCoords = CITY_COORDS[state.destinationLabel] ?: (48.8566 to 2.3522)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Ensure URL is current
                val url = settingsStore.backendUrl.first()
                ApiClient.updateBaseUrl(url)

                val request = RouteRequestDto(
                    origin = LocationDto(originCoords.first, originCoords.second, state.originLabel),
                    destination = LocationDto(destCoords.first, destCoords.second, state.destinationLabel),
                    preferences = PreferencesDto(),
                    demoMode = state.demoMode
                )

                val response = ApiClient.getApi().recommendRoute(request)
                ResultCache.put(response.requestId, response)

                _uiState.update { it.copy(isLoading = false, requestId = response.requestId) }
            } catch (e: java.net.ConnectException) {
                _uiState.update { it.copy(isLoading = false, error = "Cannot connect to backend. Check Settings.") }
            } catch (e: java.net.SocketTimeoutException) {
                _uiState.update { it.copy(isLoading = false, error = "Request timed out. Backend may be slow.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }
}

class HomeViewModelFactory(private val settingsStore: SettingsStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(settingsStore) as T
    }
}
