"""Application configuration loaded from environment variables."""

import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    app_mode: str = os.getenv("APP_MODE", "mock")
    app_port: int = int(os.getenv("APP_PORT", "8000"))
    app_host: str = os.getenv("APP_HOST", "0.0.0.0")
    here_api_key: str = os.getenv("HERE_API_KEY", "")
    weather_api_key: str = os.getenv("WEATHER_API_KEY", "")
    here_base_url: str = os.getenv("HERE_BASE_URL", "https://router.hereapi.com")
    weather_api_url: str = os.getenv("WEATHER_API_URL", "https://api.open-meteo.com")
    log_level: str = os.getenv("LOG_LEVEL", "INFO")
    request_timeout_seconds: int = int(os.getenv("REQUEST_TIMEOUT_SECONDS", "10"))
    agent_timeout_seconds: int = int(os.getenv("AGENT_TIMEOUT_SECONDS", "5"))
    mock_scenario: str = os.getenv("MOCK_SCENARIO", "default")
    trace_retention_count: int = int(os.getenv("TRACE_RETENTION_COUNT", "100"))
    cors_origins: str = os.getenv("CORS_ORIGINS", "*")

    @property
    def is_mock(self) -> bool:
        return self.app_mode == "mock"


settings = Settings()
