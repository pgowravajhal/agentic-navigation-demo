package com.naviapp.agent.ui

import com.naviapp.agent.data.RouteResponseDto

/**
 * Simple in-memory cache for passing route responses between screens.
 * Keyed by request_id.
 */
object ResultCache {
    private val cache = mutableMapOf<String, RouteResponseDto>()

    fun put(requestId: String, response: RouteResponseDto) {
        cache[requestId] = response
        // Keep cache bounded
        if (cache.size > 20) {
            cache.keys.first().let { cache.remove(it) }
        }
    }

    fun get(requestId: String): RouteResponseDto? = cache[requestId]

    fun clear() = cache.clear()
}
