# Agentic Navigator — Android App

Jetpack Compose Android application that demonstrates multi-agent route recommendation
with transparent explanations.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- JDK 17
- Android emulator API 30+

## Setup

1. Open `android/` folder in Android Studio
2. Let Gradle sync complete
3. Start the backend: `cd ../backend && python main.py`
4. Run the app on an Android emulator

## Backend Connection

The app defaults to `http://10.0.2.2:8000` which maps to the host machine's
localhost from inside the Android emulator.

To change the backend URL:
- Open Settings screen (gear icon on Home)
- Enter the backend URL
- Tap "Test Connection" to verify
- Tap "Save"

### Common URLs

| Environment | URL |
|-------------|-----|
| Android Emulator → Host | `http://10.0.2.2:8000` |
| Same machine (browser) | `http://localhost:8000` |
| EC2 instance | `http://<ec2-ip>:8000` |

## Screens

### Home Screen
- Select origin and destination (presets or custom text)
- Toggle demo mode
- Tap "Recommend Route" to call the backend

### Route Results Screen
- Shows recommended route with ETA, distance, confidence score
- Displays traffic and weather summaries
- Shows explanation of why the route was chosen
- Lists POIs along the route
- Shows alternative routes with scores

### Agent Insights Screen (Demo)
- Shows execution trace of all agents
- Displays per-agent timing, inputs, outputs, scores
- Shows tool calls made by each agent
- Dark theme for visual distinction during demos

### Settings Screen
- Configure backend URL
- Test connection with one tap
- Quick presets for emulator and local

## Architecture

```
UI Layer (Compose Screens)
    ↕ StateFlow
ViewModel Layer
    ↕ suspend functions
Data Layer (Retrofit → Backend API)
```

- **No Hilt** — simplified DI with factory pattern
- **Gson** — JSON serialization matching backend snake_case
- **DataStore** — persists settings locally
- **ResultCache** — in-memory cache for screen-to-screen data passing

## Error Handling

The app handles:
- Backend unreachable (ConnectException)
- Request timeout (SocketTimeoutException)
- Invalid responses (Gson parsing)
- Missing data gracefully (null-safe DTOs)

## Project Structure

```
app/src/main/java/com/naviapp/agent/
├── MainActivity.kt          # Entry point
├── NaviApplication.kt       # Application class
├── data/
│   ├── ApiClient.kt        # Retrofit singleton with dynamic URL
│   ├── ApiModels.kt        # All DTOs matching backend JSON
│   ├── NaviApiService.kt   # Retrofit interface
│   └── SettingsStore.kt    # DataStore persistence
└── ui/
    ├── NaviApp.kt           # Navigation graph
    ├── ResultCache.kt       # In-memory result cache
    ├── theme/Theme.kt       # Material 3 theme
    ├── home/
    │   ├── HomeScreen.kt    # Home UI
    │   └── HomeViewModel.kt # Home logic + API call
    ├── results/
    │   └── RouteResultsScreen.kt  # Results UI
    ├── insights/
    │   └── AgentInsightsScreen.kt # Agent trace UI
    └── settings/
        └── SettingsScreen.kt      # Settings UI
```
