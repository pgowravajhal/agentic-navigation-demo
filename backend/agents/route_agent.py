"""Route Agent — evaluates candidate routes by distance and duration."""

from .base import BaseAgent
from tools.here_routing import HereRoutingTool
from logging_config import logger


class RouteAgent(BaseAgent):
    """Evaluates candidate routes based on distance/duration trade-offs."""

    def __init__(self):
        self.tool = HereRoutingTool()

    @property
    def name(self) -> str:
        return "routing"

    async def evaluate(self, context: dict) -> dict:
        logger.info("Route Agent starting evaluation", extra={"agent": self.name})

        # Call routing tool to get candidates
        routes_response = await self.tool.execute({
            "origin": context["origin"],
            "destination": context["destination"],
        })
        routes = routes_response.get("routes", [])

        if not routes:
            logger.warning("No routes returned from routing tool", extra={"agent": self.name})
            return {
                "routes": [],
                "scores": {},
                "reasoning": "No routes could be calculated.",
                "tool_calls": [{"tool": "here_routing", "status": "error"}],
            }

        # Score each route — fastest route gets highest score
        max_duration = max(r["duration_seconds"] for r in routes)
        min_duration = min(r["duration_seconds"] for r in routes)
        duration_range = max_duration - min_duration if max_duration != min_duration else 1

        scores = {}
        for route in routes:
            # Normalize: fastest=0.95, slowest=0.55
            normalized = (max_duration - route["duration_seconds"]) / duration_range
            score = 0.55 + (normalized * 0.40)
            scores[route["id"]] = round(score, 2)

        # Sort routes by score for reasoning
        best_route = max(scores, key=scores.get)
        best = next(r for r in routes if r["id"] == best_route)

        reasoning = (
            f"Evaluated {len(routes)} candidate routes. "
            f"Fastest: {best['label']} at {best['duration_seconds'] // 60} min "
            f"({best['distance_meters'] / 1000:.0f} km). "
            f"Duration spread: {(max_duration - min_duration) // 60} min."
        )

        logger.info(
            f"Route Agent complete: {len(routes)} routes scored",
            extra={"agent": self.name, "duration_ms": 0},
        )

        return {
            "routes": routes,
            "scores": scores,
            "reasoning": reasoning,
            "tool_calls": [{"tool": "here_routing", "status": "success"}],
        }
