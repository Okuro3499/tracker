package com.movosoft.tracker

import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.movosoft.tracker.databinding.ActivityMapsBinding
import org.json.JSONException

public class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        direction()
//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun direction() {
        val queue = Volley.newRequestQueue(this)
        val url = Uri.parse("https://maps.googleapis.com/maps/api/directions/json")
            .buildUpon()
            .appendQueryParameter("destination", "-6.9218571, 107.6048254")
            .appendQueryParameter("origin", "-6.9249233, 107.6345122")
            .appendQueryParameter("mode", "driving")
            .appendQueryParameter("key", "AIzaSyAj6AMcyvJAN7YGP4MDdSztGk_x3DBxfm0")
            .toString()
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val status = response.getString("status")
                    if (status == "OK") {
                        val routes = response.getJSONArray("routes")
                        var points: ArrayList<LatLng?>
                        var polylineOptions: PolylineOptions? = null
                        for (i in 0 until routes.length()) {
                            points = ArrayList()
                            polylineOptions = PolylineOptions()
                            val legs = routes.getJSONObject(i).getJSONArray("legs")
                            for (j in 0 until legs.length()) {
                                val steps = legs.getJSONObject(j).getJSONArray("steps")
                                for (k in 0 until steps.length()) {
                                    val polyline = steps.getJSONObject(k).getJSONObject("polyline")
                                        .getString("points")
                                    val list: List<LatLng> = decodePoly(polyline)
                                    for (l in list.indices) {
                                        val position = LatLng(list[l].latitude, list[l].longitude)
                                        points.add(position)
                                    }
                                }
                            }
                            polylineOptions.addAll(points)
                            polylineOptions.width(10f)
                            polylineOptions.color(
                                ContextCompat.getColor(
                                    this@MapsActivity,
                                    R.color.purple_500
                                )
                            )
                            polylineOptions.geodesic(true)
                        }
                        mMap.addPolyline(polylineOptions!!)
                        mMap.addMarker(
                            MarkerOptions().position(LatLng(-6.9249233, 107.6345122))
                                .title("Point 1")
                        )
                        mMap.addMarker(
                            MarkerOptions().position(LatLng(-6.9218571, 107.6048254))
                                .title("Point 2")
                        )
                        val bounds = LatLngBounds.Builder()
                            .include(LatLng(-6.9249233, 107.6345122))
                            .include(LatLng(-6.9218571, 107.6048254)).build()
                        val point = Point()
                        windowManager.defaultDisplay.getSize(point)
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                bounds,
                                point.x,
                                150,
                                30
                            )
                        )
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { }
        val retryPolicy: RetryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        jsonObjectRequest.retryPolicy = retryPolicy
        queue.add(jsonObjectRequest)
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = java.util.ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b > 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

}