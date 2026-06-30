"""Traffic Agent — evaluates routes based on real-time traffic conditions."""

from .base import BaseAgent
from tools.here_traffic import HereTrafficTool
from logging_config import logger


class TrafficAgent(BaseAgent):
    """Evaluates routes by current traffic congestion and incidents."""

    def __init__(self):
        self.tool = HereTrafficTool()

    @property
    def name(self) -> str:
        return "traffic"

    async def evaluate(self, context: dict) -> dict:
        logger.info("Traffic Agent starting evaluation", extra={"agent": self.name})

        routes = context.get("routes", [])
        if not routes:
            return {
                "scores": {},
                "traffic_data": [],
                "reasoning": "No routes to evaluate.",
                "tool_calls": [{"tool": "here_traffic", "status": "skipped"}],
            }

        traffic_response = await self.tool.execute({"routes": routes})
        traffic_data = traffic_response.get("traffic", [])

        # Score routes: lower jam factor = higher score
        scores = {}
        for item in traffic_data:
            route_id = item["route_id"]
            jam = item.get("jam_factor", 0)
            # jam_factor: 0-10 scale. 0=free flow, 10=standstill
            score = max(0.2, 1.0 - (jam / 10.0))
            scores[route_id] = round(score, 2)

        # Generate reasoning
        worst = max(traffic_data, key=lambda x: x.get("delay_minutes", 0))
        best_route_id = max(scores, key=scores.get)
        best_traffic = next((t for t in traffic_data if t["route_id"] == best_route_id), {})

        reasoning = (
            f"Traffic analysis of {len(routes)} routes complete. "
            f"Best: {best_traffic.get('summary', 'clear')} (jam factor {best_traffic.get('jam_factor', 0):.1f}). "
            f"Worst: {worst.get('summary', 'unknown')} ({worst.get('delay_minutes', 0)} min delay)."
        )

        logger.info(
            f"Traffic Agent complete: worst delay {worst.get('delay_minutes', 0)} min",
            extra={"agent": self.name},
        )

        return {
            "scores": scores,
            "traffic_data": traffic_data,
            "reasoning": reasoning,
            "tool_calls": [{"tool": "here_traffic", "status": "success"}],
        }
