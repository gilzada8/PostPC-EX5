package com.example.ex5

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*


const val REQUEST_LOCATION = 1


class MainActivity : AppCompatActivity() {

    private val broadcastReceiver = MyBroadcastReceiver(this)
    private val actionsArray = arrayOf("started", "stopped", "newLocation", "error")
    private var tracking: Boolean = false
    private lateinit var locationTracker: LocationTracker
    private var homeLocation: LocationInfo? = null


    class MyBroadcastReceiver(private val mainAct: MainActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "started" -> {
                    mainAct.startTracking()
                }
                "stopped" -> {
                    mainAct.stopTracking()
                }
                "newLocation" -> {
                    mainAct.updateLocationsUI()
                }
                "error" -> {
                    Toast.makeText(context, intent.action, Toast.LENGTH_SHORT).show()
                }
            }

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intentFilter = IntentFilter()
        for (action: String in actionsArray) {
            intentFilter.addAction(action)
        }
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(broadcastReceiver, intentFilter)
        locationTracker = LocationTracker(applicationContext)
        setButtonsListener()
        if (loadHomeLocation()) {
            updateHomeLocUI()
        }
    }

    private fun updateHomeLocUI() {
        homeLocationUI.visibility = View.VISIBLE
        homeLatitude.text = homeLocation?.latitude.toString()
        homeLongitude.text = homeLocation?.longitude.toString()
        homeLatitude.visibility = View.VISIBLE
        homeLongitude.visibility = View.VISIBLE
        clearHome.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    fun updateLocationsUI() {
        latitude.text = "latitude: " + locationTracker.getCurrentLocation().latitude.toString()
        longitude.text = "longitude: " + locationTracker.getCurrentLocation().longitude.toString()
        accuracy.text = "accuracy: " + locationTracker.getCurrentLocation().accuracy.toString()
        latitude.visibility = View.VISIBLE
        longitude.visibility = View.VISIBLE
        accuracy.visibility = View.VISIBLE
        if (locationTracker.getCurrentLocation().accuracy < 50) {
            setHome.visibility = View.VISIBLE
        } else {
            setHome.visibility = View.GONE
        }
    }

    private fun loadHomeLocation(): Boolean {
        val locationInfoTypeToken = object : TypeToken<LocationInfo>() {}.type
        val sp = this.getSharedPreferences("HomeLocation", Context.MODE_PRIVATE)
            .getString("HomeLocation", null)
        if (sp != null) {
            homeLocation = Gson().fromJson(sp, locationInfoTypeToken)
        }
        return homeLocation != null
    }

    private fun saveHomeLocation() {
        this.getSharedPreferences("HomeLocation", Context.MODE_PRIVATE).edit()
            .putString("HomeLocation", Gson().toJson(homeLocation)).apply()
    }

    private fun setButtonsListener() {
        trackingButton.setOnClickListener {
            if (!tracking) {
                if (permissionsOK()) {
                    locationTracker.startTracking()
                }
            } else {
                locationTracker.stopTracking()
            }
        }

        clearHome.setOnClickListener {
            homeLocation = null
            homeLocationUI.visibility = View.INVISIBLE
            homeLatitude.visibility = View.INVISIBLE
            homeLongitude.visibility = View.INVISIBLE
            clearHome.visibility = View.GONE
            saveHomeLocation()
        }

        setHome.setOnClickListener {
            homeLocation = locationTracker.getCurrentLocation()
            saveHomeLocation()
            updateHomeLocUI()
        }
    }

    private fun startTracking() {
        tracking = true
        trackingButton.text = getString(R.string.stopTrack)
        trackingButton.setBackgroundColor(Color.RED)
    }

    private fun stopTracking() {
        tracking = false
        setHome.visibility = View.GONE
        latitude.visibility = View.INVISIBLE
        longitude.visibility = View.INVISIBLE
        accuracy.visibility = View.INVISIBLE
        trackingButton.text = getString(R.string.startTrack)
        trackingButton.setBackgroundColor(Color.GREEN)
    }

    private fun handleGPSOff() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Please enable location service")
        builder.setPositiveButton("click to open settings") { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        builder.setNegativeButton(android.R.string.no) { _, _ ->
            Toast.makeText(
                applicationContext, "you must enable location", Toast.LENGTH_SHORT
            ).show()
        }
        builder.show()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION
        )
    }

    private fun permissionsOK(): Boolean {
        if (!locationTracker.checkGPSOn()) {
            handleGPSOff()
            return false
        }
        if (!locationTracker.trackingAllowed()) {
            requestLocationPermission()
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationTracker.startTracking()
                } else {
                    Toast.makeText(this, "You must permit tracking", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        saveTracking()
        locationTracker.stopTracking()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
}
