@file:OptIn(ExperimentalPermissionsApi::class)

package com.example.mapboxdemo

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mapboxdemo.component.createPoiMarkerBitmap
import com.example.mapboxdemo.data.pois
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapboxUserLocationScreen()
        }
    }
}

@Composable
fun MapboxUserLocationScreen() {
    val context= LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val mapViewportState = rememberMapViewportState()
    val mapboxMapRef = remember { mutableStateOf<MapboxMap?>(null) }
    var userLocation by remember { mutableStateOf<Point?>(null) }

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    if (permissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            ) {
                MapEffect(Unit) { mapView ->
                    mapboxMapRef.value = mapView.getMapboxMap()

                    mapView.location.updateSettings {
                        enabled = true
                        puckBearing = PuckBearing.COURSE
                        puckBearingEnabled = true
                        pulsingEnabled = true

                        locationPuck = LocationPuck2D(
                            topImage = ImageHolder.from(R.drawable.mapbox_user_stroke_icon),
                            scaleExpression = interpolate {
                                linear()
                                zoom()
                                stop {
                                    literal(0.0)
                                    literal(0.6)
                                }
                                stop {
                                    literal(20.0)
                                    literal(1.0)
                                }
                            }.toJson()
                        )
                    }

                    mapView.location.addOnIndicatorPositionChangedListener { point ->
                        userLocation = point
                    }

                    pois.firstOrNull()?.let { poi ->
                        mapboxMapRef.value?.setCamera(
                            CameraOptions.Builder()
                                .center(poi.coordinate)
                                .zoom(15.0)
                                .build()
                        )
                    }

                    val annotationApi = mapView.annotations
                    val pointAnnotationManager = annotationApi.createPointAnnotationManager()
                    val polylineManager = annotationApi.createPolylineAnnotationManager()
                    val poiMap = mutableMapOf<String, String>()

                    pois.forEach { poi ->
                        val bitmap = createPoiMarkerBitmap(context, poi.title)

                        val annotationOptions = PointAnnotationOptions()
                            .withPoint(poi.coordinate)
                            .withIconImage(bitmap)
                            .withIconSize(1.0)

                        val annotation = pointAnnotationManager.create(annotationOptions)
                        poiMap[annotation.id] = poi.title                    }

                    pointAnnotationManager.addClickListener { annotation ->
                        val destination = annotation.point
                        val zoom = mapboxMapRef.value?.cameraState?.zoom ?: 15.0
                        val adjustedWidth = when {
                            zoom < 12 -> 2.0
                            zoom < 15 -> 4.0
                            else -> 6.0
                        }

                        userLocation?.let { origin ->
                            fetchRoutePolyline(
                                origin = origin,
                                destination = destination,
                                accessToken = context.getString(R.string.mapbox_access_token),
                                onResult = { routePoints ->
                                    polylineManager.deleteAll()
                                    val polyline = PolylineAnnotationOptions()
                                        .withPoints(routePoints)
                                        .withLineColor("#2962FF")
                                        .withLineWidth(adjustedWidth)
                                    val lastRoutePoint = routePoints.last()

                                    val connectorLine = PolylineAnnotationOptions()
                                        .withPoints(listOf(lastRoutePoint, destination))
                                        .withLineColor("#2962FF")
                                        .withLineOpacity(0.3)
                                        .withLineWidth(adjustedWidth)

                                    polylineManager.create(polyline)
                                    polylineManager.create(connectorLine)

                                    val cameraOptions = mapboxMapRef.value?.cameraForCoordinates(
                                        coordinates = routePoints,
                                        coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0),
                                        bearing = null,
                                        pitch = null
                                    )
                                    cameraOptions?.let {
                                        mapboxMapRef.value?.setCamera(it)
                                    }

                                },
                                onError = { msg ->
                                    Log.d("MainActivity", msg)
                                }
                            )
                        }

                        true
                    }

                }
            }

            // Zoom In Button
            FloatingActionButton(
                onClick = {
                    mapboxMapRef.value?.cameraState?.zoom?.let { currentZoom ->
                        mapboxMapRef.value?.setCamera(CameraOptions.Builder().zoom(currentZoom + 1).build())
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            // Zoom Out Button
            FloatingActionButton(
                onClick = {
                    mapboxMapRef.value?.cameraState?.zoom?.let { currentZoom ->
                        mapboxMapRef.value?.setCamera(CameraOptions.Builder().zoom(currentZoom - 1).build())
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 40.dp, end = 16.dp)
            ) {
                Icon(painterResource(R.drawable.ic_minus), contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = {
                    mapViewportState.transitionToFollowPuckState()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 40.dp, start = 16.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "My Location")
            }
        }
    } else {
        Text("Waiting for location permission...", modifier = Modifier.padding(16.dp))
    }
}

fun fetchRoutePolyline(
    origin: Point,
    destination: Point,
    accessToken: String,
    onResult: (List<Point>) -> Unit,
    onError: (String) -> Unit
) {
    val client = OkHttpClient()
    val url = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
            "${origin.longitude()},${origin.latitude()};${destination.longitude()},${destination.latitude()}" +
            "?geometries=polyline6&access_token=$accessToken"

    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onError("Network error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                onError("Failed: ${response.message}")
                return
            }
            val body = response.body?.string() ?: return onError("Empty response")
            val geometry = JSONObject(body)
                .getJSONArray("routes")
                .getJSONObject(0)
                .getString("geometry")

            val points = LineString.fromPolyline(geometry, 6).coordinates()
            onResult(points)
        }
    })
}

