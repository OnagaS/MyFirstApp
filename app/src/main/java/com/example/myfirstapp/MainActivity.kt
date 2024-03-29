package com.example.myfirstapp


import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import kotlin.math.roundToLong


const val REQUEST_PERMISSION_LOCATION = 1
const val REQUEST_PERMISSION_LOCATION_BACKGROUND = 2

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 目的地
    private var destinationAddress: EditText? = null
    private var destinationLatitude: Double = 26.15359
    private var destinationLongitude: Double = 127.67158
    // 現在地
    private var currentAddress: EditText? = null
    private var currentLatitude: Double = 0.00000
    private var currentLongitude: Double = 0.00000
    // 距離
    private var distance: EditText? = null

    // マーカー
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 位置情報へのアクセス権限確認
        requestPermission()
        // 通知チャネル作成
        createNotificationChannel(
            LocationService.CHANNEL_ID_SERVICE,
            NotificationManager.IMPORTANCE_MIN
        )
        createNotificationChannel(
            LocationService.CHANNEL_ID_ALARM,
            NotificationManager.IMPORTANCE_MAX
        )

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()

        // 目的地 住所
        destinationAddress = findViewById<EditText>(R.id.destinationAddress)
        destinationAddress?.keyListener = null
        // 現在地 住所
        currentAddress = findViewById<EditText>(R.id.currentAddress)
        currentAddress?.keyListener = null
        // 距離
        distance = findViewById<EditText>(R.id.distance)
        distance?.keyListener = null

        destinationAddress?.setText(
            CommonFunc().reverseGeocoding(
                this,
                destinationLatitude,
                destinationLongitude
            )
        )

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // 現在地に値を設定
                    currentLatitude =
                        (location.latitude * 100000).roundToLong().toDouble() / 100000
                    currentLongitude =
                        (location.longitude * 100000).roundToLong().toDouble() / 100000
                    currentAddress?.setText(
                        CommonFunc().reverseGeocoding(
                            this,
                            currentLatitude,
                            currentLongitude
                        )
                    )

                    // 距離に値を設定
                    var distanceValue = CommonFunc().calcDistance(
                        destinationLatitude,
                        destinationLongitude,
                        location.latitude,
                        location.longitude
                    )
                    distanceValue = ((distanceValue.roundToLong().toFloat() / 1000))
                    distance?.setText(distanceValue.toString())
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    finish()
                } else {
                    requestPermission()
                }
                return
            }
            REQUEST_PERMISSION_LOCATION_BACKGROUND -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    finish()
                }
                return
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        val destination = LatLng(destinationLatitude, destinationLongitude)
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        marker = googleMap.addMarker(
            MarkerOptions()
                .position(destination)
                .title(getString(R.string.destination))
        )
        marker?.showInfoWindow()
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(destination))
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
        googleMap.isMyLocationEnabled = true

        // タップした時のリスナーをセット
        googleMap.setOnMapClickListener { tapLocation ->
            val location = LatLng(tapLocation.latitude, tapLocation.longitude)
            val destinationName: String = CommonFunc().reverseGeocoding(
                this,
                location.latitude,
                location.longitude
            )
            updateDestination(googleMap, location, destinationName)
        }
        // スポットをタップした時のリスナーをセット
        googleMap.setOnPoiClickListener { poi: PointOfInterest ->
            updateDestination(googleMap, poi.latLng, poi.name)
        }
    }

    /*
     * 目的地を更新
     */
    private fun updateDestination(googleMap: GoogleMap, location: LatLng, destinationName: String) {
        destinationLatitude = location.latitude
        destinationLongitude = location.longitude
        // 距離に値を設定
        var distanceValue = CommonFunc().calcDistance(
            location.latitude,
            location.longitude,
            currentLatitude,
            currentLongitude
        )
        distanceValue = ((distanceValue.roundToLong().toFloat() / 1000))
        distance?.setText(distanceValue.toString())
        destinationAddress?.setText(destinationName)

        marker?.remove()
        marker = googleMap.addMarker(MarkerOptions().position(location).title(destinationName))
        marker?.showInfoWindow()
    }

    /*
     * 位置情報へのアクセス権限確認
     */
    private fun requestPermission() {
        val permissionAccessCoarseLocationApproved =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (!permissionAccessCoarseLocationApproved) {
            // 位置情報の権限が無いため、許可を求める
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_PERMISSION_LOCATION
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissionAccessBackgroundLocationApproved =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            if (!permissionAccessBackgroundLocationApproved) {
                // 位置情報の権限が無いため、許可を求める
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    REQUEST_PERMISSION_LOCATION_BACKGROUND
                )
            }
        }
    }

    /*
     * 通知チャネル作成
     */
    private fun createNotificationChannel(channelId: String, importance: Int) {
        val name = getString(R.string.app_name)
        val descriptionText = getString(R.string.app_name)
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun startAlarm(view: View) {
        val intent = Intent(view.context, LocationService::class.java)
        intent.putExtra("destinationLatitude", destinationLatitude)
        intent.putExtra("destinationLongitude", destinationLongitude)
        startForegroundService(intent)
    }

    fun stopAlarm(view: View) {
        val intent = Intent(view.context, LocationService::class.java)
        stopService(intent)
    }
}