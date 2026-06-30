package com.naviapp.agent.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoPolyline
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.MapImageFactory
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapPolyline
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import com.here.sdk.mapview.MapMarker
import com.here.sdk.mapview.MapImage

/**
 * Data class for map display parameters.
 */
data class MapRouteData(
    val originLat: Double,
    val originLng: Double,
    val originLabel: String,
    val destLat: Double,
    val destLng: Double,
    val destLabel: String,
    val polylinePoints: List<Pair<Double, Double>>? = null
)

/**
 * Composable wrapper around HERE SDK MapView.
 * Displays origin/destination markers and optional route polyline.
 * Falls back gracefully if HERE SDK is not initialized.
 */
@Composable
fun HereMapComposable(
    routeData: MapRouteData,
    modifier: Modifier = Modifier
) {
    var mapError by remember { mutableStateOf<String?>(null) }

    // Check if HERE SDK was initialized at app startup
    if (!com.naviapp.agent.NaviApplication.hereSdkInitialized) {
        MapPlaceholder(
            message = com.naviapp.agent.NaviApplication.hereSdkError ?: "HERE SDK not configured",
            origin = routeData.originLabel,
            destination = routeData.destLabel,
            modifier = modifier
        )
        return
    }

    if (mapError != null) {
        // Fallback: show placeholder instead of crashing
        MapPlaceholder(
            message = mapError!!,
            origin = routeData.originLabel,
            destination = routeData.destLabel,
            modifier = modifier
        )
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Lifecycle management for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            try {
                val mv = MapView(ctx)
                mapView = mv
                mv.onCreate(null)
                mv.onResume()

                mv.mapScene.loadScene(MapScheme.NORMAL_DAY) { errorCode ->
                    if (errorCode == null) {
                        // Map loaded successfully — add markers and route
                        addMapContent(mv, routeData)
                    } else {
                        Log.e("HereMap", "Scene load error: $errorCode")
                        mapError = "Map scene failed to load"
                    }
                }
                mv
            } catch (e: InstantiationErrorException) {
                Log.e("HereMap", "HERE SDK init error: ${e.error.name}")
                mapError = "HERE SDK not configured: ${e.error.name}"
                // Return a dummy view — the error state will recompose with placeholder
                android.view.View(ctx)
            } catch (e: Exception) {
                Log.e("HereMap", "Map creation error: ${e.message}")
                mapError = "Map error: ${e.message}"
                android.view.View(ctx)
            }
        },
        update = { view ->
            // Update map content when routeData changes
            if (view is MapView) {
                try {
                    addMapContent(view, routeData)
                } catch (e: Exception) {
                    Log.e("HereMap", "Map update error: ${e.message}")
                }
            }
        }
    )
}

/**
 * Adds markers and polyline to the map.
 */
private fun addMapContent(mapView: MapView, routeData: MapRouteData) {
    val mapScene = mapView.mapScene
    val camera = mapView.camera

    val origin = GeoCoordinates(routeData.originLat, routeData.originLng)
    val destination = GeoCoordinates(routeData.destLat, routeData.destLng)

    // Clear existing content
    mapView.mapScene.removeMapMarkers(mapView.mapScene.let { emptyList() })

    // Add origin marker
    try {
        val originMarker = MapMarker(origin)
        mapView.mapScene.addMapMarker(originMarker)

        val destMarker = MapMarker(destination)
        mapView.mapScene.addMapMarker(destMarker)
    } catch (e: Exception) {
        Log.e("HereMap", "Marker error: ${e.message}")
    }

    // Add route polyline if available
    if (routeData.polylinePoints != null && routeData.polylinePoints.size >= 2) {
        try {
            val geoCoords = routeData.polylinePoints.map { (lat, lng) ->
                GeoCoordinates(lat, lng)
            }
            val geoPolyline = GeoPolyline(geoCoords)
            val mapPolyline = MapPolyline(
                geoPolyline,
                MapPolyline.SolidRepresentation(
                    MapMeasure(MapMeasure.Kind.DISTANCE, 8.0),
                    com.here.sdk.core.Color.valueOf(0.13f, 0.59f, 0.95f, 1.0f), // Blue
                    com.here.sdk.core.Color.valueOf(0.13f, 0.59f, 0.95f, 1.0f)
                )
            )
            mapView.mapScene.addMapPolyline(mapPolyline)
        } catch (e: Exception) {
            Log.e("HereMap", "Polyline error: ${e.message}")
        }
    }

    // Center camera to show both points
    val centerLat = (routeData.originLat + routeData.destLat) / 2.0
    val centerLng = (routeData.originLng + routeData.destLng) / 2.0
    val center = GeoCoordinates(centerLat, centerLng)

    // Estimate zoom based on distance
    val latDiff = Math.abs(routeData.originLat - routeData.destLat)
    val lngDiff = Math.abs(routeData.originLng - routeData.destLng)
    val maxDiff = maxOf(latDiff, lngDiff)
    val distanceInMeters = when {
        maxDiff > 5 -> 1500000.0   // very far
        maxDiff > 2 -> 800000.0    // far
        maxDiff > 1 -> 400000.0    // medium
        maxDiff > 0.5 -> 200000.0  // close
        else -> 100000.0           // very close
    }

    camera.lookAt(center, MapMeasure(MapMeasure.Kind.DISTANCE, distanceInMeters))
}

/**
 * Fallback placeholder when HERE map cannot be displayed.
 */
@Composable
fun MapPlaceholder(
    message: String,
    origin: String,
    destination: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFE0E0E0))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "\uD83D\uDDFA\uFE0F Map Unavailable",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF616161)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$origin → $destination",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}
