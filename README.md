# Agentic Navigator — AWS Hackathon MVP

Multi-agent Android navigation application demonstrating AI-driven route recommendation
with transparent decision explanation.

## Architecture

```
Android App (Jetpack Compose)
       │
       ▼ HTTP/REST
FastAPI Backend (EC2)
       │
       ├── Route Agent ──► HERE Routing Tool
       ├── Traffic Agent ──► HERE Traffic Tool
       ├── Weather Agent ──► Weather Tool
       └── Recommendation Agent ──► Explanation
```

## Quick Start

### 1. Backend (on EC2)

```bash
cd backend
pip install -r requirements.txt
cp .env.example .env
# Edit .env if you have real API keys, otherwise mock mode works out of the box

python main.py
```

Backend runs on `http://0.0.0.0:8000`. Verify:

```bash
curl http://localhost:8000/health
```

### 2. Test the API

```bash
curl -X POST http://localhost:8000/recommend-route \
  -H "Content-Type: application/json" \
  -d '{
    "origin": {"latitude": 52.52, "longitude": 13.405, "label": "Berlin"},
    "destination": {"latitude": 48.8566, "longitude": 2.3522, "label": "Paris"},
    "demo_mode": true
  }'
```

### 3. Android App

Open `android/` in Android Studio. Build and run on an Android emulator (API 30+).

The app connects to the backend at `http://10.0.2.2:8000` (Android emulator's
host loopback).

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/recommend-route` | Get route recommendation from all agents |
| GET | `/agent-trace/{request_id}` | Get detailed agent execution trace |

## Modes

- **Mock mode** (default): Returns pre-built realistic data. No API keys needed.
- **Live mode**: Set `APP_MODE=live` and provide `HERE_API_KEY` in `.env`.

## Project Structure

```
navi_app/
├── backend/                  # FastAPI backend
│   ├── main.py              # App entry point
│   ├── config.py            # Environment config
│   ├── models.py            # Pydantic models
│   ├── agents/              # AI agents
│   │   ├── orchestrator.py  # Agent coordination
│   │   ├── route_agent.py   # Route evaluation
│   │   ├── traffic_agent.py # Traffic scoring
│   │   ├── weather_agent.py # Weather scoring
│   │   └── recommendation_agent.py  # Final recommendation
│   └── tools/               # External service adapters
│       ├── here_routing.py  # HERE Routing API
│       ├── here_traffic.py  # HERE Traffic API
│       └── weather.py       # Weather API
├── android/                  # Android app (Jetpack Compose)
│   └── app/src/main/java/com/naviapp/agent/
│       ├── ui/home/         # Home screen
│       ├── ui/results/      # Route results screen
│       └── ui/insights/     # Agent insights (demo screen)
└── docs/                     # Architecture documents
```

## Demo Flow

1. Launch backend: `python main.py`
2. Launch Android app on emulator
3. Select "Berlin" as origin, "Paris" as destination
4. Tap "Recommend Route"
5. View recommendation with explanation
6. Tap "Agent Insights" to see multi-agent collaboration details

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_MODE` | `mock` | `mock` or `live` |
| `APP_PORT` | `8000` | Server port |
| `HERE_API_KEY` | — | HERE API key (live mode only) |
| `WEATHER_API_KEY` | — | Weather API key (live mode only) |
