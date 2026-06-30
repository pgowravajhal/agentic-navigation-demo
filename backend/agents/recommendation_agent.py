"""Recommendation Agent — aggregates scores and generates explanation."""

from .base import BaseAgent
from logging_config import logger


class RecommendationAgent(BaseAgent):
    """Aggregates all agent scores and generates the final recommendation with natural-language explanation."""

    # Weighting for each factor in the final score
    WEIGHTS = {
        "routing": 0.25,
        "traffic": 0.35,
        "weather": 0.25,
        "poi": 0.15,
    }

    @property
    def name(self) -> str:
        return "recommendation"

    async def evaluate(self, context: dict) -> dict:
        logger.info("Recommendation Agent starting", extra={"agent": self.name})

        routes = context.get("routes", [])
        route_scores = context.get("route_scores", {})
        traffic_scores = context.get("traffic_scores", {})
        weather_scores = context.get("weather_scores", {})
        poi_scores = context.get("poi_scores", {})
        traffic_data = context.get("traffic_data", [])
        weather_data = context.get("weather_data", [])

        if not routes:
            return {
                "recommended_id": None,
                "scores": {},
                "explanation": {
                    "summary": "No routes available for recommendation.",
                    "factors": [],
                    "trade_offs": "",
                    "confidence": 0.0,
                },
            }

        # Weighted aggregation
        final_scores = {}
        for route in routes:
            rid = route["id"]
            r = route_scores.get(rid, 0.5)
            t = traffic_scores.get(rid, 0.5)
            w = weather_scores.get(rid, 0.5)
            p = poi_scores.get(rid, 0.5)

            overall = (
                r * self.WEIGHTS["routing"]
                + t * self.WEIGHTS["traffic"]
                + w * self.WEIGHTS["weather"]
                + p * self.WEIGHTS["poi"]
            )
            final_scores[rid] = {
                "overall": round(overall, 2),
                "routing": r,
                "traffic": t,
                "weather": w,
                "poi": p,
            }

        # Pick best route
        best_id = max(final_scores, key=lambda k: final_scores[k]["overall"])
        best_route = next((r for r in routes if r["id"] == best_id), routes[0])

        # Generate explanation
        explanation = self._generate_explanation(
            best_route, routes, final_scores, traffic_data, weather_data
        )

        logger.info(
            f"Recommendation: {best_id} (confidence: {explanation['confidence']:.2f})",
            extra={"agent": self.name},
        )

        return {
            "recommended_id": best_id,
            "scores": final_scores,
            "explanation": explanation,
        }

    def _generate_explanation(
        self, best: dict, routes: list, scores: dict, traffic_data: list, weather_data: list
    ) -> dict:
        best_id = best["id"]
        best_score = scores[best_id]

        # Find alternatives sorted by score (descending)
        others = sorted(
            [r for r in routes if r["id"] != best_id],
            key=lambda r: scores.get(r["id"], {}).get("overall", 0),
            reverse=True,
        )
        runner_up = others[0] if others else None

        factors = []

        # Determine dominant factor
        factor_contributions = {
            "routing": best_score["routing"] * self.WEIGHTS["routing"],
            "traffic": best_score["traffic"] * self.WEIGHTS["traffic"],
            "weather": best_score["weather"] * self.WEIGHTS["weather"],
            "poi": best_score["poi"] * self.WEIGHTS["poi"],
        }
        dominant_factor = max(factor_contributions, key=factor_contributions.get)

        # Traffic factor explanation
        traffic_for_best = next(
            (t for t in traffic_data if t["route_id"] == best_id), {}
        )
        traffic_for_others = [t for t in traffic_data if t["route_id"] != best_id]
        worst_traffic = max(traffic_for_others, key=lambda x: x.get("delay_minutes", 0)) if traffic_for_others else {}

        if worst_traffic.get("delay_minutes", 0) > 20:
            factors.append({
                "factor": "traffic",
                "influence": "high",
                "detail": f"Avoids {worst_traffic.get('summary', 'heavy congestion')}.",
            })
        elif traffic_for_best.get("delay_minutes", 0) < 10:
            factors.append({
                "factor": "traffic",
                "influence": "medium",
                "detail": f"Recommended route has {traffic_for_best.get('summary', 'clear conditions')}.",
            })
        else:
            factors.append({
                "factor": "traffic",
                "influence": "low",
                "detail": "Similar traffic conditions across all routes.",
            })

        # Duration factor explanation
        if runner_up:
            time_diff = (runner_up["duration_seconds"] - best["duration_seconds"]) // 60
            if time_diff > 10:
                factors.append({
                    "factor": "duration",
                    "influence": "high",
                    "detail": f"Saves {time_diff} minutes compared to the next best alternative.",
                })
            elif time_diff > 0:
                factors.append({
                    "factor": "duration",
                    "influence": "medium",
                    "detail": f"Arrives {time_diff} minutes earlier than the alternative.",
                })
            else:
                factors.append({
                    "factor": "duration",
                    "influence": "low",
                    "detail": "Similar arrival times across routes.",
                })

        # Weather factor explanation
        weather_for_best = next(
            (w for w in weather_data if w["route_id"] == best_id), {}
        )
        best_alerts = weather_for_best.get("alerts", [])
        all_alerts = []
        for w in weather_data:
            all_alerts.extend(w.get("alerts", []))

        if best_alerts:
            factors.append({
                "factor": "weather",
                "influence": "medium",
                "detail": f"Note: {best_alerts[0]}",
            })
        elif all_alerts:
            factors.append({
                "factor": "weather",
                "influence": "high",
                "detail": f"Avoids weather hazards affecting alternative routes.",
            })
        else:
            factors.append({
                "factor": "weather",
                "influence": "low",
                "detail": f"{weather_for_best.get('conditions', 'Fair conditions')} — no significant weather impact on any route.",
            })

        # Build natural-language summary
        summary_parts = [f"{best['label']} is recommended"]
        high_factors = [f for f in factors if f["influence"] == "high"]
        if high_factors:
            reasons = []
            for f in high_factors:
                if f["factor"] == "traffic":
                    reasons.append("it avoids heavy traffic")
                elif f["factor"] == "duration":
                    time_diff = (runner_up["duration_seconds"] - best["duration_seconds"]) // 60 if runner_up else 0
                    reasons.append(f"it saves approximately {time_diff} minutes")
                elif f["factor"] == "weather":
                    reasons.append("it avoids severe weather")
            if reasons:
                summary_parts.append("because " + " and ".join(reasons))

        summary = " ".join(summary_parts) + "."

        # Trade-offs
        trade_off = ""
        if runner_up:
            dist_diff_km = (best["distance_meters"] - runner_up["distance_meters"]) / 1000
            runner_score = scores.get(runner_up["id"], {}).get("overall", 0)
            if dist_diff_km > 10:
                trade_off = (
                    f"The recommended route is {dist_diff_km:.0f} km longer than {runner_up['label']}, "
                    f"but significantly faster due to current traffic conditions."
                )
            elif dist_diff_km < -10:
                trade_off = (
                    f"The recommended route is also {abs(dist_diff_km):.0f} km shorter, "
                    f"making it the best choice by all metrics."
                )
            else:
                trade_off = (
                    f"Routes are similar in distance. The recommendation is based on "
                    f"better traffic and weather conditions along the corridor."
                )

        confidence = best_score["overall"]

        return {
            "summary": summary,
            "factors": factors,
            "trade_offs": trade_off,
            "confidence": confidence,
        }
