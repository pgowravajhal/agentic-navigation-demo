# Tool Abstraction and MCP Readiness Architecture
## Accelerating the V-Cycle with Agentic AI — Navigation Application

| Field | Value |
|-------|-------|
| Document Version | 1.0 |
| Status | Draft |
| Date | June 30, 2026 |
| Context | AWS Hackathon — Tool-based design with MCP evolution path |

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Tool Abstraction Layer](#2-tool-abstraction-layer)
3. [Tool Definitions](#3-tool-definitions)
4. [Agent-Tool Consumption Matrix](#4-agent-tool-consumption-matrix)
5. [MCP Readiness Strategy](#5-mcp-readiness-strategy)
6. [Future MCP Server Boundaries](#6-future-mcp-server-boundaries)
7. [Evolution Path](#7-evolution-path)

---

## 1. Design Philosophy

### 1.1 Core Principle: Tools as the Abstraction Boundary

Agents do not call external APIs directly. Instead, they invoke **tools** — well-defined
capabilities with explicit input/output contracts. This indirection provides:

- **Decoupling:** Agents are unaware of underlying API details (endpoints, auth, pagination).
- **Testability:** Tools can be mocked independently of agents.
- **Portability:** Swapping a HERE API for a different provider requires changing only the tool adapter.
- **MCP Readiness:** Tools already match the MCP tool model (name, description, input schema, output schema).

### 1.2 Hackathon MVP Approach

For the hackathon, tools are implemented as **REST-based adapters** — lightweight backend
functions that wrap external APIs behind a uniform interface. Each tool:

1. Accepts a JSON input conforming to its schema.
2. Calls the underlying external service.
3. Returns a normalized JSON output.
4. Handles errors and returns structured error responses.

### 1.3 Future MCP Approach

Post-hackathon, each tool (or group of related tools) can be promoted to an **MCP server**
that exposes the same interface via the Model Context Protocol. Agent logic remains unchanged
because agents interact with tool interfaces, not transport mechanisms.

---

## 2. Tool Abstraction Layer

### 2.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      AGENT LAYER                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Routing  │  │ Traffic  │  │ Weather  │  │   POI    │   │
│  │  Agent   │  │  Agent   │  │  Agent   │  │  Agent   │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
│       │              │              │              │         │
└───────┼──────────────┼──────────────┼──────────────┼─────────┘
        │              │              │              │
┌───────┼──────────────┼──────────────┼──────────────┼─────────┐
│       ▼              ▼              ▼              ▼         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              TOOL REGISTRY / DISPATCHER              │    │
│  │         (resolves tool name → tool adapter)          │    │
│  └────┬────────┬────────┬────────┬────────┬────────────┘    │
│       │        │        │        │        │                  │
│       ▼        ▼        ▼        ▼        ▼                  │
│  ┌────────┐┌────────┐┌────────┐┌────────┐┌────────────┐    │
│  │HERE    ││HERE    ││HERE    ││Weather ││Route       │    │
│  │Routing ││Traffic ││Places  ││Tool    ││Explanation │    │
│  │Tool    ││Tool    ││Tool    ││        ││Tool        │    │
│  └───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────────┘    │
│      │         │         │         │         │              │
│  TOOL ABSTRACTION LAYER (uniform interface)                  │
└──────┼─────────┼─────────┼─────────┼─────────┼──────────────┘
       │         │         │         │         │
       ▼         ▼         ▼         ▼         ▼
┌─────────────────────────────────────────────────────────────┐
│                   EXTERNAL SERVICES                          │
│  HERE Routing  HERE Traffic  HERE Places  Weather API  LLM  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Tool Interface Contract

Every tool implements a uniform interface:

```
ToolInterface {
    name: string              // Unique tool identifier
    description: string       // Human-readable description of what the tool does
    inputSchema: JSONSchema   // Defines expected input structure
    outputSchema: JSONSchema  // Defines guaranteed output structure
    execute(input): output    // Performs the tool's function
}
```

### 2.3 Tool Registry

The Tool Registry is the single lookup mechanism that maps a tool name to its adapter
implementation. This is the seam where transport can later change from direct function
call to MCP protocol without affecting callers.

```
ToolRegistry {
    register(toolName, toolAdapter)
    invoke(toolName, input) → output
    list() → [ToolDescriptor]
}
```

---

## 3. Tool Definitions

### 3.1 HERE Routing Tool

| Field | Value |
|-------|-------|
| **Name** | `here_routing` |
| **Description** | Calculates one or more candidate routes between an origin and destination, returning geometry, distance, and duration for each. |
| **Consumed By** | Routing Agent, Orchestrator |

**Input Schema:**

```json
{
  "origin": {
    "latitude": 52.5200,
    "longitude": 13.4050
  },
  "destination": {
    "latitude": 48.8566,
    "longitude": 2.3522
  },
  "transportMode": "car",
  "alternatives": 3,
  "departureTime": "2026-06-30T08:00:00Z"
}
```

**Output Schema:**

```json
{
  "routes": [
    {
      "id": "route-1",
      "summary": {
        "distance_meters": 1050000,
        "duration_seconds": 36000,
        "base_duration_seconds": 34000
      },
      "geometry": {
        "polyline": "encoded-polyline-string"
      },
      "sections": [
        {
          "departure": { "latitude": 52.52, "longitude": 13.405 },
          "arrival": { "latitude": 48.8566, "longitude": 2.3522 },
          "transport": { "mode": "car" }
        }
      ]
    }
  ],
  "metadata": {
    "timestamp": "2026-06-30T08:00:01Z",
    "provider": "HERE",
    "api_version": "v8"
  }
}
```

**Error Response:**

```json
{
  "error": {
    "code": "ROUTING_FAILED",
    "message": "Unable to calculate route between specified points",
    "retryable": true
  }
}
```

---

### 3.2 HERE Traffic Tool

| Field | Value |
|-------|-------|
| **Name** | `here_traffic` |
| **Description** | Retrieves real-time traffic flow data and incidents for a given route corridor or bounding box. |
| **Consumed By** | Traffic Agent |

**Input Schema:**

```json
{
  "corridor": {
    "polyline": "encoded-polyline-string",
    "width_meters": 1000
  },
  "alternatively": {
    "bounding_box": {
      "north": 52.55,
      "south": 48.80,
      "east": 13.50,
      "west": 2.30
    }
  }
}
```

**Output Schema:**

```json
{
  "flow": [
    {
      "segment_id": "seg-001",
      "from": { "latitude": 52.50, "longitude": 13.40 },
      "to": { "latitude": 52.45, "longitude": 13.35 },
      "current_speed_kmh": 45,
      "free_flow_speed_kmh": 80,
      "congestion_level": "heavy",
      "jam_factor": 7.2
    }
  ],
  "incidents": [
    {
      "id": "inc-001",
      "type": "accident",
      "location": { "latitude": 51.00, "longitude": 10.00 },
      "description": "Multi-vehicle accident blocking right lane",
      "severity": "major",
      "start_time": "2026-06-30T06:30:00Z",
      "estimated_end_time": "2026-06-30T10:00:00Z"
    }
  ],
  "metadata": {
    "timestamp": "2026-06-30T08:00:02Z",
    "provider": "HERE",
    "data_freshness_seconds": 120
  }
}
```

**Error Response:**

```json
{
  "error": {
    "code": "TRAFFIC_UNAVAILABLE",
    "message": "Traffic data not available for the specified corridor",
    "retryable": true
  }
}
```

---

### 3.3 HERE Places Tool

| Field | Value |
|-------|-------|
| **Name** | `here_places` |
| **Description** | Searches for points of interest along or near a route corridor, filtered by category. |
| **Consumed By** | POI Agent |

**Input Schema:**

```json
{
  "corridor": {
    "polyline": "encoded-polyline-string",
    "width_meters": 5000
  },
  "categories": ["fuel-station", "rest-area", "ev-charging", "restaurant"],
  "limit": 20
}
```

**Output Schema:**

```json
{
  "places": [
    {
      "id": "place-001",
      "name": "Autohof Geiselwind",
      "category": "fuel-station",
      "location": { "latitude": 49.77, "longitude": 10.47 },
      "distance_from_route_meters": 200,
      "address": "A3 Ausfahrt 76, 96160 Geiselwind, Germany",
      "open_now": true
    }
  ],
  "metadata": {
    "timestamp": "2026-06-30T08:00:03Z",
    "provider": "HERE",
    "total_results": 15,
    "returned_results": 15
  }
}
```

**Error Response:**

```json
{
  "error": {
    "code": "PLACES_SEARCH_FAILED",
    "message": "Unable to retrieve points of interest for the corridor",
    "retryable": true
  }
}
```

---

### 3.4 Weather Tool

| Field | Value |
|-------|-------|
| **Name** | `weather_forecast` |
| **Description** | Retrieves current weather conditions and short-term forecasts for a set of waypoints along a route. |
| **Consumed By** | Weather Agent |

**Input Schema:**

```json
{
  "waypoints": [
    { "latitude": 52.52, "longitude": 13.405, "eta_offset_minutes": 0 },
    { "latitude": 51.00, "longitude": 10.00, "eta_offset_minutes": 120 },
    { "latitude": 48.85, "longitude": 2.35, "eta_offset_minutes": 360 }
  ],
  "include_alerts": true
}
```

**Output Schema:**

```json
{
  "conditions": [
    {
      "waypoint_index": 0,
      "location": { "latitude": 52.52, "longitude": 13.405 },
      "current": {
        "temperature_celsius": 18,
        "condition": "partly_cloudy",
        "precipitation_mm": 0,
        "wind_speed_kmh": 15,
        "visibility_km": 10
      },
      "forecast_at_eta": {
        "temperature_celsius": 20,
        "condition": "clear",
        "precipitation_probability_pct": 10,
        "wind_speed_kmh": 12
      }
    }
  ],
  "alerts": [
    {
      "type": "heavy_rain",
      "severity": "moderate",
      "affected_waypoints": [1],
      "description": "Heavy rainfall expected between 10:00 and 12:00",
      "start_time": "2026-06-30T10:00:00Z",
      "end_time": "2026-06-30T12:00:00Z"
    }
  ],
  "metadata": {
    "timestamp": "2026-06-30T08:00:04Z",
    "provider": "weather-service",
    "data_freshness_seconds": 600
  }
}
```

**Error Response:**

```json
{
  "error": {
    "code": "WEATHER_UNAVAILABLE",
    "message": "Weather data not available for one or more waypoints",
    "retryable": true,
    "partial_data_available": true
  }
}
```

---

### 3.5 Route Explanation Tool

| Field | Value |
|-------|-------|
| **Name** | `route_explanation` |
| **Description** | Generates a natural-language explanation of the route recommendation based on structured agent evaluations. Grounds the explanation in data to prevent hallucination. |
| **Consumed By** | Orchestrator |

**Input Schema:**

```json
{
  "recommended_route": {
    "id": "route-1",
    "distance_meters": 1050000,
    "duration_seconds": 36000
  },
  "alternatives": [
    {
      "id": "route-2",
      "distance_meters": 980000,
      "duration_seconds": 39000
    }
  ],
  "agent_evaluations": [
    {
      "agent": "routing",
      "recommended_score": 0.85,
      "reasoning": "Fastest route despite being 70km longer"
    },
    {
      "agent": "traffic",
      "recommended_score": 0.90,
      "reasoning": "Route-1 avoids major congestion on A3"
    },
    {
      "agent": "weather",
      "recommended_score": 0.70,
      "reasoning": "Light rain expected on both routes; no significant difference"
    }
  ],
  "context": {
    "departure_time": "2026-06-30T08:00:00Z",
    "transport_mode": "car"
  }
}
```

**Output Schema:**

```json
{
  "explanation": {
    "summary": "Route 1 is recommended because it saves approximately 50 minutes compared to Route 2 by avoiding heavy congestion on the A3 corridor, despite being 70km longer.",
    "factors": [
      {
        "factor": "traffic",
        "influence": "high",
        "detail": "Route 2 passes through a major accident zone on A3 with estimated 45-minute delays."
      },
      {
        "factor": "duration",
        "influence": "high",
        "detail": "Estimated arrival 50 minutes earlier on Route 1."
      },
      {
        "factor": "weather",
        "influence": "low",
        "detail": "Light rain expected on both routes with no material difference in safety."
      }
    ],
    "trade_offs": "Route 2 is 70km shorter and uses less fuel, but current traffic conditions make it significantly slower.",
    "confidence": 0.85
  },
  "metadata": {
    "timestamp": "2026-06-30T08:00:05Z",
    "model": "llm-model-id",
    "grounded_in_data": true
  }
}
```

**Error Response:**

```json
{
  "error": {
    "code": "EXPLANATION_GENERATION_FAILED",
    "message": "Unable to generate route explanation",
    "retryable": true,
    "fallback": "Route 1 recommended based on fastest estimated arrival time."
  }
}
```

---

## 4. Agent-Tool Consumption Matrix

| Tool | Routing Agent | Traffic Agent | Weather Agent | POI Agent | Orchestrator |
|------|:---:|:---:|:---:|:---:|:---:|
| `here_routing` | ✅ Primary | — | — | — | ✅ Trigger |
| `here_traffic` | — | ✅ Primary | — | — | — |
| `here_places` | — | — | — | ✅ Primary | — |
| `weather_forecast` | — | — | ✅ Primary | — | — |
| `route_explanation` | — | — | — | — | ✅ Primary |

### 4.1 Data Flow

```
User Request
    │
    ▼
Orchestrator
    │
    ├──► here_routing tool ──► Routing Agent evaluates candidates
    │
    ├──► here_traffic tool ──► Traffic Agent scores each route
    │
    ├──► weather_forecast tool ──► Weather Agent scores each route
    │
    ├──► here_places tool ──► POI Agent scores each route
    │
    ▼
Orchestrator aggregates scores
    │
    ├──► route_explanation tool ──► Generates natural-language explanation
    │
    ▼
Recommendation returned to user
```

### 4.2 Tool Invocation Patterns

| Pattern | Description | Example |
|---------|------------|---------|
| **Sequential** | One tool feeds the next | `here_routing` → routes feed into traffic/weather/POI tools |
| **Parallel** | Multiple tools invoked simultaneously | `here_traffic` + `weather_forecast` + `here_places` run in parallel on the same routes |
| **Aggregation** | Results combined by orchestrator | All agent scores merged before calling `route_explanation` |
| **Fallback** | Alternative path on failure | If `weather_forecast` fails, orchestrator proceeds without weather scores |

---

## 5. MCP Readiness Strategy

### 5.1 Why the Tool Abstraction Enables MCP

The Model Context Protocol defines tools as:

```
Tool {
    name: string
    description: string
    inputSchema: JSON Schema
}
```

Our tool interface already mirrors this structure exactly. The difference between
the hackathon MVP and a full MCP deployment is **transport only**:

| Aspect | Hackathon MVP | MCP Future |
|--------|--------------|------------|
| Tool discovery | Tool Registry (in-process) | MCP `tools/list` method |
| Tool invocation | Direct function call or REST | MCP `tools/call` method |
| Transport | In-process / HTTP | stdio, SSE, or HTTP (MCP transports) |
| Schema format | JSON Schema | JSON Schema (identical) |
| Agent logic | Calls `registry.invoke(name, input)` | Calls `mcp_client.call_tool(name, input)` |

### 5.2 Design Decisions That Preserve MCP Compatibility

| Decision | Rationale |
|----------|-----------|
| Tools use JSON Schema for inputs/outputs | MCP requires JSON Schema for tool definitions |
| Tools are stateless | MCP tools are stateless by specification |
| Tools return structured JSON (not raw API responses) | MCP responses must be self-contained content blocks |
| Tool names follow `provider_capability` naming | Maps cleanly to MCP tool naming conventions |
| Agents reference tools by name, not by implementation | Allows swapping registry for MCP client transparently |
| Error responses are structured with codes | MCP `isError` flag maps to our error envelope |

### 5.3 Migration Path (No Agent Code Changes Required)

**Step 1: Hackathon MVP (Current)**
```
Agent → ToolRegistry.invoke("here_routing", input) → HereRoutingAdapter → HERE API
```

**Step 2: Introduce MCP Client (Post-Hackathon)**
```
Agent → MCPClient.call_tool("here_routing", input) → MCP Server → HereRoutingAdapter → HERE API
```

**Step 3: Distributed MCP Servers**
```
Agent → MCPClient.call_tool("here_routing", input) → [network] → Dedicated MCP Server → HERE API
```

The key insight: if the `ToolRegistry.invoke()` signature and the `MCPClient.call_tool()`
signature are kept identical (name + JSON input → JSON output), then agents never need
to know which transport is in use.

### 5.4 Adapter Pattern for Transition

```
// Hackathon: ToolRegistry wraps adapters directly
class ToolRegistry {
    invoke(toolName, input) {
        adapter = this.adapters[toolName]
        return adapter.execute(input)
    }
}

// Post-hackathon: ToolRegistry delegates to MCP client
class ToolRegistry {
    invoke(toolName, input) {
        if (this.mcpServers[toolName]) {
            return this.mcpClient.callTool(toolName, input)
        }
        // Fallback to local adapter
        return this.adapters[toolName].execute(input)
    }
}
```

This hybrid approach allows incremental migration — one tool at a time can be moved
behind an MCP server without disrupting others.

---

## 6. Future MCP Server Boundaries

### 6.1 Proposed MCP Server Decomposition

Each MCP server groups related tools that share configuration, credentials, and lifecycle:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        MCP CLIENT (Orchestrator)                     │
└───────┬──────────────┬──────────────┬──────────────┬────────────────┘
        │              │              │              │
        ▼              ▼              ▼              ▼
┌──────────────┐┌──────────────┐┌──────────────┐┌──────────────────┐
│  HERE MCP    ││  Weather     ││  Explanation ││  Geocoding MCP   │
│  Server      ││  MCP Server  ││  MCP Server  ││  Server          │
│              ││              ││              ││                  │
│ • here_      ││ • weather_   ││ • route_     ││ • geocode        │
│   routing    ││   forecast   ││   explanation││ • reverse_       │
│ • here_      ││              ││              ││   geocode        │
│   traffic    ││              ││              ││ • autocomplete   │
│ • here_      ││              ││              ││                  │
│   places     ││              ││              ││                  │
└──────────────┘└──────────────┘└──────────────┘└──────────────────┘
```

### 6.2 Server Boundary Rationale

| MCP Server | Tools Included | Boundary Rationale |
|------------|---------------|-------------------|
| **HERE Navigation Server** | `here_routing`, `here_traffic`, `here_places` | Shared HERE API credentials, rate limits, and authentication lifecycle |
| **Weather Server** | `weather_forecast` | Separate provider credentials; may swap providers independently |
| **Explanation Server** | `route_explanation` | LLM-dependent; different scaling, cost, and latency profile |
| **Geocoding Server** | `geocode`, `reverse_geocode`, `autocomplete` | User-facing latency sensitivity; high call frequency; could use different provider |

### 6.3 MCP Server Configuration (Future)

Each server would be configured similar to standard MCP patterns:

```json
{
  "mcpServers": {
    "here-navigation": {
      "command": "node",
      "args": ["./mcp-servers/here-navigation/index.js"],
      "env": {
        "HERE_API_KEY": "${HERE_API_KEY}"
      }
    },
    "weather": {
      "command": "python",
      "args": ["-m", "weather_mcp_server"],
      "env": {
        "WEATHER_API_KEY": "${WEATHER_API_KEY}"
      }
    },
    "explanation": {
      "command": "node",
      "args": ["./mcp-servers/explanation/index.js"],
      "env": {
        "LLM_ENDPOINT": "${LLM_ENDPOINT}"
      }
    },
    "geocoding": {
      "command": "node",
      "args": ["./mcp-servers/geocoding/index.js"],
      "env": {
        "HERE_API_KEY": "${HERE_API_KEY}"
      }
    }
  }
}
```

### 6.4 MCP Tool Definitions (Future Format)

When exposed as MCP tools, the definitions would look like:

```json
{
  "tools": [
    {
      "name": "here_routing",
      "description": "Calculates candidate routes between origin and destination using HERE Routing API v8. Returns geometry, distance, and duration for each route.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "origin": {
            "type": "object",
            "properties": {
              "latitude": { "type": "number" },
              "longitude": { "type": "number" }
            },
            "required": ["latitude", "longitude"]
          },
          "destination": {
            "type": "object",
            "properties": {
              "latitude": { "type": "number" },
              "longitude": { "type": "number" }
            },
            "required": ["latitude", "longitude"]
          },
          "transportMode": { "type": "string", "enum": ["car"] },
          "alternatives": { "type": "integer", "minimum": 1, "maximum": 5 },
          "departureTime": { "type": "string", "format": "date-time" }
        },
        "required": ["origin", "destination"]
      }
    }
  ]
}
```

---

## 7. Evolution Path

### 7.1 Phase Summary

| Phase | Transport | Deployment | Agent Changes |
|-------|-----------|-----------|---------------|
| **Phase 1: Hackathon MVP** | In-process function calls / REST adapters | Single backend service | N/A (initial) |
| **Phase 2: Local MCP** | stdio-based MCP | Co-located MCP server processes | None — registry swaps to MCP client |
| **Phase 3: Remote MCP** | SSE or HTTP-based MCP | Distributed MCP servers | None — transport is transparent |
| **Phase 4: Multi-tenant** | HTTP MCP with auth | Independent deployable services | None — auth handled at transport layer |

### 7.2 What Changes at Each Phase

**Phase 1 → Phase 2:**
- Add MCP server wrapper around existing tool adapters
- Replace `ToolRegistry.invoke()` internals with MCP client calls
- Add `tools/list` and `tools/call` handlers to servers
- Zero changes to agent prompt logic or scoring algorithms

**Phase 2 → Phase 3:**
- Switch MCP transport from stdio to SSE/HTTP
- Deploy MCP servers to separate containers or Lambda functions
- Add network resilience (timeouts, retries) at MCP client level
- Zero changes to tool schemas or agent logic

**Phase 3 → Phase 4:**
- Add OAuth/API key authentication to MCP endpoints
- Implement rate limiting per client
- Add monitoring and usage tracking at server level
- Zero changes to tool implementations

### 7.3 Key Architectural Invariants (Must Not Change)

These invariants hold across all phases:

1. **Agents invoke tools by name** — never by endpoint URL or transport detail.
2. **Tool inputs and outputs are JSON** — conforming to declared schemas.
3. **Tools are stateless** — no session or conversation state held within a tool.
4. **Tools handle their own errors** — returning structured error objects, not throwing transport exceptions at agents.
5. **The orchestrator decides tool invocation order** — tools do not call other tools directly.

---

## Summary

This architecture ensures the hackathon MVP is simple and deliverable (REST adapters behind a
registry) while being structurally ready for MCP without requiring any refactoring of agent logic.
The tool abstraction boundary is the key enabler — it decouples "what capability do I need" from
"how do I reach it."

---

*Document Status: Complete Draft*
*Created: June 30, 2026*
*Predecessor: software-requirements-specification.md*
*Next Step: System architecture and agent design (when ready to proceed beyond requirements).*
