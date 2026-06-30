"""Agent Orchestrator — coordinates all agents to produce a route recommendation."""

import asyncio
import time
import uuid
from collections import OrderedDict
from datetime import datetime, timezone, timedelta

from .route_agent import RouteAgent
from .traffic_agent import TrafficAgent
from .weather_agent import WeatherAgent
from .poi_agent import PoiAgent
from .recommendation_agent import RecommendationAgent
from config import settings
from logging_config import logger


class TraceStore:
    """In-memory trace storage with bounded size."""

    def __init__(self, max_size: int = 100):
        self._store: OrderedDict[str, dict] = OrderedDict()
        self._max_size = max_size

    def save(self, request_id: str, trace: dict) -> None:
        if len(self._store) >= self._max_size:
            self._store.popitem(last=False)  # Remove oldest
        self._store[request_id] = trace

    def get(self, request_id: str) -> dict | None:
        return self._store.get(request_id)

    def clear(self) -> int:
        count = len(self._store)
        self._store.clear()
        return count

    @property
    def count(self) -> int:
        return len(self._store)


# Global trace store
trace_store = TraceStore(max_size=settings.trace_retention_count)


def get_trace(request_id: str) -> dict | None:
    return trace_store.get(request_id)


class Orchestrator:
    """Coordinates all agents to produce a route recommendation.

    Execution flow:
    1. Route Agent gets candidate routes
    2. Traffic, Weather, and POI agents evaluate routes in parallel
    3. Recommendation Agent aggregates and explains
    """

    def __init__(self):
        self.route_agent = RouteAgent()
        self.traffic_agent = TrafficAgent()
        self.weather_agent = WeatherAgent()
        self.poi_agent = PoiAgent()
        self.recommendation_agent = RecommendationAgent()

    async def recommend(self, origin: dict, destination: dict, preferences: dict) -> dict:
        request_id = f"req-{uuid.uuid4().hex[:8]}"
        start_time = time.time()
        trace_entries = []

        logger.info(
            f"Starting recommendation: {origin.get('label', '?')} → {destination.get('label', '?')}",
            extra={"request_id": request_id},
        )

        # --- Step 1: Get candidate routes ---
        t0 = time.time()
        try:
            route_result = await asyncio.wait_for(
                self.route_agent.evaluate({"origin": origin, "destination": destination}),
                timeout=settings.agent_timeout_seconds,
            )
        except asyncio.TimeoutError:
            route_result = {"routes": [], "scores": {}, "reasoning": "Route agent timed out", "tool_calls": []}
        except Exception as e:
            logger.error(f"Route agent failed: {e}", extra={"request_id": request_id})
            route_result = {"routes": [], "scores": {}, "reasoning": f"Error: {e}", "tool_calls": []}

        route_duration_ms = int((time.time() - t0) * 1000)
        routes = route_result.get("routes", [])

        trace_entries.append({
            "name": "routing",
            "status": "success" if routes else "error",
            "duration_ms": route_duration_ms,
            "input_summary": f"{origin.get('label', 'Origin')} → {destination.get('label', 'Destination')}, {preferences.get('transport_mode', 'car')}",
            "output_summary": route_result.get("reasoning", f"{len(routes)} routes"),
            "score": max(route_result.get("scores", {}).values()) if route_result.get("scores") else None,
            "tool_calls": [
                {"tool": tc["tool"], "duration_ms": route_duration_ms, "status": tc["status"]}
                for tc in route_result.get("tool_calls", [])
            ],
        })

        if not routes:
            return self._build_error_response(request_id, start_time, trace_entries, "No routes found")

        # --- Step 2: Run evaluation agents in parallel ---
        t1 = time.time()
        context_with_routes = {"routes": routes}

        async def safe_evaluate(agent, ctx):
            """Run agent with timeout and error handling."""
            try:
                return await asyncio.wait_for(
                    agent.evaluate(ctx),
                    timeout=settings.agent_timeout_seconds,
                )
            except asyncio.TimeoutError:
                logger.warning(f"{agent.name} agent timed out", extra={"request_id": request_id})
                return {"scores": {}, "reasoning": f"{agent.name} timed out", "tool_calls": [], "_failed": True}
            except Exception as e:
                logger.error(f"{agent.name} agent failed: {e}", extra={"request_id": request_id})
                return {"scores": {}, "reasoning": f"Error: {e}", "tool_calls": [], "_failed": True}

        traffic_result, weather_result, poi_result = await asyncio.gather(
            safe_evaluate(self.traffic_agent, context_with_routes),
            safe_evaluate(self.weather_agent, context_with_routes),
            safe_evaluate(self.poi_agent, context_with_routes),
        )
        parallel_duration_ms = int((time.time() - t1) * 1000)

        # Record traffic trace
        trace_entries.append({
            "name": "traffic",
            "status": "error" if traffic_result.get("_failed") else "success",
            "duration_ms": parallel_duration_ms,
            "input_summary": f"{len(routes)} route corridors",
            "output_summary": traffic_result.get("reasoning", ""),
            "score": max(traffic_result.get("scores", {}).values()) if traffic_result.get("scores") else None,
            "tool_calls": [
                {"tool": tc["tool"], "duration_ms": parallel_duration_ms, "status": tc["status"]}
                for tc in traffic_result.get("tool_calls", [])
            ],
        })

        # Record weather trace
        trace_entries.append({
            "name": "weather",
            "status": "error" if weather_result.get("_failed") else "success",
            "duration_ms": parallel_duration_ms,
            "input_summary": f"{len(routes)} route corridors",
            "output_summary": weather_result.get("reasoning", ""),
            "score": max(weather_result.get("scores", {}).values()) if weather_result.get("scores") else None,
            "tool_calls": [
                {"tool": tc["tool"], "duration_ms": parallel_duration_ms, "status": tc["status"]}
                for tc in weather_result.get("tool_calls", [])
            ],
        })

        # Record POI trace
        trace_entries.append({
            "name": "poi",
            "status": "error" if poi_result.get("_failed") else "success",
            "duration_ms": parallel_duration_ms,
            "input_summary": f"{len(routes)} route corridors",
            "output_summary": poi_result.get("reasoning", ""),
            "score": max(poi_result.get("scores", {}).values()) if poi_result.get("scores") else None,
            "tool_calls": [
                {"tool": tc["tool"], "duration_ms": parallel_duration_ms, "status": tc["status"]}
                for tc in poi_result.get("tool_calls", [])
            ],
        })

        # --- Step 3: Recommendation agent aggregates and explains ---
        t2 = time.time()
        rec_context = {
            "routes": routes,
            "route_scores": route_result.get("scores", {}),
            "traffic_scores": traffic_result.get("scores", {}),
            "weather_scores": weather_result.get("scores", {}),
            "poi_scores": poi_result.get("scores", {}),
            "traffic_data": traffic_result.get("traffic_data", []),
            "weather_data": weather_result.get("weather_data", []),
        }
        rec_result = await self.recommendation_agent.evaluate(rec_context)
        rec_duration_ms = int((time.time() - t2) * 1000)

        trace_entries.append({
            "name": "recommendation",
            "status": "success",
            "duration_ms": rec_duration_ms,
            "input_summary": "All agent scores + route data",
            "output_summary": f"Recommended: {rec_result['recommended_id']} (confidence: {rec_result['explanation']['confidence']:.2f})",
            "score": None,
            "tool_calls": [],
        })

        total_duration_ms = int((time.time() - start_time) * 1000)

        # --- Build trace ---
        trace = {
            "request_id": request_id,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "total_duration_ms": total_duration_ms,
            "agents": trace_entries,
            "final_decision": {
                "recommended": rec_result["recommended_id"],
                "confidence": rec_result["explanation"]["confidence"],
                "method": "weighted_score",
            },
        }
        trace_store.save(request_id, trace)

        # --- Build response ---
        recommended_id = rec_result["recommended_id"]
        all_scores = rec_result["scores"]
        recommended_route = next((r for r in routes if r["id"] == recommended_id), routes[0])
        alternatives = [r for r in routes if r["id"] != recommended_id]

        # Traffic summary for recommended route
        traffic_for_rec = next(
            (t for t in traffic_result.get("traffic_data", []) if t["route_id"] == recommended_id),
            {"level": "unknown", "summary": "No traffic data available"},
        )

        # Weather summary
        weather_conditions = weather_result.get("conditions", "Unknown")
        weather_alerts = weather_result.get("alerts", [])

        # POIs for recommended route
        pois_by_route = poi_result.get("pois_by_route", {})
        pois_for_rec = pois_by_route.get(recommended_id, [])

        # Agents consulted
        agents_consulted = ["routing", "traffic", "weather", "poi", "recommendation"]
        failed_agents = [e["name"] for e in trace_entries if e["status"] == "error"]

        logger.info(
            f"Recommendation complete: {recommended_id} in {total_duration_ms}ms",
            extra={"request_id": request_id, "duration_ms": total_duration_ms},
        )

        return {
            "request_id": request_id,
            "recommended_route": self._format_route(recommended_route, all_scores.get(recommended_id, {}), True),
            "alternatives": [
                self._format_route(r, all_scores.get(r["id"], {}), False) for r in alternatives
            ],
            "explanation": rec_result["explanation"],
            "traffic_summary": {
                "level": traffic_for_rec.get("level", "unknown"),
                "summary": traffic_for_rec.get("summary", "No traffic data"),
            },
            "weather_summary": {
                "conditions": weather_conditions,
                "alerts": weather_alerts,
            },
            "pois": pois_for_rec,
            "agent_trace": trace,
            "metadata": {
                "processing_time_ms": total_duration_ms,
                "agents_consulted": agents_consulted,
                "failed_agents": failed_agents,
                "mode": settings.app_mode,
            },
        }

    def _format_route(self, route: dict, scores: dict, is_recommended: bool) -> dict:
        """Format a route dict for the API response."""
        duration_seconds = route.get("duration_seconds", 0)
        now = datetime.now(timezone.utc)
        eta = (now + timedelta(seconds=duration_seconds)).strftime("%H:%M")

        return {
            "id": route["id"],
            "label": route["label"],
            "summary": {
                "distance_km": round(route.get("distance_meters", 0) / 1000, 1),
                "duration_minutes": duration_seconds // 60,
                "eta": eta,
            },
            "scores": {
                "overall": scores.get("overall", 0.0),
                "routing": scores.get("routing", 0.0),
                "traffic": scores.get("traffic", 0.0),
                "weather": scores.get("weather", 0.0),
                "poi": scores.get("poi", 0.0),
            },
            "is_recommended": is_recommended,
        }

    def _build_error_response(self, request_id: str, start_time: float, traces: list, message: str) -> dict:
        """Build an error/empty response when routing fails."""
        total_ms = int((time.time() - start_time) * 1000)
        trace = {
            "request_id": request_id,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "total_duration_ms": total_ms,
            "agents": traces,
            "final_decision": {"recommended": None, "confidence": 0, "method": "none"},
        }
        trace_store.save(request_id, trace)

        return {
            "request_id": request_id,
            "recommended_route": {
                "id": "none",
                "label": "No route available",
                "summary": {"distance_km": 0, "duration_minutes": 0, "eta": "N/A"},
                "scores": {"overall": 0, "routing": 0, "traffic": 0, "weather": 0, "poi": 0},
                "is_recommended": False,
            },
            "alternatives": [],
            "explanation": {
                "summary": message,
                "factors": [],
                "trade_offs": "",
                "confidence": 0.0,
            },
            "traffic_summary": {"level": "unknown", "summary": "No data"},
            "weather_summary": {"conditions": "Unknown", "alerts": []},
            "pois": [],
            "agent_trace": trace,
            "metadata": {
                "processing_time_ms": total_ms,
                "agents_consulted": ["routing"],
                "failed_agents": ["routing"],
                "mode": settings.app_mode,
            },
        }
