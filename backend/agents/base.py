"""Base class for all agents."""

from abc import ABC, abstractmethod


class BaseAgent(ABC):
    """Abstract base for navigation agents."""

    @property
    @abstractmethod
    def name(self) -> str:
        """Agent identifier."""
        ...

    @abstractmethod
    async def evaluate(self, context: dict) -> dict:
        """Evaluate routes and return scores + reasoning.

        Args:
            context: Dictionary containing routes and relevant data.

        Returns:
            Dictionary with scores, reasoning, and tool_calls.
        """
        ...
