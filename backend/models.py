"""Pydantic models for request/response validation and API documentation."""

from pydantic import BaseModel, Field
from typing import Optional


# --- Request Models ---

class Location(BaseModel):
    """Geographic coordinates with optional label."""
    latitude: float = Field(..., ge=-90, le=90, description="Latitude in decimal degrees")
    longitude: float = Field(..., ge=-180, le=180, description="Longitude in decimal degrees")
    label: Optional[str] = Field(None, description="Human-readable location name")


class RoutePreferences(BaseModel):
    """User preferences for route calculation."""
    transport_mode: str = Field("car", description="Transport mode (car)")
    avoid_highways: bool = Field(False, description="Whether to avoid highways")
    prioritize: str = Field("fastest", description="Optimization target: fastest or shortest")


class RouteRequest(BaseModel):
    """Request body for route recommendation."""
    origin: Location
    destination: Location
    preferences: Optional[RoutePreferences] = None
    demo_mode: bool = Field(True, description="Enable demo mode with mock fallbacks")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "origin": {"latitude": 52.52, "longitude": 13.405, "label": "Berlin"},
                    "destination": {"latitude": 48.8566, "longitude": 2.3522, "label": "Paris"},
                    "preferences": {"transport_mode": "car", "prioritize": "fastest"},
                    "demo_mode": True,
                }
            ]
        }
    }


class DemoResetRequest(BaseModel):
    """Request body for demo reset."""
    mode: Optional[str] = Field(None, description="Switch to 'mock' or 'live' mode")
    scenario: Optional[str] = Field(None, description="Named scenario to load")


class PoiSearchRequest(BaseModel):
    """Request body for POI search."""
    route_polyline: str = Field(..., description="Encoded polyline of the route")
    corridor_width_km: float = Field(5.0, ge=0.5, le=50, description="Search corridor width in km")
    categories: list[str] = Field(
        default=["fuel-station", "rest-area"],
        description="POI categories to search for"
    )
    limit: int = Field(10, ge=1, le=50, description="Maximum results to return")


# --- Response Models ---

class RouteSummary(BaseModel):
    """Summary metrics for a route."""
    distance_km: float
    duration_minutes: int
    eta: str


class RouteScores(BaseModel):
    """Agent scores for a route."""
    overall: float
    routing: float
    traffic: float
    weather: float
    poi: float = 0.0


class RouteInfo(BaseModel):
    """Complete information for a single route."""
    id: str
    label: str
    summary: RouteSummary
    scores: RouteScores
    is_recommended: bool = False


class ExplanationFactor(BaseModel):
    """A single factor that influenced the recommendation."""
    factor: str
    influence: str = Field(..., description="high, medium, or low")
    detail: str


class Explanation(BaseModel):
    """Natural-language explanation of the route recommendation."""
    summary: str
    factors: list[ExplanationFactor]
    trade_offs: str
    confidence: float = Field(..., ge=0, le=1)


class WeatherSummary(BaseModel):
    """Weather conditions summary for the route."""
    conditions: str
    alerts: list[str]


class TrafficSummary(BaseModel):
    """Traffic conditions summary for the route."""
    level: str
    summary: str


class ToolCallTrace(BaseModel):
    """Record of a tool invocation within an agent."""
    tool: str
    duration_ms: int
    status: str


class AgentTraceEntry(BaseModel):
    """Execution trace for a single agent."""
    name: str
    status: str
    duration_ms: int
    input_summary: str
    output_summary: str
    score: Optional[float] = None
    tool_calls: list[ToolCallTrace] = []


class FinalDecision(BaseModel):
    """The orchestrator's final routing decision."""
    recommended: str
    confidence: float
    method: str


class AgentTrace(BaseModel):
    """Complete execution trace for a recommendation request."""
    request_id: str
    timestamp: str
    total_duration_ms: int
    agents: list[AgentTraceEntry]
    final_decision: FinalDecision


class PoiInfo(BaseModel):
    """Point of interest information."""
    id: str
    name: str
    category: str
    location: Location
    distance_from_route_km: float
    address: Optional[str] = None
    open_now: Optional[bool] = None


class RequestMetadata(BaseModel):
    """Metadata about request processing."""
    processing_time_ms: int
    agents_consulted: list[str]
    mode: str


class RouteResponse(BaseModel):
    """Complete response for a route recommendation request."""
    request_id: str
    recommended_route: RouteInfo
    alternatives: list[RouteInfo]
    explanation: Explanation
    traffic_summary: TrafficSummary
    weather_summary: WeatherSummary
    pois: list[PoiInfo] = []
    agent_trace: AgentTrace
    metadata: RequestMetadata


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    mode: str
    version: str
    services: dict = {}


class DemoResetResponse(BaseModel):
    """Demo reset response."""
    status: str
    mode: str
    scenario: str
    traces_cleared: int
    timestamp: str


class ErrorResponse(BaseModel):
    """Standard error response."""
    error: str
    detail: Optional[str] = None
    request_id: Optional[str] = None
