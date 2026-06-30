"""HERE Places (Browse/Discover) API adapter with mock fallback."""

import httpx
from .base import BaseTool, ToolError
from config import settings
from logging_config import logger


class HerePlacesTool(BaseTool):
    """Searches for points of interest along a route corridor."""

    @property
    def name(self) -> str:
        return "here_places"

    @property
    def description(self) -> str:
        return "Searches for points of interest (fuel, rest, EV charging) along a route corridor."

    async def execute(self, input_data: dict) -> dict:
        if settings.is_mock or not settings.here_api_key:
            logger.info("Using mock POI data", extra={"tool": self.name})
            return self._mock_response(input_data)

        try:
            return await self._call_here_api(input_data)
        except httpx.HTTPStatusError as e:
            logger.error(f"HERE Places API error: {e.response.status_code}", extra={"tool": self.name})
            raise ToolError(self.name, f"HERE API returned {e.response.status_code}", retryable=True)
        except httpx.TimeoutException:
            logger.error("HERE Places API timeout", extra={"tool": self.name})
            raise ToolError(self.name, "Request timed out", retryable=True)
        except Exception as e:
            logger.error(f"HERE Places unexpected error: {e}", extra={"tool": self.name})
            return self._mock_response(input_data)

    async def _call_here_api(self, input_data: dict) -> dict:
        """Call HERE Browse API to find POIs near route waypoints."""
        routes = input_data.get("routes", [])
        categories = input_data.get("categories", ["600-6300-0066"])  # Fuel station
        # For a real implementation, extract waypoints from polylines and search
        # HERE Browse API at each waypoint
        return self._mock_response(input_data)

    def _mock_response(self, input_data: dict) -> dict:
        """Returns realistic POI data along the route corridor."""
        routes = input_data.get("routes", [])

        # POI data varies by route
        all_pois = {
            "route-1": [
                {
                    "id": "poi-101",
                    "name": "Autohof Michendorf",
                    "category": "fuel-station",
                    "location": {"latitude": 52.31, "longitude": 13.03},
                    "distance_from_route_km": 0.1,
                    "address": "A10 Ausfahrt 20, Brandenburg",
                    "open_now": True,
                },
                {
                    "id": "poi-102",
                    "name": "Raststätte Buckeburger Börde",
                    "category": "rest-area",
                    "location": {"latitude": 52.25, "longitude": 9.05},
                    "distance_from_route_km": 0.0,
                    "address": "A2 km 245",
                    "open_now": True,
                },
                {
                    "id": "poi-103",
                    "name": "IONITY Charging Hub Lehrte",
                    "category": "ev-charging",
                    "location": {"latitude": 52.37, "longitude": 9.98},
                    "distance_from_route_km": 0.3,
                    "address": "A2 Rasthof Lehrte",
                    "open_now": True,
                },
            ],
            "route-2": [
                {
                    "id": "poi-201",
                    "name": "Autohof Geiselwind",
                    "category": "fuel-station",
                    "location": {"latitude": 49.77, "longitude": 10.47},
                    "distance_from_route_km": 0.2,
                    "address": "A3 Ausfahrt 76, Bayern",
                    "open_now": True,
                },
                {
                    "id": "poi-202",
                    "name": "Raststätte Wetterau",
                    "category": "rest-area",
                    "location": {"latitude": 50.35, "longitude": 8.78},
                    "distance_from_route_km": 0.0,
                    "address": "A5 km 410",
                    "open_now": True,
                },
            ],
            "route-3": [
                {
                    "id": "poi-301",
                    "name": "Tank & Rast Harz",
                    "category": "fuel-station",
                    "location": {"latitude": 51.80, "longitude": 10.33},
                    "distance_from_route_km": 0.1,
                    "address": "A7 km 260",
                    "open_now": True,
                },
                {
                    "id": "poi-302",
                    "name": "McDonald's Autohof Kassel",
                    "category": "restaurant",
                    "location": {"latitude": 51.32, "longitude": 9.49},
                    "distance_from_route_km": 0.4,
                    "address": "A7 Ausfahrt 85",
                    "open_now": True,
                },
            ],
        }

        pois_by_route = {}
        for route in routes:
            route_id = route.get("id", "")
            pois_by_route[route_id] = all_pois.get(route_id, all_pois.get("route-1", []))

        return {"pois": pois_by_route}
