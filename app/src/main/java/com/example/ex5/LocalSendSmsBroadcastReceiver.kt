package com.example.ex5

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class LocalSendSmsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Send sms not allowed")
            return
        } else {
            val phoneNumber = intent.getStringExtra(PHONE)
            val smsContent = intent.getStringExtra(CONTENT)
            if (phoneNumber == null || smsContent == null) {
                Log.e(TAG, "No phone number / no message content")
                return
            }
            try {
                SmsManager.getDefault().sendTextMessage(phoneNumber, null, smsContent, null, null)
                setNotificationChannel(context)
                createNotification(context, phoneNumber, smsContent)
            } catch (e: java.lang.IllegalArgumentException) {
                Toast.makeText(
                    context,
                    "Couldn't send SMS. Check that dest phone is valid.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_ID
            val descriptionText = "Honey Im Home channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(context: Context, phoneNumber: String, smsContent: String) {
        val ntf = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Honey Im Home")
            .setContentText("sending sms to $phoneNumber: $smsContent")
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, ntf)
    }
}
