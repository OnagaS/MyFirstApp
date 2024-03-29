package com.example.myfirstapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.*
import kotlin.math.roundToLong

class LocationService : Service() {

    companion object {
        const val CHANNEL_ID_ALARM = "9901"
        const val CHANNEL_ID_SERVICE = "9902"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 目的地
        var destinationLatitude = 0.00
        var destinationLongitude = 0.00
        if (intent != null) {
            destinationLatitude = intent.getDoubleExtra("destinationLatitude", 0.00)
            destinationLongitude = intent.getDoubleExtra("destinationLongitude", 0.00)
        }
        var updatedCount = 0
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updatedCount++
                    Log.d(
                        this.javaClass.name,
                        "[${updatedCount}] ${location.latitude} , ${location.longitude}"
                    )
                    val distance = CommonFunc().calcDistance(
                        destinationLatitude,
                        destinationLongitude,
                        location.latitude,
                        location.longitude
                    )
                    if (distance < 1000) {
                        createNotification(distance)
                    }
                }
            }
        }

        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("位置情報を取得しています...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openIntent)
            .build()

        startForeground(9999, notification)

        startLocationUpdates()

        return START_STICKY
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun stopService(name: Intent?): Boolean {
        stopLocationUpdates()
        return super.stopService(name)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        stopSelf()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = createLocationRequest()
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    fun createNotification(distance: Float) {
        val distanceDouble: Double = distance.toDouble()
        val distanceMeter: Long = distanceDouble.roundToLong()
        val text: String =
            getString(R.string.remain) + distanceMeter + getString(R.string.meter)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(0, builder.build())
        }
    }
}