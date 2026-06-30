# Backend Implementation Plan
## Accelerating the V-Cycle with Agentic AI — Navigation Application

| Field | Value |
|-------|-------|
| Document Version | 1.0 |
| Status | Draft |
| Date | June 30, 2026 |
| Runtime Environment | Existing AWS EC2 instance |
| Framework | FastAPI (Python 3.11+) |
| Context | Hackathon MVP — lightweight, single-instance deployment |

---

## Table of Contents

1. [Backend Folder Structure](#1-backend-folder-structure)
2. [Python Module Plan](#2-python-module-plan)
3. [REST API Contract](#3-rest-api-contract)
4. [Tool Abstraction Design](#4-tool-abstraction-design)
5. [Environment Variable List](#5-environment-variable-list)
6. [Mock Mode Strategy](#6-mock-mode-strategy)
7. [Minimal Execution Sequence](#7-minimal-execution-sequence)
8. [Implementation Order](#8-implementation-order)

---

## 1. Backend Folder Structure

```
backend/
├── main.py                        # FastAPI app entry point, uvicorn runner
├── config.py                      # Settings from environment variables
├── requirements.txt               # Python dependencies
├── .env.example                   # Template for environment variables
│
├── api/
│   ├── __init__.py
│   ├── router.py                  # Mounts all route modules
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── health.py             # GET /health
│   │   ├── recommend.py          # POST /recommend-route
│   │   ├── poi.py                # POST /search-poi
│   │   ├── trace.py             # GET /agent-trace/{request_id}
│   │   └── demo.py              # POST /demo/reset
│   └── schemas/
│       ├── __init__.py
│       ├── request.py            # Pydantic request models
│       ├── response.py           # Pydantic response models
│       └── common.py            # Shared types (Location, RouteSegment, etc.)
│
├── agents/
│   ├── __init__.py
│   ├── base.py                   # Abstract Agent interface
│   ├── orchestrator.py           # Agent orchestration logic
│   ├── route_agent.py            # Route evaluation agent
│   ├── traffic_agent.py          # Traffic evaluation agent
│   ├── weather_agent.py          # Weather evaluation agent
│   ├── poi_agent.py              # POI evaluation agent
│   └── recommendation_agent.py   # Final recommendation + explanation
│
├── tools/
│   ├── __init__.py
│   ├── registry.py               # Tool registry (name → adapter lookup)
│   ├── base.py                   # Abstract Tool interface
│   ├── here_routing.py           # HERE Routing API adapter
│   ├── here_traffic.py           # HERE Traffic API adapter
│   ├── here_places.py            # HERE Places API adapter
│   └── weather.py                # Weather API adapter
│
├── mocks/
│   ├── __init__.py
│   ├── mock_provider.py          # Mock data orchestration
│   ├── data/
│   │   ├── routes.json           # Pre-built route responses
│   │   ├── traffic.json          # Pre-built traffic data
│   │   ├── weather.json          # Pre-built weather data
│   │   ├── places.json           # Pre-built POI data
│   │   └── scenarios.json        # Named demo scenarios
│   └── mock_tools.py             # Mock tool implementations
│
├── models/
│   ├── __init__.py
│   ├── route.py                  # Route domain model
│   ├── agent_result.py           # Agent evaluation result model
│   ├── recommendation.py         # Recommendation domain model
│   └── trace.py                  # Request trace model
│
└── utils/
    ├── __init__.py
    ├── logging.py                # Structured logging setup
    ├── errors.py                 # Error types and handlers
    └── tracing.py                # Request tracing / correlation IDs
```

---

## 2. Python Module Plan

### 2.1 Dependencies (requirements.txt)

```
fastapi==0.111.*
uvicorn[standard]==0.30.*
pydantic==2.*
httpx==0.27.*
python-dotenv==1.0.*
structlog==24.*
```

### 2.2 Module Responsibilities

| Module | Responsibility |
|--------|---------------|
| `main.py` | App creation, middleware, startup/shutdown, uvicorn launch |
| `config.py` | Load `.env`, validate required vars, expose typed `Settings` object |
| `api/router.py` | Aggregate all route modules under versioned prefix |
| `api/routes/*` | Thin HTTP handlers — validate input, delegate to orchestrator, format output |
| `api/schemas/*` | Pydantic models for request/response validation and serialization |
| `agents/base.py` | `BaseAgent` ABC with `evaluate(route_candidates, context) → AgentResult` |
| `agents/orchestrator.py` | Invoke agents (parallel via asyncio), aggregate scores, produce recommendation |
| `agents/route_agent.py` | Score routes by distance/duration trade-offs |
| `agents/traffic_agent.py` | Score routes by traffic conditions using `here_traffic` tool |
| `agents/weather_agent.py` | Score routes by weather safety using `weather_forecast` tool |
| `agents/poi_agent.py` | Score routes by POI availability using `here_places` tool |
| `agents/recommendation_agent.py` | Generate final recommendation + natural-language explanation |
| `tools/registry.py` | `ToolRegistry` — maps names to adapters, supports mock swap |
| `tools/base.py` | `BaseTool` ABC with `execute(input: dict) → dict` |
| `tools/here_routing.py` | Wraps HERE Routing v8 REST API |
| `tools/here_traffic.py` | Wraps HERE Traffic v7 REST API |
| `tools/here_places.py` | Wraps HERE Browse/Discover API |
| `tools/weather.py` | Wraps weather service REST API |
| `mocks/mock_provider.py` | Manages demo state, scenario selection, mock data loading |
| `mocks/mock_tools.py` | Implements `BaseTool` returning pre-defined JSON from `data/` |
| `models/*` | Domain objects decoupled from API schemas |
| `utils/logging.py` | Configure structlog with JSON output |
| `utils/errors.py` | Custom exceptions, FastAPI exception handlers |
| `utils/tracing.py` | Generate request IDs, store agent traces in-memory |

### 2.3 Key Design Patterns

| Pattern | Usage |
|---------|-------|
| **Dependency Injection** | FastAPI `Depends()` for registry, settings, mock provider |
| **Strategy Pattern** | Agents implement a common interface; orchestrator doesn't know specifics |
| **Adapter Pattern** | Tools wrap external APIs behind a uniform interface |
| **Factory Pattern** | `ToolRegistry` creates tool instances based on mode (live/mock) |
| **Async-first** | All tools use `httpx.AsyncClient`; agents run concurrently via `asyncio.gather` |

---

## 3. REST API Contract

### 3.1 GET /health

**Purpose:** Health check for monitoring and demo readiness verification.

**Request:** No body or parameters.

**Response (200 OK):**

```json
{
  "status": "healthy",
  "mode": "live",
  "timestamp": "2026-06-30T08:00:00Z",
  "services": {
    "here_routing": "reachable",
    "here_traffic": "reachable",
    "weather": "reachable"
  },
  "version": "0.1.0"
}
```

**Error Response (503 Service Unavailable):**

```json
{
  "status": "degraded",
  "mode": "live",
  "timestamp": "2026-06-30T08:00:00Z",
  "services": {
    "here_routing": "reachable",
    "here_traffic": "unreachable",
    "weather": "reachable"
  },
  "version": "0.1.0"
}
```

---

### 3.2 POST /recommend-route

**Purpose:** Main endpoint. Accepts origin/destination, orchestrates all agents, returns recommended route with explanation.

**Request:**

```json
{
  "origin": {
    "latitude": 52.5200,
    "longitude": 13.4050,
    "label": "Berlin"
  },
  "destination": {
    "latitude": 48.8566,
    "longitude": 2.3522,
    "label": "Paris"
  },
  "preferences": {
    "transport_mode": "car",
    "departure_time": "2026-06-30T08:00:00Z",
    "avoid_highways": false,
    "prioritize": "fastest"
  }
}
```

**Response (200 OK):**

```json
{
  "request_id": "req-abc123",
  "recommended_route": {
    "id": "route-1",
    "label": "Via A2 / A4",
    "summary": {
      "distance_km": 1050.5,
      "duration_minutes": 600,
      "eta": "2026-06-30T18:00:00Z"
    },
    "geometry": {
      "encoded_polyline": "encoded-string"
    },
    "scores": {
      "overall": 0.87,
      "routing": 0.85,
      "traffic": 0.90,
      "weather": 0.70,
      "poi": 0.75
    }
  },
  "alternatives": [
    {
      "id": "route-2",
      "label": "Via A3 / A6",
      "summary": {
        "distance_km": 980.0,
        "duration_minutes": 650,
        "eta": "2026-06-30T18:50:00Z"
      },
      "geometry": {
        "encoded_polyline": "encoded-string"
      },
      "scores": {
        "overall": 0.72,
        "routing": 0.80,
        "traffic": 0.55,
        "weather": 0.70,
        "poi": 0.80
      }
    }
  ],
  "explanation": {
    "summary": "Route 1 via A2/A4 is recommended. It saves approximately 50 minutes compared to Route 2 by avoiding heavy congestion on the A3 corridor near Frankfurt.",
    "factors": [
      {
        "factor": "traffic",
        "influence": "high",
        "detail": "Route 2 has a major slowdown on A3 due to road works, adding ~45 minutes."
      },
      {
        "factor": "duration",
        "influence": "high",
        "detail": "Despite being 70km longer, Route 1 arrives 50 minutes earlier."
      },
      {
        "factor": "weather",
        "influence": "low",
        "detail": "Light rain expected on both routes — no significant safety difference."
      }
    ],
    "trade_offs": "Route 2 is shorter and more fuel-efficient, but current traffic makes it significantly slower."
  },
  "weather_summary": {
    "alerts": [],
    "conditions": "Partly cloudy, 18°C at departure. Light rain possible near Cologne."
  },
  "pois": [
    {
      "name": "Autohof Geiselwind",
      "category": "fuel-station",
      "location": { "latitude": 49.77, "longitude": 10.47 },
      "distance_from_route_km": 0.2
    }
  ],
  "metadata": {
    "processing_time_ms": 4500,
    "agents_consulted": ["routing", "traffic", "weather", "poi"],
    "mode": "live"
  }
}
```

**Error Response (422 Validation Error):**

```json
{
  "detail": [
    {
      "loc": ["body", "origin", "latitude"],
      "msg": "field required",
      "type": "value_error.missing"
    }
  ]
}
```

**Error Response (500 Internal Server Error):**

```json
{
  "error": {
    "code": "ORCHESTRATION_FAILED",
    "message": "Unable to produce route recommendation",
    "request_id": "req-abc123",
    "partial_results_available": true
  }
}
```

**Error Response (504 Gateway Timeout):**

```json
{
  "error": {
    "code": "TIMEOUT",
    "message": "Route recommendation timed out after 10 seconds",
    "request_id": "req-abc123"
  }
}
```

---

### 3.3 POST /search-poi

**Purpose:** Search for points of interest along a given route corridor. Can be called independently of route recommendation.

**Request:**

```json
{
  "route_polyline": "encoded-polyline-string",
  "corridor_width_km": 5,
  "categories": ["fuel-station", "rest-area", "ev-charging"],
  "limit": 10
}
```

**Response (200 OK):**

```json
{
  "pois": [
    {
      "id": "place-001",
      "name": "Autohof Geiselwind",
      "category": "fuel-station",
      "location": { "latitude": 49.77, "longitude": 10.47 },
      "distance_from_route_km": 0.2,
      "address": "A3 Ausfahrt 76, 96160 Geiselwind",
      "open_now": true
    },
    {
      "id": "place-002",
      "name": "Raststätte Feucht",
      "category": "rest-area",
      "location": { "latitude": 49.38, "longitude": 11.22 },
      "distance_from_route_km": 0.0,
      "address": "A9 km 412",
      "open_now": true
    }
  ],
  "metadata": {
    "total_found": 8,
    "returned": 2,
    "mode": "live"
  }
}
```

**Error Response (400 Bad Request):**

```json
{
  "error": {
    "code": "INVALID_POLYLINE",
    "message": "The provided route polyline could not be decoded"
  }
}
```

---

### 3.4 GET /agent-trace/{request_id}

**Purpose:** Retrieve the detailed trace of agent invocations for a given request. Useful for debugging, demonstration, and observability.

**Request:** Path parameter `request_id` (string).

**Response (200 OK):**

```json
{
  "request_id": "req-abc123",
  "timestamp": "2026-06-30T08:00:00Z",
  "total_duration_ms": 4500,
  "agents": [
    {
      "name": "routing",
      "status": "success",
      "duration_ms": 1200,
      "input_summary": "Berlin → Paris, car, 3 alternatives",
      "output_summary": "3 candidate routes generated",
      "score": 0.85,
      "tool_calls": [
        {
          "tool": "here_routing",
          "duration_ms": 1100,
          "status": "success"
        }
      ]
    },
    {
      "name": "traffic",
      "status": "success",
      "duration_ms": 800,
      "input_summary": "3 route corridors",
      "output_summary": "Route-1: clear, Route-2: heavy congestion, Route-3: moderate",
      "score": 0.90,
      "tool_calls": [
        {
          "tool": "here_traffic",
          "duration_ms": 700,
          "status": "success"
        }
      ]
    },
    {
      "name": "weather",
      "status": "success",
      "duration_ms": 600,
      "input_summary": "9 waypoints across 3 routes",
      "output_summary": "Light rain near Cologne on all routes",
      "score": 0.70,
      "tool_calls": [
        {
          "tool": "weather_forecast",
          "duration_ms": 500,
          "status": "success"
        }
      ]
    },
    {
      "name": "poi",
      "status": "success",
      "duration_ms": 500,
      "input_summary": "3 route corridors, categories: fuel, rest",
      "output_summary": "15 POIs found across all routes",
      "score": 0.75,
      "tool_calls": [
        {
          "tool": "here_places",
          "duration_ms": 400,
          "status": "success"
        }
      ]
    },
    {
      "name": "recommendation",
      "status": "success",
      "duration_ms": 1400,
      "input_summary": "4 agent scores + route data",
      "output_summary": "Route-1 recommended with 0.87 confidence",
      "tool_calls": []
    }
  ],
  "final_decision": {
    "recommended": "route-1",
    "confidence": 0.87,
    "method": "weighted_score"
  }
}
```

**Error Response (404 Not Found):**

```json
{
  "error": {
    "code": "TRACE_NOT_FOUND",
    "message": "No trace found for request_id: req-xyz999"
  }
}
```

---

### 3.5 POST /demo/reset

**Purpose:** Reset the demo state. Clears trace history, reloads mock data, and optionally switches between live and mock mode.

**Request:**

```json
{
  "mode": "mock",
  "scenario": "berlin-to-paris-congestion"
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `mode` | No | `"live"` or `"mock"`. If omitted, keeps current mode. |
| `scenario` | No | Named scenario to load (mock mode only). If omitted, uses default. |

**Response (200 OK):**

```json
{
  "status": "reset_complete",
  "mode": "mock",
  "scenario": "berlin-to-paris-congestion",
  "traces_cleared": 12,
  "timestamp": "2026-06-30T08:05:00Z"
}
```

**Error Response (400 Bad Request):**

```json
{
  "error": {
    "code": "INVALID_SCENARIO",
    "message": "Scenario 'unknown-scenario' not found. Available: berlin-to-paris-congestion, munich-to-hamburg-clear, stuttgart-to-zurich-rain"
  }
}
```

---

## 4. Tool Abstraction Design

### 4.1 Base Tool Interface

```python
from abc import ABC, abstractmethod
from typing import Any

class BaseTool(ABC):
    """Abstract base for all tools. MCP-ready interface."""

    @property
    @abstractmethod
    def name(self) -> str:
        """Unique tool identifier (matches MCP tool name)."""
        ...

    @property
    @abstractmethod
    def description(self) -> str:
        """Human-readable description (matches MCP tool description)."""
        ...

    @property
    @abstractmethod
    def input_schema(self) -> dict:
        """JSON Schema for expected input (matches MCP inputSchema)."""
        ...

    @abstractmethod
    async def execute(self, input: dict) -> dict:
        """Execute the tool and return structured output."""
        ...
```

### 4.2 Tool Registry

```python
class ToolRegistry:
    """Maps tool names to implementations. Swap point for MCP migration."""

    def __init__(self):
        self._tools: dict[str, BaseTool] = {}

    def register(self, tool: BaseTool) -> None:
        self._tools[tool.name] = tool

    async def invoke(self, tool_name: str, input: dict) -> dict:
        tool = self._tools.get(tool_name)
        if not tool:
            raise ToolNotFoundError(tool_name)
        return await tool.execute(input)

    def list_tools(self) -> list[dict]:
        """Returns MCP-compatible tool descriptors."""
        return [
            {
                "name": t.name,
                "description": t.description,
                "inputSchema": t.input_schema
            }
            for t in self._tools.values()
        ]
```

### 4.3 Registry Initialization

```python
def create_registry(settings: Settings) -> ToolRegistry:
    """Factory: builds registry with live or mock tools based on mode."""
    registry = ToolRegistry()

    if settings.mode == "mock":
        registry.register(MockHereRoutingTool())
        registry.register(MockHereTrafficTool())
        registry.register(MockHerePlacesTool())
        registry.register(MockWeatherTool())
    else:
        registry.register(HereRoutingTool(api_key=settings.here_api_key))
        registry.register(HereTrafficTool(api_key=settings.here_api_key))
        registry.register(HerePlacesTool(api_key=settings.here_api_key))
        registry.register(WeatherTool(api_key=settings.weather_api_key))

    return registry
```

### 4.4 MCP Migration Seam

When ready to expose tools as MCP servers, the registry gains a second resolution path:

```python
class ToolRegistry:
    async def invoke(self, tool_name: str, input: dict) -> dict:
        # Future: check MCP server mapping first
        if tool_name in self._mcp_mappings:
            return await self._mcp_client.call_tool(tool_name, input)

        # Current: local adapter
        tool = self._tools.get(tool_name)
        if not tool:
            raise ToolNotFoundError(tool_name)
        return await tool.execute(input)
```

No agent code changes required — they continue calling `registry.invoke(name, input)`.

---

## 5. Environment Variable List

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `APP_MODE` | No | `live` | `live` or `mock` — controls which tool implementations are used |
| `APP_PORT` | No | `8000` | Port for uvicorn to bind |
| `APP_HOST` | No | `0.0.0.0` | Host for uvicorn to bind |
| `HERE_API_KEY` | Yes (live) | — | HERE API key (from AWS Marketplace subscription) |
| `WEATHER_API_KEY` | Yes (live) | — | Weather service API key |
| `WEATHER_API_URL` | No | `https://api.open-meteo.com` | Base URL for weather service |
| `HERE_BASE_URL` | No | `https://router.hereapi.com` | HERE API base URL |
| `LOG_LEVEL` | No | `INFO` | Logging level (DEBUG, INFO, WARNING, ERROR) |
| `REQUEST_TIMEOUT_SECONDS` | No | `10` | Maximum time for a full recommendation request |
| `AGENT_TIMEOUT_SECONDS` | No | `5` | Maximum time for a single agent evaluation |
| `MOCK_SCENARIO` | No | `default` | Which mock scenario to load at startup |
| `TRACE_RETENTION_COUNT` | No | `100` | Number of request traces to keep in memory |
| `CORS_ORIGINS` | No | `*` | Allowed CORS origins (comma-separated) |

### 5.1 .env.example

```bash
# Application
APP_MODE=mock
APP_PORT=8000
APP_HOST=0.0.0.0
LOG_LEVEL=INFO

# HERE APIs (from AWS Marketplace)
HERE_API_KEY=your-here-api-key-here
HERE_BASE_URL=https://router.hereapi.com

# Weather
WEATHER_API_KEY=your-weather-api-key-here
WEATHER_API_URL=https://api.open-meteo.com

# Timeouts
REQUEST_TIMEOUT_SECONDS=10
AGENT_TIMEOUT_SECONDS=5

# Mock
MOCK_SCENARIO=default

# Tracing
TRACE_RETENTION_COUNT=100

# CORS
CORS_ORIGINS=*
```

---

## 6. Mock Mode Strategy

### 6.1 Purpose

Mock mode ensures:
- Development can proceed without live API keys.
- Demos are repeatable and reliable regardless of external service availability.
- The demo can be reset instantly between presentations.

### 6.2 How It Works

```
┌──────────────────────────────────────────────┐
│              APP_MODE = "mock"                │
│                                              │
│  ToolRegistry loads MockTools instead of     │
│  live adapters. MockTools read from JSON     │
│  files in mocks/data/ and return             │
│  pre-defined responses.                      │
│                                              │
│  Agent logic runs identically — it doesn't   │
│  know whether tools are live or mock.        │
└──────────────────────────────────────────────┘
```

### 6.3 Named Scenarios

Mock data is organized into scenarios — named configurations representing specific demo flows:

| Scenario | Description | Demonstrates |
|----------|-------------|--------------|
| `default` | Berlin → Paris, moderate traffic | Standard happy-path flow |
| `berlin-to-paris-congestion` | Berlin → Paris, heavy A3 traffic | Traffic agent influence on recommendation |
| `munich-to-hamburg-clear` | Munich → Hamburg, no issues | Fast recommendation, all agents agree |
| `stuttgart-to-zurich-rain` | Stuttgart → Zurich, heavy rain | Weather agent influence, safety warnings |

### 6.4 Scenario File Structure

```json
// mocks/data/scenarios.json
{
  "scenarios": {
    "default": {
      "routes_file": "routes.json",
      "traffic_file": "traffic.json",
      "weather_file": "weather.json",
      "places_file": "places.json"
    },
    "berlin-to-paris-congestion": {
      "routes_file": "routes.json",
      "traffic_file": "traffic_congestion.json",
      "weather_file": "weather.json",
      "places_file": "places.json"
    }
  }
}
```

### 6.5 Mock Tool Behavior

- Returns pre-loaded JSON from scenario files.
- Introduces artificial delay (200-500ms) to simulate realistic latency.
- Supports `POST /demo/reset` to switch scenarios at runtime.
- Mock tools implement the same `BaseTool` interface as live tools.

### 6.6 Switching Modes

| Method | How |
|--------|-----|
| At startup | Set `APP_MODE=mock` in environment |
| At runtime | Call `POST /demo/reset` with `{"mode": "mock"}` or `{"mode": "live"}` |

---

## 7. Minimal Execution Sequence

### 7.1 POST /recommend-route — Happy Path

```
Client                  FastAPI              Orchestrator           Tools/Agents
  │                       │                      │                      │
  │── POST /recommend ──►│                      │                      │
  │                       │── validate input ──►│                      │
  │                       │                      │                      │
  │                       │                      │── invoke here_routing ──►│
  │                       │                      │◄── 3 candidate routes ───│
  │                       │                      │                      │
  │                       │                      │── parallel ──────────────│
  │                       │                      │   ├─ traffic_agent       │
  │                       │                      │   │  └─ here_traffic ──►│
  │                       │                      │   ├─ weather_agent       │
  │                       │                      │   │  └─ weather_fcst ─►│
  │                       │                      │   └─ poi_agent           │
  │                       │                      │      └─ here_places ──►│
  │                       │                      │◄── agent results ────────│
  │                       │                      │                      │
  │                       │                      │── aggregate scores       │
  │                       │                      │── recommendation_agent   │
  │                       │                      │   └─ generate explanation│
  │                       │                      │                      │
  │                       │                      │── store trace            │
  │                       │                      │                      │
  │                       │◄── recommendation ───│                      │
  │◄── 200 JSON ─────────│                      │                      │
```

### 7.2 Execution Steps (Pseudocode)

```python
async def recommend_route(request: RouteRequest) -> RouteResponse:
    request_id = generate_request_id()
    trace = Trace(request_id)

    # Step 1: Get candidate routes
    routes = await registry.invoke("here_routing", {
        "origin": request.origin,
        "destination": request.destination,
        "transportMode": request.preferences.transport_mode,
        "alternatives": 3
    })
    trace.record("routing", routes)

    # Step 2: Run evaluation agents in parallel
    traffic_task = traffic_agent.evaluate(routes, registry)
    weather_task = weather_agent.evaluate(routes, registry)
    poi_task = poi_agent.evaluate(routes, registry)

    traffic_result, weather_result, poi_result = await asyncio.gather(
        traffic_task, weather_task, poi_task,
        return_exceptions=True  # graceful degradation
    )
    trace.record("traffic", traffic_result)
    trace.record("weather", weather_result)
    trace.record("poi", poi_result)

    # Step 3: Aggregate and recommend
    recommendation = recommendation_agent.recommend(
        routes=routes,
        evaluations=[traffic_result, weather_result, poi_result]
    )
    trace.record("recommendation", recommendation)

    # Step 4: Store trace for /agent-trace endpoint
    trace_store.save(trace)

    return build_response(request_id, recommendation)
```

### 7.3 Timeout and Degradation

```python
async def recommend_route_with_timeout(request):
    try:
        return await asyncio.wait_for(
            recommend_route(request),
            timeout=settings.request_timeout_seconds
        )
    except asyncio.TimeoutError:
        return build_timeout_response(request_id)
```

If an individual agent fails:
- The result is marked as `None` with an error note.
- The orchestrator proceeds with available results.
- The confidence score is reduced proportionally.
- The explanation notes which data was unavailable.

---

## 8. Implementation Order

### 8.1 Phase Breakdown

| Phase | Deliverable | Depends On | Estimated Effort |
|-------|-------------|-----------|-----------------|
| **1** | Project scaffold + config + health endpoint | Nothing | 1 hour |
| **2** | Tool base interface + registry + mock tools | Phase 1 | 2 hours |
| **3** | Mock data files (default scenario) | Phase 2 | 1 hour |
| **4** | Agent base interface + route agent (scoring logic) | Phase 2 | 1.5 hours |
| **5** | Orchestrator (sequential flow, no parallelism yet) | Phase 4 | 1.5 hours |
| **6** | POST /recommend-route endpoint (mock mode) | Phase 3, 5 | 1 hour |
| **7** | Traffic, weather, POI agents | Phase 4 | 2 hours |
| **8** | Parallel agent execution (asyncio.gather) | Phase 7 | 1 hour |
| **9** | Recommendation agent + explanation generation | Phase 8 | 1.5 hours |
| **10** | Agent trace store + GET /agent-trace endpoint | Phase 8 | 1 hour |
| **11** | POST /search-poi endpoint | Phase 2 | 0.5 hours |
| **12** | POST /demo/reset endpoint + scenario switching | Phase 3 | 0.5 hours |
| **13** | Live HERE routing tool (real API calls) | Phase 2 | 1 hour |
| **14** | Live HERE traffic tool | Phase 13 | 1 hour |
| **15** | Live HERE places tool | Phase 13 | 0.5 hours |
| **16** | Live weather tool | Phase 2 | 1 hour |
| **17** | Error handling + graceful degradation | Phase 8 | 1 hour |
| **18** | Additional mock scenarios | Phase 12 | 1 hour |

### 8.2 Critical Path

```
Phase 1 → Phase 2 → Phase 4 → Phase 5 → Phase 6
                 ↓
           Phase 3 (parallel)
```

The earliest a working `/recommend-route` (mock mode) can be demonstrated: after Phase 6.

### 8.3 MVP Milestone Definition

**Minimal Demo (Phases 1-6):**
- Health endpoint works.
- `/recommend-route` returns a mock recommendation with explanation.
- Mock tools return pre-built data.
- Single agent (routing) scoring.

**Full Mock Demo (Phases 1-12):**
- All agents evaluate routes in parallel.
- Explanation references all agent factors.
- Agent trace is inspectable.
- Demo reset switches scenarios.

**Full Live Demo (Phases 1-18):**
- Real HERE API calls for routing, traffic, places.
- Real weather data.
- Graceful degradation on service failures.
- Multiple demo scenarios with fallback to mock.

### 8.4 Run Command

```bash
# Development (mock mode)
cd backend
pip install -r requirements.txt
cp .env.example .env
# Edit .env: APP_MODE=mock
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Production-like (live mode, on EC2)
APP_MODE=live uvicorn main:app --host 0.0.0.0 --port 8000 --workers 2
```

---

*Document Status: Complete*
*Created: June 30, 2026*
*Predecessors: product-vision.md, software-requirements-specification.md, tool-abstraction-architecture.md*
*Next Step: Implement Phase 1 (scaffold + config + health endpoint).*
