# Agentic Navigator — Backend

FastAPI backend that orchestrates multiple AI agents to produce intelligent route
recommendations with natural-language explanations.

## Quick Start

```bash
pip install -r requirements.txt
cp .env.example .env   # Edit if you have real API keys
python main.py
```

Server starts at http://0.0.0.0:8000. Mock mode works immediately without any API keys.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check with service status |
| POST | `/recommend-route` | Multi-agent route recommendation |
| GET | `/agent-trace/{request_id}` | Detailed agent execution trace |
| POST | `/demo/reset` | Reset demo state, switch mode/scenario |

Interactive API docs: http://localhost:8000/docs

## Architecture

```
POST /recommend-route
       │
       ▼
   Orchestrator
       │
       ├─── Route Agent ──► HERE Routing Tool ──► 3 candidate routes
       │
       ├─── (parallel) ─────────────────────────────────────────────
       │    ├── Traffic Agent ──► HERE Traffic Tool ──► congestion scores
       │    ├── Weather Agent ──► Weather Tool ──► safety scores
       │    └── POI Agent ──► HERE Places Tool ──► convenience scores
       │
       └─── Recommendation Agent ──► weighted aggregation + explanation
```

## Agents

| Agent | Tool Used | Responsibility |
|-------|-----------|----------------|
| Route Agent | `here_routing` | Calculate candidates, score by duration |
| Traffic Agent | `here_traffic` | Score by congestion/incidents |
| Weather Agent | `weather_forecast` | Score by weather safety |
| POI Agent | `here_places` | Score by POI availability |
| Recommendation Agent | — | Aggregate scores, generate explanation |

## Modes

- **Mock** (`APP_MODE=mock`): Pre-built data, no API keys needed. Default.
- **Live** (`APP_MODE=live`): Real HERE API calls. Requires `HERE_API_KEY`.

Switch at runtime: `POST /demo/reset` with `{"mode": "mock"}` or `{"mode": "live"}`.

## Testing

```bash
# Start server in one terminal
python main.py

# Run tests in another
bash test_api.sh
```

All 23 tests verify: health, recommendation, traces, demo reset, validation, OpenAPI.

## Environment Variables

See `.env.example` for all options. Key variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_MODE` | `mock` | `mock` or `live` |
| `HERE_API_KEY` | — | HERE API key (live mode) |
| `AGENT_TIMEOUT_SECONDS` | `5` | Per-agent timeout |
| `LOG_LEVEL` | `INFO` | Logging verbosity |

## Project Structure

```
backend/
├── main.py              # FastAPI app + endpoints
├── config.py            # Settings from env vars
├── models.py            # Pydantic request/response models
├── logging_config.py    # Structured JSON logging
├── agents/
│   ├── orchestrator.py  # Agent coordination + trace store
│   ├── route_agent.py   # Route scoring
│   ├── traffic_agent.py # Traffic scoring
│   ├── weather_agent.py # Weather scoring
│   ├── poi_agent.py     # POI scoring
│   └── recommendation_agent.py  # Aggregation + explanation
├── tools/
│   ├── here_routing.py  # HERE Routing v8 adapter
│   ├── here_traffic.py  # HERE Traffic v7 adapter
│   ├── here_places.py   # HERE Browse API adapter
│   └── weather.py       # Weather API adapter
└── test_api.sh          # Test script (23 tests)
```
