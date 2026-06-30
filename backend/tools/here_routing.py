"""HERE Routing API v8 adapter with mock fallback."""

import httpx
from .base import BaseTool, ToolError
from config import settings
from logging_config import logger


class HereRoutingTool(BaseTool):
    """Calculates candidate routes between origin and destination using HERE Routing API v8."""

    @property
    def name(self) -> str:
        return "here_routing"

    @property
    def description(self) -> str:
        return "Calculates candidate routes between origin and destination, returning geometry, distance, and duration."

    async def execute(self, input_data: dict) -> dict:
        if settings.is_mock or not settings.here_api_key:
            logger.info("Using mock routing data", extra={"tool": self.name})
            return self._mock_response(input_data)

        try:
            return await self._call_here_api(input_data)
        except httpx.HTTPStatusError as e:
            logger.error(f"HERE Routing API error: {e.response.status_code}", extra={"tool": self.name})
            raise ToolError(self.name, f"HERE API returned {e.response.status_code}", retryable=True)
        except httpx.TimeoutException:
            logger.error("HERE Routing API timeout", extra={"tool": self.name})
            raise ToolError(self.name, "Request timed out", retryable=True)
        except Exception as e:
            logger.error(f"HERE Routing unexpected error: {e}", extra={"tool": self.name})
            # Fallback to mock on unexpected errors
            return self._mock_response(input_data)

    async def _call_here_api(self, input_data: dict) -> dict:
        origin = input_data["origin"]
        destination = input_data["destination"]
        url = f"{settings.here_base_url}/v8/routes"
        params = {
            "apiKey": settings.here_api_key,
            "origin": f"{origin['latitude']},{origin['longitude']}",
            "destination": f"{destination['latitude']},{destination['longitude']}",
            "transportMode": "car",
            "alternatives": 2,
            "return": "summary,polyline",
        }

        async with httpx.AsyncClient(timeout=settings.agent_timeout_seconds) as client:
            resp = await client.get(url, params=params)
            resp.raise_for_status()
            data = resp.json()

        # Transform HERE response to our standard format
        routes = []
        for i, route in enumerate(data.get("routes", [])):
            section = route.get("sections", [{}])[0]
            summary = section.get("summary", {})
            routes.append({
                "id": f"route-{i+1}",
                "label": f"Route {i+1}",
                "distance_meters": summary.get("length", 0),
                "duration_seconds": summary.get("duration", 0),
                "polyline": section.get("polyline", ""),
            })

        return {"routes": routes}

    def _mock_response(self, input_data: dict) -> dict:
        """Returns realistic mock route data for demo purposes."""
        origin = input_data.get("origin", {})
        destination = input_data.get("destination", {})
        origin_label = origin.get("label", "Origin")
        dest_label = destination.get("label", "Destination")

        return {
            "routes": [
                {
                    "id": "route-1",
                    "label": f"Via A2 / A4 ({origin_label} → {dest_label})",
                    "distance_meters": 1050000,
                    "duration_seconds": 36000,
                    "polyline": "mock-polyline-1",
                },
                {
                    "id": "route-2",
                    "label": f"Via A3 / A6 ({origin_label} → {dest_label})",
                    "distance_meters": 980000,
                    "duration_seconds": 39000,
                    "polyline": "mock-polyline-2",
                },
                {
                    "id": "route-3",
                    "label": f"Via A7 / A5 ({origin_label} → {dest_label})",
                    "distance_meters": 1120000,
                    "duration_seconds": 41000,
                    "polyline": "mock-polyline-3",
                },
            ]
        }
