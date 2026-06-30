# Agentic Navigator — Android App

Jetpack Compose Android application with HERE SDK map integration that demonstrates
multi-agent route recommendation with transparent explanations.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- JDK 17
- Android emulator API 30+
- HERE platform account (for map rendering)

## Quick Start

1. Open `android/` folder in Android Studio
2. Set up HERE SDK credentials (see below)
3. Let Gradle sync complete
4. Run the app on an Android emulator

## Default Backend URL

The app connects to `http://3.87.198.126:8000` by default (EC2 instance).

### Changing the Backend URL

**At runtime (no recompile needed):**
1. Open the app → tap ⚙️ Settings (gear icon on Home screen)
2. Enter the new backend URL
3. Tap "Test Connection" to verify
4. Tap "Save"

The URL is persisted in DataStore and survives app restarts.

**Quick presets in Settings:**
- `EC2` → http://3.87.198.126:8000
- `Emulator` → http://10.0.2.2:8000 (emulator → host localhost)
- `Local` → http://localhost:8000

### Where the URL lives in code

The default URL is defined in a single place:
- `data/SettingsStore.kt` → `DEFAULT_URL` constant

The `ApiClient` also has this as its initial value. Both are updated together via
the Settings screen. **Never hardcode the URL elsewhere.**

## HERE SDK Setup

### 1. Get Credentials

1. Go to https://platform.here.com/
2. Create a project or use existing
3. Generate an App (REST type)
4. Note the **Access Key ID** and **Access Key Secret**

### 2. Configure local.properties

Copy the template and fill in your credentials:

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:
```properties
HERE_ACCESS_KEY_ID=your-actual-key-id
HERE_ACCESS_KEY_SECRET=your-actual-key-secret
```

**IMPORTANT:** `local.properties` is in `.gitignore` and must never be committed.

### 3. How it works

- Credentials flow: `local.properties` → `build.gradle.kts` (manifestPlaceholders) → `AndroidManifest.xml` (meta-data)
- The HERE SDK reads credentials from manifest meta-data at startup
- If credentials are missing or invalid, the app shows a "Map Unavailable" placeholder instead of crashing

### 4. Without HERE credentials

The app works fine without HERE credentials:
- Route recommendation, explanation, and agent insights all work normally
- The map area shows a placeholder: "🗺️ Map Unavailable — HERE SDK not configured"
- All other functionality is unaffected

## Screens

### Home Screen
- Select origin and destination (city presets or custom text)
- Toggle demo mode
- Tap "🚀 Recommend Route" to call the backend

### Route Results Screen
- **HERE Map** at the top with origin/destination markers and route visualization
- Recommended route card (ETA, distance, confidence score)
- Traffic and weather summaries
- Weather alerts
- Points of Interest along the route
- Explanation with factors and trade-offs
- Alternative routes with scores

### Agent Insights Screen (Demo)
- Execution trace of all 5 agents
- Per-agent timing, inputs, outputs, scores
- Tool calls made by each agent
- Dark theme for visual distinction

### Settings Screen
- Backend URL configuration
- Quick presets (EC2 / Emulator / Local)
- "Test Connection" with live health check
- Persistent storage via DataStore

## Architecture

```
UI Layer (Compose Screens + HERE MapView)
    ↕ StateFlow
ViewModel Layer
    ↕ suspend functions
Data Layer (Retrofit → Backend REST API)
```

## Error Handling

| Scenario | Behavior |
|----------|----------|
| Backend unreachable | Error message on Home screen |
| Request timeout | Timeout error with suggestion |
| HERE SDK not configured | Map placeholder, rest of app works |
| HERE SDK init failure | Map placeholder, logged as warning |
| Invalid response | Gson parsing error shown |

## Project Structure

```
app/src/main/java/com/naviapp/agent/
├── MainActivity.kt
├── NaviApplication.kt           # HERE SDK initialization
├── data/
│   ├── ApiClient.kt            # Retrofit with dynamic URL
│   ├── ApiModels.kt            # DTOs matching backend JSON
│   ├── NaviApiService.kt       # Retrofit interface
│   └── SettingsStore.kt        # DataStore (DEFAULT_URL defined here)
└── ui/
    ├── NaviApp.kt              # Navigation graph
    ├── ResultCache.kt          # In-memory result cache
    ├── theme/Theme.kt          # Material 3 theme
    ├── home/
    │   ├── HomeScreen.kt
    │   └── HomeViewModel.kt
    ├── map/
    │   └── HereMapView.kt      # HERE SDK MapView composable + placeholder
    ├── results/
    │   └── RouteResultsScreen.kt  # Map + recommendation cards
    ├── insights/
    │   └── AgentInsightsScreen.kt
    └── settings/
        └── SettingsScreen.kt
```
