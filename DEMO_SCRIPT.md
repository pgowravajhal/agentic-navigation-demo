# Demo Script — Agentic Navigator

## Pre-Demo Checklist

- [ ] Backend running: `cd backend && python main.py`
- [ ] Verify: `curl http://localhost:8000/health` returns `{"status": "healthy"}`
- [ ] Android emulator running (API 30+)
- [ ] App installed and launched

## Demo Flow (2-3 minutes)

### Act 1: The Problem (30 seconds)

**Say:** "Traditional navigation apps use a single routing engine. You get a route
but never understand why it was chosen or what alternatives were considered."

### Act 2: Multi-Agent Recommendation (60 seconds)

1. Show the Home screen
2. Origin is pre-set to "Berlin"
3. Tap "Paris" chip as destination
4. Tap "🚀 Recommend Route"
5. Wait for the recommendation to appear

**Say:** "Our app uses multiple AI agents — each specialized in a different aspect
of the journey. A Route Agent evaluates candidates, a Traffic Agent scores congestion,
and a Weather Agent checks conditions along the corridor. They collaborate to recommend
the best route."

6. Show the recommended route card (ETA, distance, confidence score)
7. Point out the traffic and weather summaries
8. Read the explanation aloud

**Say:** "The app doesn't just tell you which route — it explains WHY. In this case,
Route 1 avoids heavy traffic on the A3, saving 50 minutes despite being slightly longer."

### Act 3: Agent Transparency (60 seconds)

1. Tap "Agent Insights"
2. Show the trace screen with all agent cards

**Say:** "For complete transparency, we can inspect exactly what each agent did.
The Routing Agent found 3 candidates. The Traffic Agent detected heavy congestion
on one route. The Weather Agent checked conditions at waypoints. And the
Recommendation Agent aggregated everything with weighted scoring."

3. Point out individual agent scores, tool calls, and timing

### Act 4: V-Cycle Acceleration (30 seconds)

**Say:** "Beyond the runtime application, this project demonstrates V-cycle
acceleration. Our requirements, architecture, API specifications, and this
demo script were all generated with agentic AI — accelerating every phase
from requirements to verification."

## Backup Plan

If the backend is unreachable during demo:
- The mock mode ensures consistent responses
- Pre-captured screenshots are available as backup

## Key Messages

- Multi-agent collaboration for intelligent decisions
- Explainability — not just what, but WHY
- MCP-ready tool abstraction for future evolution
- V-cycle acceleration throughout the engineering workflow
