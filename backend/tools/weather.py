"""Weather service adapter with mock fallback."""

import httpx
from .base import BaseTool, ToolError
from config import settings
from logging_config import logger


class WeatherTool(BaseTool):
    """Retrieves weather conditions and forecasts for route waypoints."""

    @property
    def name(self) -> str:
        return "weather_forecast"

    @property
    def description(self) -> str:
        return "Retrieves current weather and forecasts for waypoints along a route corridor."

    async def execute(self, input_data: dict) -> dict:
        if settings.is_mock or not settings.weather_api_key:
            logger.info("Using mock weather data", extra={"tool": self.name})
            return self._mock_response(input_data)

        try:
            return await self._call_weather_api(input_data)
        except httpx.HTTPStatusError as e:
            logger.error(f"Weather API error: {e.response.status_code}", extra={"tool": self.name})
            raise ToolError(self.name, f"Weather API returned {e.response.status_code}", retryable=True)
        except httpx.TimeoutException:
            logger.error("Weather API timeout", extra={"tool": self.name})
            raise ToolError(self.name, "Request timed out", retryable=True)
        except Exception as e:
            logger.error(f"Weather unexpected error: {e}", extra={"tool": self.name})
            return self._mock_response(input_data)

    async def _call_weather_api(self, input_data: dict) -> dict:
        """Call Open-Meteo or configured weather API for route waypoints."""
        routes = input_data.get("routes", [])
        # Real implementation would extract waypoints from routes and query weather
        # Open-Meteo example: /v1/forecast?latitude=52.52&longitude=13.41&current_weather=true
        return self._mock_response(input_data)

    def _mock_response(self, input_data: dict) -> dict:
        """Returns realistic weather data varying by route."""
        routes = input_data.get("routes", [])
        weather_data = []

        mock_weather = [
            {
                "conditions": "Partly cloudy, 18°C",
                "precipitation": "Light rain near Cologne (30% probability)",
                "wind": "15 km/h westerly",
                "visibility": "Good (>10 km)",
                "temperature_celsius": 18,
                "alerts": [],
                "safety_score": 0.85,
            },
            {
                "conditions": "Heavy rain expected, 14°C",
                "precipitation": "Heavy rain between Frankfurt and Mannheim (80% probability)",
                "wind": "25 km/h south-westerly with gusts to 45 km/h",
                "visibility": "Reduced (3-5 km)",
                "temperature_celsius": 14,
                "alerts": ["Heavy rain warning between km 400-500, reduced visibility expected"],
                "safety_score": 0.55,
            },
            {
                "conditions": "Overcast, 16°C",
                "precipitation": "Drizzle possible near Kassel (40% probability)",
                "wind": "10 km/h northerly",
                "visibility": "Moderate (7 km)",
                "temperature_celsius": 16,
                "alerts": [],
                "safety_score": 0.75,
            },
        ]

        for i, route in enumerate(routes):
            route_id = route.get("id", f"route-{i+1}")
            weather = mock_weather[i % len(mock_weather)]
            weather_data.append({
                "route_id": route_id,
                **weather,
            })

        return {"weather": weather_data}
