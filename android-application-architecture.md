# Android Application Architecture
## Accelerating the V-Cycle with Agentic AI — Navigation Application

| Field | Value |
|-------|-------|
| Document Version | 1.0 |
| Status | Draft |
| Date | June 30, 2026 |
| Platform | Android (API 30+) |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + Repository + Clean Architecture |
| Target | Android Emulator (portable to AAOS / vECU) |

---

## Table of Contents

1. [Application Architecture](#1-application-architecture)
2. [Package Structure](#2-package-structure)
3. [Screen Specifications](#3-screen-specifications)
4. [Navigation Flow](#4-navigation-flow)
5. [Data Models](#5-data-models)
6. [API Integration Design](#6-api-integration-design)
7. [State Management](#7-state-management)
8. [Error Handling](#8-error-handling)
9. [Future AAOS Compatibility](#9-future-aaos-compatibility)

---

## 1. Application Architecture

### 1.1 Layer Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Jetpack Compose Screens                 │    │
│  │  Home │ RouteResults │ Explanation │ AgentInsights │ │    │
│  └────────────────────────┬────────────────────────────┘    │
│                           │ observes StateFlow               │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │                   ViewModels                         │    │
│  │  HomeVM │ RouteVM │ ExplanationVM │ InsightsVM      │    │
│  └────────────────────────┬────────────────────────────┘    │
└───────────────────────────┼─────────────────────────────────┘
                            │ calls suspend functions
┌───────────────────────────┼─────────────────────────────────┐
│                    DOMAIN LAYER                              │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │                  Use Cases                           │    │
│  │  RecommendRoute │ SearchPOI │ GetTrace │ ResetDemo  │    │
│  └────────────────────────┬────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │              Domain Models                           │    │
│  │  Route │ Recommendation │ AgentResult │ POI          │    │
│  └─────────────────────────────────────────────────────┘    │
└───────────────────────────┼─────────────────────────────────┘
                            │ repository interfaces
┌───────────────────────────┼─────────────────────────────────┐
│                    DATA LAYER                                │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │               Repositories (Impl)                    │    │
│  │  NavigationRepository │ SettingsRepository           │    │
│  └────────────────────────┬────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │              Data Sources                            │    │
│  │  RemoteDataSource (Retrofit) │ LocalDataSource      │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Architectural Principles

| Principle | Application |
|-----------|------------|
| **MVVM** | ViewModels expose UI state via StateFlow; Compose screens observe reactively |
| **Repository Pattern** | Single source of truth; abstracts remote vs. local data |
| **Dependency Injection** | Hilt provides all dependencies; enables testing and AAOS portability |
| **Unidirectional Data Flow** | Events flow up (user actions), state flows down (StateFlow) |
| **Separation of Concerns** | UI knows nothing about networking; data layer knows nothing about UI |
| **Coroutines** | All async operations use structured concurrency via viewModelScope |

### 1.3 Key Libraries

| Category | Library | Purpose |
|----------|---------|---------|
| UI | Jetpack Compose + Material 3 | Declarative UI |
| Navigation | Compose Navigation | Screen routing |
| Networking | Retrofit + OkHttp + Moshi | REST API calls |
| DI | Hilt | Dependency injection |
| Async | Kotlin Coroutines + Flow | Concurrency |
| Map | HERE SDK for Android (Lite) or MapLibre | Map rendering |
| State | StateFlow + SavedStateHandle | Reactive state |
| Image | Coil | Image loading (POI icons, weather) |

---

## 2. Package Structure

```
com.naviapp.agent/
├── NaviApplication.kt                  # Application class, Hilt entry point
│
├── di/                                 # Dependency Injection modules
│   ├── AppModule.kt                   # App-scoped bindings (settings, HTTP client)
│   ├── NetworkModule.kt               # Retrofit, OkHttp, interceptors
│   └── RepositoryModule.kt            # Repository bindings
│
├── ui/                                 # Presentation Layer
│   ├── navigation/
│   │   └── NavGraph.kt               # Compose Navigation graph
│   ├── theme/
│   │   ├── Theme.kt                  # Material 3 theme
│   │   ├── Color.kt                  # Color palette
│   │   └── Type.kt                   # Typography
│   ├── components/                    # Reusable composables
│   │   ├── MapView.kt               # Map composable wrapper
│   │   ├── RouteCard.kt             # Route summary card
│   │   ├── AgentScoreBar.kt         # Agent score visualization
│   │   ├── SearchBar.kt             # Location search input
│   │   ├── LoadingOverlay.kt        # Loading state
│   │   └── ErrorBanner.kt           # Error display
│   ├── home/
│   │   ├── HomeScreen.kt            # Home screen composable
│   │   └── HomeViewModel.kt         # Home state + actions
│   ├── results/
│   │   ├── RouteResultsScreen.kt    # Results screen composable
│   │   └── RouteResultsViewModel.kt # Results state + actions
│   ├── explanation/
│   │   ├── ExplanationScreen.kt     # Explanation screen composable
│   │   └── ExplanationViewModel.kt  # Explanation state
│   ├── insights/
│   │   ├── AgentInsightsScreen.kt   # Developer/demo screen
│   │   └── AgentInsightsViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt         # Settings screen composable
│       └── SettingsViewModel.kt      # Settings state + persistence
│
├── domain/                            # Domain Layer
│   ├── model/                        # Domain models
│   │   ├── Location.kt
│   │   ├── Route.kt
│   │   ├── Recommendation.kt
│   │   ├── AgentResult.kt
│   │   ├── TrafficInfo.kt
│   │   ├── WeatherInfo.kt
│   │   ├── PointOfInterest.kt
│   │   └── Explanation.kt
│   ├── usecase/                      # Use cases
│   │   ├── RecommendRouteUseCase.kt
│   │   ├── SearchPoiUseCase.kt
│   │   ├── GetAgentTraceUseCase.kt
│   │   └── ResetDemoUseCase.kt
│   └── repository/                   # Repository interfaces
│       ├── NavigationRepository.kt
│       └── SettingsRepository.kt
│
├── data/                              # Data Layer
│   ├── remote/                       # Network
│   │   ├── api/
│   │   │   └── NaviApiService.kt    # Retrofit interface
│   │   ├── dto/                     # Data Transfer Objects
│   │   │   ├── RouteRequestDto.kt
│   │   │   ├── RouteResponseDto.kt
│   │   │   ├── PoiRequestDto.kt
│   │   │   ├── PoiResponseDto.kt
│   │   │   ├── TraceResponseDto.kt
│   │   │   └── HealthResponseDto.kt
│   │   └── mapper/                  # DTO → Domain mappers
│   │       ├── RouteMapper.kt
│   │       ├── PoiMapper.kt
│   │       └── TraceMapper.kt
│   ├── local/                       # Local storage
│   │   ├── PreferencesDataStore.kt  # Settings persistence
│   │   └── RecentDestinationStore.kt
│   └── repository/                  # Repository implementations
│       ├── NavigationRepositoryImpl.kt
│       └── SettingsRepositoryImpl.kt
│
└── util/                             # Utilities
    ├── NetworkMonitor.kt            # Connectivity observer
    ├── Result.kt                    # Result wrapper (Success/Error/Loading)
    └── Extensions.kt               # Kotlin extensions
```

---

## 3. Screen Specifications

### 3.1 Home Screen

**Purpose:** Entry point. User specifies origin and destination to request a route recommendation.

```
┌────────────────────────────────────┐
│  🧭 Agentic Navigator             │
├────────────────────────────────────┤
│                                    │
│  ┌──────────────────────────────┐  │
│  │ 📍 Origin                    │  │
│  │    [My Location         ▼]   │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ 🏁 Destination               │  │
│  │    [Enter destination... ]    │  │
│  └──────────────────────────────┘  │
│                                    │
│  ── Recent Destinations ────────── │
│  │ 📍 Paris, France             │  │
│  │ 📍 Munich, Germany           │  │
│  │ 📍 Zurich, Switzerland       │  │
│                                    │
│  ── Preferences ────────────────── │
│  │ Mode: [Fastest ▼]           │  │
│  │ Avoid highways: [ ]          │  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │      🚀 Find Route           │  │
│  └──────────────────────────────┘  │
│                                    │
│  [⚙ Settings]                      │
└────────────────────────────────────┘
```

**UI State:**

| State | Display |
|-------|---------|
| `Idle` | Empty form, recent destinations visible |
| `Loading` | "Find Route" shows spinner, form disabled |
| `Error` | Error banner at top with retry action |

**User Actions:**
- Enter/select origin (text input, "My Location" button, or recent)
- Enter/select destination (text input or recent)
- Set preferences (fastest/shortest, avoid highways)
- Tap "Find Route" → triggers recommendation request
- Tap Settings → navigate to Settings screen

---

### 3.2 Route Results Screen

**Purpose:** Display the recommended route on an interactive map with key metrics, alternatives, and summary information.

```
┌────────────────────────────────────┐
│ ← Back              Agent Insights │
├────────────────────────────────────┤
│                                    │
│  ┌──────────────────────────────┐  │
│  │                              │  │
│  │        INTERACTIVE MAP       │  │
│  │                              │  │
│  │    [Route polylines shown]   │  │
│  │    [Origin/dest markers]     │  │
│  │    [POI markers]             │  │
│  │                              │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ ★ RECOMMENDED: Via A2/A4     │  │
│  │ ⏱ 6h 00min  📏 1,050 km     │  │
│  │ 🚦 Light traffic             │  │
│  │ 🌤 Partly cloudy, light rain │  │
│  │ Confidence: ████████░░ 87%   │  │
│  │                              │  │
│  │ [Why this route? ▼]          │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ Alternative: Via A3/A6       │  │
│  │ ⏱ 6h 50min  📏 980 km       │  │
│  │ 🚦 Heavy traffic             │  │
│  │ Score: ████████░░░ 72%       │  │
│  └──────────────────────────────┘  │
│                                    │
└────────────────────────────────────┘
```

**UI State:**

| State | Display |
|-------|---------|
| `Loading` | Map visible, bottom sheet shows skeleton loading |
| `Success` | Full route data, map with polylines, recommendation card |
| `PartialSuccess` | Recommendation shown with "Some data unavailable" note |
| `Error` | Error state with retry option |

**User Actions:**
- Pan/zoom map
- Tap alternative route → highlights on map, shows comparison
- Tap "Why this route?" → navigate to Explanation screen
- Tap "Agent Insights" → navigate to Agent Insights screen
- Tap POI marker → show POI detail popup
- Tap back → return to Home

---

### 3.3 Route Explanation Screen

**Purpose:** Present the natural-language explanation of why the route was recommended, with factor breakdown.

```
┌────────────────────────────────────┐
│ ← Back         Route Explanation   │
├────────────────────────────────────┤
│                                    │
│  ┌──────────────────────────────┐  │
│  │ "Route B was selected        │  │
│  │  because it avoids heavy     │  │
│  │  traffic and severe rain     │  │
│  │  while increasing travel     │  │
│  │  time by only three          │  │
│  │  minutes."                   │  │
│  └──────────────────────────────┘  │
│                                    │
│  ── Factors Considered ─────────── │
│                                    │
│  🚦 Traffic         Influence: HIGH│
│  Route 2 has major congestion on   │
│  A3 adding ~45 minutes.           │
│                                    │
│  ⏱ Duration         Influence: HIGH│
│  Despite being 70km longer,        │
│  Route 1 arrives 50 min earlier.   │
│                                    │
│  🌧 Weather          Influence: LOW│
│  Light rain on both routes — no    │
│  material safety difference.       │
│                                    │
│  ── Trade-offs ─────────────────── │
│                                    │
│  Route 2 is shorter and more fuel- │
│  efficient, but current traffic    │
│  makes it significantly slower.    │
│                                    │
│  ── Confidence ─────────────────── │
│  Overall: 87%                      │
│  ████████████████████░░░           │
│                                    │
└────────────────────────────────────┘
```

**UI State:**

| State | Display |
|-------|---------|
| `Success` | Full explanation with factors and trade-offs |
| `Partial` | Summary available but some factors missing |

**User Actions:**
- Read explanation
- Tap back → return to Route Results

---

### 3.4 Agent Insights Screen (Demo/Developer)

**Purpose:** Show detailed agent trace data for hackathon demonstration. Reveals the multi-agent collaboration happening behind the scenes.

```
┌────────────────────────────────────┐
│ ← Back            Agent Insights   │
├────────────────────────────────────┤
│  Request: req-abc123               │
│  Total time: 4,500ms              │
│  Mode: live                        │
├────────────────────────────────────┤
│                                    │
│  ▼ 🛣 Routing Agent (1,200ms) ✅   │
│  ┌──────────────────────────────┐  │
│  │ Input: Berlin → Paris, car   │  │
│  │ Output: 3 candidate routes   │  │
│  │ Score: 0.85                  │  │
│  │ Tool: here_routing (1,100ms) │  │
│  └──────────────────────────────┘  │
│                                    │
│  ▼ 🚦 Traffic Agent (800ms) ✅     │
│  ┌──────────────────────────────┐  │
│  │ Input: 3 route corridors     │  │
│  │ Output: R1 clear, R2 heavy   │  │
│  │ Score: 0.90                  │  │
│  │ Tool: here_traffic (700ms)   │  │
│  └──────────────────────────────┘  │
│                                    │
│  ▼ 🌧 Weather Agent (600ms) ✅     │
│  ┌──────────────────────────────┐  │
│  │ Input: 9 waypoints           │  │
│  │ Output: Light rain Cologne   │  │
│  │ Score: 0.70                  │  │
│  │ Tool: weather_forecast       │  │
│  └──────────────────────────────┘  │
│                                    │
│  ▼ 📍 POI Agent (500ms) ✅         │
│  ┌──────────────────────────────┐  │
│  │ Input: 3 corridors           │  │
│  │ Output: 15 POIs found        │  │
│  │ Score: 0.75                  │  │
│  │ Tool: here_places (400ms)    │  │
│  └──────────────────────────────┘  │
│                                    │
│  ▼ 🤖 Recommendation (1,400ms) ✅  │
│  ┌──────────────────────────────┐  │
│  │ Decision: Route-1            │  │
│  │ Method: weighted_score       │  │
│  │ Confidence: 0.87             │  │
│  └──────────────────────────────┘  │
│                                    │
└────────────────────────────────────┘
```

**UI State:**

| State | Display |
|-------|---------|
| `Loading` | Fetching trace data spinner |
| `Success` | Expandable agent cards with full trace |
| `NotFound` | "No trace data available" message |

**User Actions:**
- Expand/collapse individual agent cards
- Tap back → return to Route Results

---

### 3.5 Settings Screen

**Purpose:** Configure backend connection, mode, and debug options.

```
┌────────────────────────────────────┐
│ ← Back                  Settings   │
├────────────────────────────────────┤
│                                    │
│  ── Connection ─────────────────── │
│  Backend URL                       │
│  [http://10.0.2.2:8000    ]       │
│                                    │
│  ── Mode ───────────────────────── │
│  Demo Mode          [Toggle ON ]   │
│  Mock Mode          [Toggle OFF]   │
│                                    │
│  ── Demo Controls ──────────────── │
│  Scenario                          │
│  [berlin-to-paris-congestion ▼]   │
│                                    │
│  [🔄 Reset Demo]                   │
│                                    │
│  ── Debug ──────────────────────── │
│  Verbose Logging    [Toggle OFF]   │
│  Show Agent Insights [Toggle ON ]  │
│                                    │
│  ── Info ───────────────────────── │
│  App Version: 0.1.0               │
│  Backend Status: ✅ Connected      │
│                                    │
└────────────────────────────────────┘
```

**UI State:**

| State | Display |
|-------|---------|
| `Connected` | Green status indicator |
| `Disconnected` | Red status, "Check URL" message |
| `Resetting` | Spinner on Reset Demo button |

**User Actions:**
- Edit backend URL
- Toggle demo/mock modes
- Select scenario
- Tap "Reset Demo" → calls POST /demo/reset
- Toggle debug options
- Tap back → return to Home

---

## 4. Navigation Flow

### 4.1 Screen Navigation Graph

```
                    ┌──────────┐
                    │  Home    │
                    └────┬─────┘
                         │
                    "Find Route"
                         │
                         ▼
                    ┌──────────────┐
             ┌──── │ Route Results │ ────┐
             │     └──────────────┘     │
             │            │              │
      "Why this      "Agent           tap
       route?"      Insights"        back
             │            │              │
             ▼            ▼              ▼
      ┌─────────────┐ ┌──────────────┐ (Home)
      │ Explanation │ │Agent Insights│
      └─────────────┘ └──────────────┘

  Settings is accessible from Home via gear icon.

      ┌──────────┐      ┌──────────┐
      │  Home    │ ───► │ Settings │
      └──────────┘ ◄─── └──────────┘
```

### 4.2 Navigation Routes

| Route | Path | Arguments |
|-------|------|-----------|
| Home | `/home` | None |
| Route Results | `/results/{requestId}` | `requestId: String` |
| Explanation | `/explanation/{requestId}` | `requestId: String` |
| Agent Insights | `/insights/{requestId}` | `requestId: String` |
| Settings | `/settings` | None |

### 4.3 Deep Linking (Future)

For AAOS integration, deep links could trigger navigation directly:

```
naviapp://navigate?from=Berlin&to=Paris
naviapp://demo/reset?scenario=congestion
```

---

## 5. Data Models

### 5.1 Domain Models

```kotlin
// Location
data class Location(
    val latitude: Double,
    val longitude: Double,
    val label: String? = null
)

// Route
data class Route(
    val id: String,
    val label: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val eta: Instant,
    val encodedPolyline: String,
    val scores: RouteScores,
    val isRecommended: Boolean
)

data class RouteScores(
    val overall: Float,
    val routing: Float,
    val traffic: Float,
    val weather: Float,
    val poi: Float
)

// Recommendation
data class Recommendation(
    val requestId: String,
    val recommendedRoute: Route,
    val alternatives: List<Route>,
    val explanation: Explanation,
    val weatherSummary: WeatherSummary,
    val pois: List<PointOfInterest>,
    val metadata: RequestMetadata
)

// Explanation
data class Explanation(
    val summary: String,
    val factors: List<ExplanationFactor>,
    val tradeOffs: String,
    val confidence: Float
)

data class ExplanationFactor(
    val factor: String,
    val influence: Influence,
    val detail: String
)

enum class Influence { HIGH, MEDIUM, LOW }

// Traffic
data class TrafficInfo(
    val level: TrafficLevel,
    val summary: String
)

enum class TrafficLevel { CLEAR, LIGHT, MODERATE, HEAVY, SEVERE }

// Weather
data class WeatherSummary(
    val conditions: String,
    val alerts: List<WeatherAlert>
)

data class WeatherAlert(
    val type: String,
    val severity: String,
    val description: String
)

// POI
data class PointOfInterest(
    val id: String,
    val name: String,
    val category: PoiCategory,
    val location: Location,
    val distanceFromRouteKm: Double,
    val address: String?,
    val openNow: Boolean?
)

enum class PoiCategory { FUEL_STATION, REST_AREA, EV_CHARGING, RESTAURANT }

// Agent Result (for Insights screen)
data class AgentTrace(
    val requestId: String,
    val timestamp: Instant,
    val totalDurationMs: Int,
    val agents: List<AgentResult>,
    val finalDecision: FinalDecision
)

data class AgentResult(
    val name: String,
    val status: AgentStatus,
    val durationMs: Int,
    val inputSummary: String,
    val outputSummary: String,
    val score: Float?,
    val toolCalls: List<ToolCall>
)

data class ToolCall(
    val tool: String,
    val durationMs: Int,
    val status: AgentStatus
)

data class FinalDecision(
    val recommended: String,
    val confidence: Float,
    val method: String
)

enum class AgentStatus { SUCCESS, FAILED, TIMEOUT }

// Metadata
data class RequestMetadata(
    val processingTimeMs: Int,
    val agentsConsulted: List<String>,
    val mode: String
)
```

### 5.2 API Response Wrapper

```kotlin
// Generic result type for all async operations
sealed class UiResult<out T> {
    object Loading : UiResult<Nothing>()
    data class Success<T>(val data: T) : UiResult<T>()
    data class Error(
        val message: String,
        val code: String? = null,
        val retryable: Boolean = true
    ) : UiResult<Nothing>()
}
```

---

## 6. API Integration Design

### 6.1 Retrofit Service Interface

```kotlin
interface NaviApiService {

    @GET("health")
    suspend fun getHealth(): HealthResponseDto

    @POST("recommend-route")
    suspend fun recommendRoute(
        @Body request: RouteRequestDto
    ): RouteResponseDto

    @POST("search-poi")
    suspend fun searchPoi(
        @Body request: PoiRequestDto
    ): PoiResponseDto

    @GET("agent-trace/{requestId}")
    suspend fun getAgentTrace(
        @Path("requestId") requestId: String
    ): TraceResponseDto

    @POST("demo/reset")
    suspend fun resetDemo(
        @Body request: DemoResetRequestDto
    ): DemoResetResponseDto
}
```

### 6.2 Network Module (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)  // allow for agent processing
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        settings: SettingsRepository
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(settings.getBackendUrl())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): NaviApiService {
        return retrofit.create(NaviApiService::class.java)
    }
}
```

### 6.3 Repository Pattern

```kotlin
// Domain layer defines the interface
interface NavigationRepository {
    suspend fun recommendRoute(
        origin: Location,
        destination: Location,
        preferences: RoutePreferences
    ): UiResult<Recommendation>

    suspend fun searchPoi(
        routePolyline: String,
        categories: List<PoiCategory>,
        corridorWidthKm: Double
    ): UiResult<List<PointOfInterest>>

    suspend fun getAgentTrace(requestId: String): UiResult<AgentTrace>

    suspend fun resetDemo(mode: String?, scenario: String?): UiResult<Unit>

    suspend fun checkHealth(): UiResult<HealthStatus>
}

// Data layer implements it
class NavigationRepositoryImpl @Inject constructor(
    private val api: NaviApiService,
    private val routeMapper: RouteMapper,
    private val traceMapper: TraceMapper
) : NavigationRepository {

    override suspend fun recommendRoute(...): UiResult<Recommendation> {
        return try {
            val response = api.recommendRoute(buildRequest(...))
            UiResult.Success(routeMapper.toDomain(response))
        } catch (e: HttpException) {
            UiResult.Error(parseApiError(e))
        } catch (e: IOException) {
            UiResult.Error("Network error. Check connection.", retryable = true)
        }
    }
}
```

### 6.4 DTO ↔ Domain Mapping

DTOs mirror the backend JSON exactly (snake_case via Moshi `@Json`). Mappers convert to domain models (camelCase, enums, value types).

```
API JSON → Moshi → DTO (data layer) → Mapper → Domain Model (domain layer)
```

This separation means backend changes affect only DTOs and mappers, never ViewModels or UI.

---

## 7. State Management

### 7.1 ViewModel State Pattern

Each ViewModel exposes a single `StateFlow<ScreenState>` that the Compose UI observes:

```kotlin
// Example: RouteResultsViewModel
@HiltViewModel
class RouteResultsViewModel @Inject constructor(
    private val recommendRouteUseCase: RecommendRouteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteResultsState>(RouteResultsState.Loading)
    val uiState: StateFlow<RouteResultsState> = _uiState.asStateFlow()

    fun requestRoute(origin: Location, destination: Location, prefs: RoutePreferences) {
        viewModelScope.launch {
            _uiState.value = RouteResultsState.Loading
            when (val result = recommendRouteUseCase(origin, destination, prefs)) {
                is UiResult.Success -> _uiState.value = RouteResultsState.Success(result.data)
                is UiResult.Error -> _uiState.value = RouteResultsState.Error(result.message)
            }
        }
    }
}
```

### 7.2 State Definitions

```kotlin
// Home Screen
sealed class HomeState {
    object Idle : HomeState()
    object Loading : HomeState()
    data class Error(val message: String) : HomeState()
}

data class HomeUiState(
    val origin: Location? = null,
    val destination: Location? = null,
    val recentDestinations: List<Location> = emptyList(),
    val preferences: RoutePreferences = RoutePreferences(),
    val screenState: HomeState = HomeState.Idle
)

// Route Results Screen
sealed class RouteResultsState {
    object Loading : RouteResultsState()
    data class Success(val recommendation: Recommendation) : RouteResultsState()
    data class PartialSuccess(
        val recommendation: Recommendation,
        val warnings: List<String>
    ) : RouteResultsState()
    data class Error(val message: String, val retryable: Boolean = true) : RouteResultsState()
}

// Agent Insights Screen
sealed class InsightsState {
    object Loading : InsightsState()
    data class Success(val trace: AgentTrace) : InsightsState()
    data class NotFound(val requestId: String) : InsightsState()
}

// Settings Screen
data class SettingsState(
    val backendUrl: String,
    val demoMode: Boolean,
    val mockMode: Boolean,
    val selectedScenario: String,
    val verboseLogging: Boolean,
    val showAgentInsights: Boolean,
    val connectionStatus: ConnectionStatus
)

enum class ConnectionStatus { CONNECTED, DISCONNECTED, CHECKING }
```

### 7.3 Event Handling

User interactions are modeled as functions on the ViewModel rather than a sealed event class (simpler for hackathon scope):

```kotlin
class HomeViewModel {
    fun setOrigin(location: Location) { ... }
    fun setDestination(location: Location) { ... }
    fun updatePreferences(prefs: RoutePreferences) { ... }
    fun findRoute() { ... }  // triggers navigation on success
    fun clearError() { ... }
}
```

### 7.4 Data Sharing Between Screens

The recommendation result is passed between screens via navigation arguments (requestId) and a shared in-memory cache:

```kotlin
// Shared cache scoped to activity (survives screen transitions)
@ActivityRetainedScoped
class RecommendationCache @Inject constructor() {
    private val cache = mutableMapOf<String, Recommendation>()

    fun put(requestId: String, recommendation: Recommendation) { ... }
    fun get(requestId: String): Recommendation? { ... }
}
```

---

## 8. Error Handling

### 8.1 Error Categories

| Category | Cause | User Impact | Handling |
|----------|-------|-------------|----------|
| **Network Offline** | No internet connectivity | Cannot make any requests | Show offline banner; disable "Find Route"; show cached map tiles |
| **Timeout** | Backend or agents too slow (>10s) | No recommendation received | Show timeout message with retry button |
| **API Error (4xx)** | Invalid input, bad request | Specific validation error | Show field-level error; guide user to fix input |
| **Server Error (5xx)** | Backend crash or overload | No recommendation | Show generic error with retry; suggest switching to mock mode |
| **Partial Agent Failure** | One agent failed, others succeeded | Reduced confidence recommendation | Show recommendation with warning badge; note missing data |
| **Invalid Backend URL** | Misconfigured settings | All requests fail | Settings screen shows red status; prompt URL correction |

### 8.2 Network Monitor

```kotlin
class NetworkMonitor @Inject constructor(
    private val context: Context
) {
    val isOnline: StateFlow<Boolean>  // observed by ViewModels

    // Uses ConnectivityManager.NetworkCallback
    // Updates StateFlow on connectivity changes
}
```

### 8.3 Error UI Components

```kotlin
// Reusable error banner composable
@Composable
fun ErrorBanner(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
)

// Offline indicator
@Composable
fun OfflineBanner()

// Timeout-specific UI
@Composable
fun TimeoutCard(
    onRetry: () -> Unit,
    onSwitchToMock: () -> Unit
)
```

### 8.4 Retry Strategy

| Scenario | Strategy |
|----------|----------|
| Network timeout | User-initiated retry (tap button) |
| Transient 5xx | Auto-retry once after 2 seconds; then user-initiated |
| Agent partial failure | No retry — present available results with warning |
| Persistent failure | Suggest switching to mock mode in Settings |

### 8.5 Degraded Mode Behavior

When the backend is unreachable:
- Map remains visible with last-loaded tiles.
- "Find Route" button is disabled with "Offline" label.
- Recent destinations remain accessible (local storage).
- Settings screen shows backend status as disconnected.
- If mock mode is enabled locally, a local mock can serve data (future enhancement).

---

## 9. Future AAOS Compatibility

### 9.1 Android Automotive OS Considerations

The architecture is designed to be portable to AAOS with minimal changes:

| Aspect | Current (Phone/Emulator) | AAOS Adaptation |
|--------|-------------------------|-----------------|
| **UI Framework** | Jetpack Compose | Same — Compose works on AAOS |
| **Screen Size** | 5-7" phone | Wider landscape displays (10-15") |
| **Input** | Touch | Touch + rotary controller |
| **Distraction** | No restrictions | Driver distraction guidelines apply |
| **Location** | Fused Location Provider | Vehicle location service |
| **Lifecycle** | Standard Activity | Car App Library or standard Activity |
| **Network** | WiFi / Cellular | Vehicle connectivity (TCU) |

### 9.2 Design Decisions for Portability

| Decision | Rationale |
|----------|-----------|
| All logic in ViewModels, not in Composables | UI layer can be swapped for AAOS-specific layouts without touching logic |
| Backend URL is configurable | vECU may point to a different backend endpoint |
| No direct HERE API calls from the app | Backend handles all external API calls; app is a thin client |
| Settings stored in DataStore | Works identically on AAOS |
| Hilt for DI | Modules can be swapped for AAOS-specific implementations |
| Navigation via sealed routes | Can be adapted to Car App Library navigation model |
| No hardcoded screen dimensions | Compose adapts to any screen size |

### 9.3 AAOS-Specific Adaptations (Future)

When porting to AAOS, the following modules would need attention:

| Module | Change Required |
|--------|----------------|
| `ui/theme/` | Adjust for larger screens, night mode, AAOS color schemes |
| `ui/components/MapView.kt` | Potentially use vehicle-integrated map renderer |
| `ui/navigation/NavGraph.kt` | Adapt for Car App Library templates if required |
| `di/AppModule.kt` | Bind vehicle location service instead of phone GPS |
| `util/NetworkMonitor.kt` | Use vehicle connectivity APIs |
| New: `automotive/` package | AAOS-specific service bindings, distraction optimization |

### 9.4 Distraction Optimization (Future)

For AAOS, the app would need to respect driver distraction guidelines:
- Limit text length on Route Explanation screen while driving.
- Disable complex interactions (scrolling agent insights) when vehicle is moving.
- Use larger touch targets for in-vehicle displays.
- Simplify to essential information: map + ETA + next turn.

These are **future** concerns and will not affect the hackathon implementation, but the clean separation of UI from logic ensures these adaptations won't require architectural changes.

---

*Document Status: Complete*
*Created: June 30, 2026*
*Predecessors: product-vision.md, software-requirements-specification.md, tool-abstraction-architecture.md, backend-implementation-plan.md*
*Next Step: Implement backend (Phase 5 of backend plan), then implement Android application.*
