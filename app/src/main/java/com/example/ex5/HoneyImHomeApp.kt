package com.example.ex5

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

const val PERIODIC_WORK: Long = 15

class HoneyImHomeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            LocalSendSmsBroadcastReceiver(),
            IntentFilter(ACTION_SMS)
        )

        val periodicWork: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<HoneyImHomeWorker>(PERIODIC_WORK, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(periodicWork)
    }

}

class HoneyImHomeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val sp: SharedPreferences =
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun doWork(): Result {
        val phoneNumber: String? = sp.getString(PHONE, null)
        val homeLocation: LocationInfo? = loadLocation(HOME, sp)
        if (!permissionsExists() || phoneNumber == null || homeLocation == null) {
            return Result.success()
        }
        trackLocation()
        return Result.success()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val currentLocation = LocationInfo(
                locationResult.lastLocation.latitude,
                locationResult.lastLocation.longitude,
                locationResult.lastLocation.accuracy
            )
            fusedLocationClient.removeLocationUpdates(this)
            val previousLocation: LocationInfo? = loadLocation(PREV_LOCATION, sp)
            if (currentLocation.accuracy >= 50 || previousLocation == null || currentLocation.toLocation()
                    .distanceTo(previousLocation.toLocation()) < 50
            ) {
                if (currentLocation.accuracy < 50) {
                    sp.edit().putString(PREF_NAME, Gson().toJson(currentLocation)).apply()
                }
                return
            }
            val homeLocation: LocationInfo? = loadLocation(HOME, sp)
            if (currentLocation.toLocation().distanceTo(homeLocation?.toLocation()) < 50) {
                val intent = Intent(ACTION_SMS)
                intent.putExtra(PHONE, sp.getString(PHONE, null))
                intent.putExtra(CONTENT, "Honey I'm Home!")
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
            sp.edit().putString(PREF_NAME, Gson().toJson(currentLocation)).apply()
            return
        }
    }

    private fun trackLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun permissionsExists(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadLocation(location: String, sp: SharedPreferences): LocationInfo? {
        val homeLocation: LocationInfo?
        val locationInfoTypeToken = object : TypeToken<LocationInfo>() {}.type
        val homeLoc = sp.getString(location, null)
        homeLocation = if (sp.getString(location, null) != null) {
            Gson().fromJson(homeLoc, locationInfoTypeToken)
        } else {
            null
        }
        return homeLocation
    }
}
