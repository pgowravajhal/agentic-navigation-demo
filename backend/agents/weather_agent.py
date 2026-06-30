"""Weather Agent — evaluates routes based on weather conditions along the corridor."""

from .base import BaseAgent
from tools.weather import WeatherTool
from logging_config import logger


class WeatherAgent(BaseAgent):
    """Evaluates routes by weather safety along the corridor."""

    def __init__(self):
        self.tool = WeatherTool()

    @property
    def name(self) -> str:
        return "weather"

    async def evaluate(self, context: dict) -> dict:
        logger.info("Weather Agent starting evaluation", extra={"agent": self.name})

        routes = context.get("routes", [])
        if not routes:
            return {
                "scores": {},
                "weather_data": [],
                "conditions": "Unknown",
                "alerts": [],
                "reasoning": "No routes to evaluate.",
                "tool_calls": [{"tool": "weather_forecast", "status": "skipped"}],
            }

        weather_response = await self.tool.execute({"routes": routes})
        weather_data = weather_response.get("weather", [])

        # Score routes by safety score from weather data
        scores = {}
        for item in weather_data:
            route_id = item["route_id"]
            scores[route_id] = round(item.get("safety_score", 0.7), 2)

        # Collect alerts across all routes
        all_alerts = []
        for item in weather_data:
            all_alerts.extend(item.get("alerts", []))

        # General conditions (from first route as representative)
        conditions_summary = weather_data[0]["conditions"] if weather_data else "Unknown"

        # Reasoning
        safest_id = max(scores, key=scores.get) if scores else None
        safest_weather = next((w for w in weather_data if w["route_id"] == safest_id), {})

        reasoning = (
            f"Weather analysis of {len(routes)} route corridors complete. "
            f"Conditions: {conditions_summary}. "
            f"Alerts: {len(all_alerts)}. "
            f"Safest corridor: {safest_id} (score {scores.get(safest_id, 0):.2f})."
        )

        logger.info(
            f"Weather Agent complete: {len(all_alerts)} alerts",
            extra={"agent": self.name},
        )

        return {
            "scores": scores,
            "weather_data": weather_data,
            "conditions": conditions_summary,
            "alerts": all_alerts,
            "reasoning": reasoning,
            "tool_calls": [{"tool": "weather_forecast", "status": "success"}],
        }
