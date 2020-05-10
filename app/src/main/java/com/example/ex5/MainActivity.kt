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
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*


const val NOTIFICATION_ID = 1
const val REQUEST_LOCATION = 1
const val REQUEST_SMS = 2
const val PHONE = "phone"
const val CONTENT = "content"
const val HOME = "HomeLocation"
const val TAG = "HoneyImHome"
const val PREF_NAME = "HoneyImHome"
const val CHANNEL_ID = "HoneyImHomeApp"
const val PREV_LOCATION = "current"
const val ACTION_SMS = "POST_PC.ACTION_SEND_SMS"


class MainActivity : AppCompatActivity() {
    private val broadcastReceiver = MyBroadcastReceiver(this)
    private val actionsArray = arrayOf("started", "stopped", "newLocation", "error")
    private var tracking: Boolean = false
    private lateinit var locationTracker: LocationTracker
    private var homeLocation: LocationInfo? = null
    private var phoneNumber: String? = null

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
        loadPhoneNumber()
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
        val sp = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(HOME, null)
        if (sp != null) {
            homeLocation = Gson().fromJson(sp, locationInfoTypeToken)
        }
        return homeLocation != null
    }

    private fun loadPhoneNumber() {
        phoneNumber = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PHONE, null)
        if (phoneNumber == null) {
            setPhone.text = getString(R.string.setDestPhone)
        } else {
            setPhone.text = getString(R.string.resetDestPhone)
            testSMS.visibility = View.VISIBLE
        }
    }

    private fun saveHomeLocation() {
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(HOME, Gson().toJson(homeLocation)).apply()
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

        setPhone.setOnClickListener {
            if (phoneNumber == null) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    setPhoneDialog()
                } else {
                    requestSMSPermission()
                }
            } else { // reset phone number
                setPhone.text = getString(R.string.setDestPhone)
                testSMS.visibility = View.GONE
                phoneNumber = null
                applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putString(PHONE, phoneNumber).apply()
            }
        }

        testSMS.setOnClickListener {
            val intent = Intent(ACTION_SMS)
            intent.putExtra(PHONE, phoneNumber)
            intent.putExtra(CONTENT, "Honey I'm Sending a Test Message!")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
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

    private fun requestSMSPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS
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
            REQUEST_SMS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setPhoneDialog()
                } else {
                    Toast.makeText(this, "You must permit sending SMS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun setPhoneDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("With EditText")
        val dialogLayout = inflater.inflate(R.layout.alert_dialog_tv, null)
        val phoneNum = dialogLayout.findViewById<EditText>(R.id.phoneNumber)
        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            if (android.util.Patterns.PHONE.matcher(phoneNum.text.toString()).matches()) {
                applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putString(PHONE, phoneNum.text.toString()).apply()
                this.phoneNumber = phoneNum.text.toString()
                testSMS.visibility = View.VISIBLE
                setPhone.text = getString(R.string.resetDestPhone)
            } else {
                Toast.makeText(this, "You must enter valid phone number", Toast.LENGTH_SHORT).show()
            }
        }.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopTracking()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
}
