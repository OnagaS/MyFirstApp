package com.example.myfirstapp

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.places.PlaceReport
import java.util.*

class CommonFunc {
    /*
     * 2点間の距離を計算
     */
    fun calcDistance(
        destinationLatitude: Double,
        destinationLongitude: Double,
        currentLatitude: Double,
        currentLongitude: Double
    ): Float {
        // 結果格納用
        val results = FloatArray(3)

        // 距離計算
        Location.distanceBetween(
            destinationLatitude,
            destinationLongitude,
            currentLatitude,
            currentLongitude,
            results
        )
        return results[0]
    }

    /*
     * 緯度・経度を住所へ変換
     */
    fun reverseGeocoding(context: Context, latitude: Double, longitude: Double): String {
        val gcd = Geocoder(context, Locale.getDefault())
        val addresses: List<Address> = gcd.getFromLocation(
            latitude, longitude, 1
        )

        var addressValue = ""
        addressValue += addresses[0].adminArea ?: ""
        addressValue += addresses[0].subAdminArea ?: ""
        addressValue += addresses[0].locality ?: ""
        addressValue += addresses[0].thoroughfare ?: ""
        addressValue += addresses[0].subThoroughfare ?: ""
        addressValue += addresses[0].featureName ?: ""
        return addressValue
    }
}