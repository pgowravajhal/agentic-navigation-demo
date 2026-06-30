"""Agentic Navigator Backend — FastAPI application entry point.

Multi-agent navigation recommendation service that orchestrates
routing, traffic, weather, and POI agents to produce intelligent
route recommendations with natural-language explanations.
"""

from datetime import datetime, timezone

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from config import settings
from logging_config import logger
from models import (
    RouteRequest,
    RouteResponse,
    DemoResetRequest,
    DemoResetResponse,
    HealthResponse,
    ErrorResponse,
)
from agents.orchestrator import Orchestrator, get_trace, trace_store


# --- Application setup ---

app = FastAPI(
    title="Agentic Navigator API",
    version="0.1.0",
    description=(
        "Multi-agent navigation recommendation backend. "
        "Orchestrates Route, Traffic, Weather, and POI agents to produce "
        "intelligent route recommendations with transparent explanations. "
        "\n\n"
        "**Modes:**\n"
        "- `mock` (default): Returns realistic pre-built data. No API keys needed.\n"
        "- `live`: Calls real HERE APIs (requires HERE_API_KEY).\n"
    ),
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins.split(","),
    allow_methods=["*"],
    allow_headers=["*"],
)

orchestrator = Orchestrator()


# --- Error handlers ---

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error", "detail": str(exc)},
    )


# --- Endpoints ---

@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["System"],
    summary="Health check",
    description="Returns backend health status, current mode, and service reachability.",
)
async def health():
    """Check backend health and service status."""
    services = {
        "here_routing": "available" if (settings.here_api_key or settings.is_mock) else "no_key",
        "here_traffic": "available" if (settings.here_api_key or settings.is_mock) else "no_key",
        "here_places": "available" if (settings.here_api_key or settings.is_mock) else "no_key",
        "weather": "available" if (settings.weather_api_key or settings.is_mock) else "no_key",
    }
    all_available = all(v == "available" for v in services.values())

    return {
        "status": "healthy" if all_available else "degraded",
        "mode": settings.app_mode,
        "version": "0.1.0",
        "services": services,
    }


@app.post(
    "/recommend-route",
    tags=["Navigation"],
    summary="Get route recommendation",
    description=(
        "Accepts origin and destination, orchestrates all agents "
        "(routing, traffic, weather, POI), and returns the optimal route "
        "recommendation with explanation, scores, and execution trace."
    ),
    responses={
        200: {"description": "Route recommendation with explanation"},
        422: {"description": "Validation error"},
        500: {"model": ErrorResponse, "description": "Orchestration failure"},
    },
)
async def recommend_route(request: RouteRequest):
    """Get an agent-powered route recommendation."""
    origin = request.origin.model_dump()
    destination = request.destination.model_dump()
    preferences = request.preferences.model_dump() if request.preferences else {}

    logger.info(
        f"Route request: {origin.get('label', '?')} → {destination.get('label', '?')}",
    )

    try:
        result = await orchestrator.recommend(origin, destination, preferences)
        return result
    except Exception as e:
        logger.error(f"Orchestration failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail={"error": "ORCHESTRATION_FAILED", "message": str(e)},
        )


@app.get(
    "/agent-trace/{request_id}",
    tags=["Observability"],
    summary="Get agent execution trace",
    description=(
        "Retrieves the detailed execution trace for a specific request, "
        "showing which agents were invoked, their inputs/outputs, scores, "
        "tool calls, and timing information."
    ),
    responses={
        200: {"description": "Agent trace data"},
        404: {"model": ErrorResponse, "description": "Trace not found"},
    },
)
async def agent_trace(request_id: str):
    """Retrieve agent execution trace for a request."""
    trace = get_trace(request_id)
    if not trace:
        raise HTTPException(
            status_code=404,
            detail={"error": "TRACE_NOT_FOUND", "message": f"No trace found for request_id: {request_id}"},
        )
    return trace


@app.post(
    "/demo/reset",
    response_model=DemoResetResponse,
    tags=["Demo"],
    summary="Reset demo state",
    description=(
        "Clears trace history and optionally switches between live and mock mode. "
        "Used for hackathon demonstrations to ensure clean state between runs."
    ),
    responses={
        200: {"description": "Demo reset successful"},
        400: {"model": ErrorResponse, "description": "Invalid mode or scenario"},
    },
)
async def demo_reset(request: DemoResetRequest):
    """Reset demo state: clear traces, switch mode, select scenario."""
    # Validate mode
    if request.mode and request.mode not in ("mock", "live"):
        raise HTTPException(
            status_code=400,
            detail={"error": "INVALID_MODE", "message": f"Mode must be 'mock' or 'live', got: {request.mode}"},
        )

    # Validate scenario
    available_scenarios = ["default", "berlin-to-paris-congestion", "munich-to-hamburg-clear", "stuttgart-to-zurich-rain"]
    if request.scenario and request.scenario not in available_scenarios:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "INVALID_SCENARIO",
                "message": f"Scenario '{request.scenario}' not found. Available: {', '.join(available_scenarios)}",
            },
        )

    # Apply mode switch
    if request.mode:
        settings.app_mode = request.mode
        logger.info(f"Mode switched to: {request.mode}")

    # Apply scenario
    scenario = request.scenario or settings.mock_scenario
    if request.scenario:
        settings.mock_scenario = request.scenario
        logger.info(f"Scenario switched to: {request.scenario}")

    # Clear traces
    cleared = trace_store.clear()
    logger.info(f"Demo reset: cleared {cleared} traces, mode={settings.app_mode}, scenario={scenario}")

    return {
        "status": "reset_complete",
        "mode": settings.app_mode,
        "scenario": scenario,
        "traces_cleared": cleared,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


# --- Startup ---

@app.on_event("startup")
async def startup():
    logger.info(f"Agentic Navigator starting — mode={settings.app_mode}, port={settings.app_port}")


@app.on_event("shutdown")
async def shutdown():
    logger.info("Agentic Navigator shutting down")


# --- Run directly ---

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=False,
        log_level=settings.log_level.lower(),
    )
