package com.example.mapboxdemo.data

import com.mapbox.geojson.Point

data class PoiLocation(
    val title: String,
    val coordinate: Point
)

val pois = listOf(
    PoiLocation("Gymkhana Muay Thai", Point.fromLngLat(85.3177, 27.7056)),
    PoiLocation("Fitness Park", Point.fromLngLat(85.3221, 27.7110)),
    PoiLocation("Roadhouse Cafe", Point.fromLngLat(85.3129, 27.7145)),
    PoiLocation("Himalayan Java Coffee", Point.fromLngLat(85.3156, 27.7122)),
    PoiLocation("Everest Gym", Point.fromLngLat(85.3198, 27.7079))
)

