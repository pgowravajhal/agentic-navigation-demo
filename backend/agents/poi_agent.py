"""POI Agent — evaluates routes based on availability of points of interest."""

from .base import BaseAgent
from tools.here_places import HerePlacesTool
from logging_config import logger


class PoiAgent(BaseAgent):
    """Evaluates routes by availability of useful POIs (fuel, rest stops, charging)."""

    def __init__(self):
        self.tool = HerePlacesTool()

    @property
    def name(self) -> str:
        return "poi"

    async def evaluate(self, context: dict) -> dict:
        logger.info("POI Agent starting evaluation", extra={"agent": self.name})

        routes = context.get("routes", [])
        if not routes:
            return {
                "scores": {},
                "pois_by_route": {},
                "reasoning": "No routes to evaluate.",
                "tool_calls": [{"tool": "here_places", "status": "skipped"}],
            }

        places_response = await self.tool.execute({"routes": routes})
        pois_by_route = places_response.get("pois", {})

        # Score routes: more POIs and closer to route = higher score
        scores = {}
        for route in routes:
            route_id = route.get("id", "")
            pois = pois_by_route.get(route_id, [])
            if not pois:
                scores[route_id] = 0.5
                continue

            # Score based on count and proximity
            count_score = min(1.0, len(pois) / 5.0)  # 5+ POIs = max count score
            avg_distance = sum(p.get("distance_from_route_km", 1) for p in pois) / len(pois)
            proximity_score = max(0.3, 1.0 - (avg_distance / 5.0))  # closer = better

            score = (count_score * 0.6) + (proximity_score * 0.4)
            scores[route_id] = round(score, 2)

        # Find best route for POIs
        best_id = max(scores, key=scores.get) if scores else None
        best_pois = pois_by_route.get(best_id, [])
        total_pois = sum(len(v) for v in pois_by_route.values())

        reasoning = (
            f"POI search along {len(routes)} corridors found {total_pois} points of interest. "
            f"Best coverage: {best_id} with {len(best_pois)} POIs within corridor."
        )

        logger.info(
            f"POI Agent complete: {total_pois} POIs found",
            extra={"agent": self.name},
        )

        return {
            "scores": scores,
            "pois_by_route": pois_by_route,
            "reasoning": reasoning,
            "tool_calls": [{"tool": "here_places", "status": "success"}],
        }
