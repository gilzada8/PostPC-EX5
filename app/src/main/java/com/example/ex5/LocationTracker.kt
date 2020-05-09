package com.example.ex5

import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*


//hold latitude, longitude and accuracy.
class LocationInfo(
    var latitude: Double,
    var longitude: Double,
    var accuracy: Float
)

class LocationTracker(private val context: Context) {


    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = LocationRequest.create().apply {
        interval = 0
        fastestInterval = 0
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private var currentLocation = LocationInfo(0.0, 0.0, 0f)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            currentLocation.latitude = locationResult.lastLocation.latitude
            currentLocation.longitude = locationResult.lastLocation.longitude
            currentLocation.accuracy = locationResult.lastLocation.accuracy
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("newLocation"))
        }
    }

    fun getCurrentLocation(): LocationInfo {
        return currentLocation
    }

    fun startTracking() {
        if (checkGPSOn() && trackingAllowed()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("started"))
        } else {
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("error"))
        }
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("stopped"))
    }

    fun checkGPSOn(): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun trackingAllowed(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
