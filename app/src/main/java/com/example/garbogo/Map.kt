
package com.example.garbogo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import java.io.IOException

class Map : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    private lateinit var userLatLng: LatLng // User's current location
    private lateinit var wasteBinMarkers: ArrayList<Marker> // Markers for waste bins
    private var pathPolyline: Polyline? = null // Polyline to represent path

    private val OPEN_ROUTE_SERVICE_API_KEY = "5b3ce3597851110001cf6248a548eb173eb74503aedd1f827728bd9f"

    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Initialize the map fragment
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        // Set callback for when the map is ready
        mapFragment.getMapAsync(this)

        // Initialize views
        val btnFindNearestBin: Button = findViewById(R.id.btnFindNearestBin)

        // Set click listener for the button
        btnFindNearestBin.setOnClickListener {
            findNearestBinAndNavigate()
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }

        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLatLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    map.addMarker(MarkerOptions().position(userLatLng).title("Your Location"))
                }
            }

        val wasteBinLocations = listOf(
            LatLng(9.8519947, 76.9394600),
            LatLng(9.8520879, 76.9447557),
            LatLng(9.8523852, 76.9488417),
            LatLng(9.8587962, 76.9471334)

        )

        wasteBinMarkers = ArrayList()

        val wasteBinIcon = BitmapDescriptorFactory.fromResource(R.drawable.waste)

        for (location in wasteBinLocations) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Waste Bin")
                    .icon(wasteBinIcon)
            )
            marker?.let {
                wasteBinMarkers.add(it)
            }
        }

        map.setOnMarkerClickListener { clickedMarker ->
            clickedMarker?.let {
                val clickedLocation = clickedMarker.position
                fetchAndDrawRoute(userLatLng, clickedLocation)
                true
            } ?: false
        }
    }

    private fun fetchAndDrawRoute(start: LatLng, end: LatLng) {
        val url =
            "https://api.openrouteservice.org/v2/directions/driving-car?api_key=$OPEN_ROUTE_SERVICE_API_KEY&start=${start.longitude},${start.latitude}&end=${end.longitude},${end.latitude}"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MapActivity", "Failed to fetch and draw route: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val json = responseBody.string()
                    Log.d("MapActivity", "JSON Response: $json")

                    val responseCode = response.code
                    val responseMessage = response.message // Assigning response message directly to a variable

                    if (!response.isSuccessful) {
                        Log.e("MapActivity", "Error response: $responseCode - $responseMessage")
                        // Handle error response here, such as showing an error message to the user
                        return
                    }

                    try {
                        val route = gson.fromJson(json, Route::class.java)

                        val polylineOptions = PolylineOptions()
                        route.features[0].geometry.coordinates.forEach { coord ->
                            val latLng = LatLng(coord[1], coord[0])
                            polylineOptions.add(latLng)
                        }

                        polylineOptions.color(Color.BLUE)
                            .width(10f)

                        runOnUiThread {
                            pathPolyline?.remove()
                            pathPolyline = map.addPolyline(polylineOptions)
                        }
                    } catch (e: JsonSyntaxException) {
                        Log.e("MapActivity", "Error parsing JSON: ${e.message}")
                    }
                }
            }






        })
    }

    private fun findNearestBinAndNavigate() {
        // Check if user location is available
        if (::userLatLng.isInitialized) {
            // Find the nearest waste bin
            var nearestBin: LatLng? = null
            var minDistance = Double.MAX_VALUE

            for (marker in wasteBinMarkers) {
                val binLocation = marker.position
                val distance = calculateDistance(userLatLng, binLocation)
                if (distance < minDistance) {
                    minDistance = distance
                    nearestBin = binLocation
                }
            }

            // If a nearest bin is found, navigate to it
            nearestBin?.let {
                fetchAndDrawRoute(userLatLng, it)
            }
        } else {
            // Handle the case when user location is not available
            Toast.makeText(this, "User location not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to calculate distance between two LatLng points
    private fun calculateDistance(latLng1: LatLng, latLng2: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            latLng1.latitude, latLng1.longitude,
            latLng2.latitude, latLng2.longitude,
            results
        )
        return results[0].toDouble()
    }
}


private data class Route(
    val features: List<Feature>
)

private data class Feature(
    val geometry: Geometry,
    val properties: Properties
)

private data class Geometry(
    val coordinates: List<List<Double>>
)

private data class Properties(
    val summary: Summary
)

private data class Summary(
    val distance: Double,
    val duration: Double
)