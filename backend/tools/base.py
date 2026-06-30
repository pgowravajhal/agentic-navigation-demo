"""Base class for all tool adapters."""

from abc import ABC, abstractmethod
from typing import Any


class BaseTool(ABC):
    """Abstract base for all tools. Matches MCP tool interface."""

    @property
    @abstractmethod
    def name(self) -> str:
        """Unique tool identifier."""
        ...

    @property
    def description(self) -> str:
        """Human-readable description."""
        return ""

    @property
    def input_schema(self) -> dict:
        """JSON Schema for expected input."""
        return {}

    @abstractmethod
    async def execute(self, input_data: dict) -> dict:
        """Execute the tool and return structured output."""
        ...


class ToolError(Exception):
    """Raised when a tool encounters an error."""

    def __init__(self, tool_name: str, message: str, retryable: bool = True):
        self.tool_name = tool_name
        self.message = message
        self.retryable = retryable
        super().__init__(f"[{tool_name}] {message}")
