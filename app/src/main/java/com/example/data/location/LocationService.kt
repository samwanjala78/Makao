package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.data.seed.KenyaPropertySeed
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val isGpsActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocation? {
        if (!hasLocationPermission()) return null

        return try {
            val cancellationSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationSource.token
            ).await()

            if (location != null) {
                UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    displayName = "Current GPS Location",
                    isGpsActive = true
                )
            } else {
                // Fallback to last known location
                val lastLoc = fusedLocationClient.lastLocation.await()
                if (lastLoc != null) {
                    UserLocation(
                        latitude = lastLoc.latitude,
                        longitude = lastLoc.longitude,
                        displayName = "Last GPS Location",
                        isGpsActive = true
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        // Default central Nairobi location (Westlands / Nairobi CBD)
        val DEFAULT_KENYA_LOCATION = UserLocation(
            latitude = -1.2683,
            longitude = 36.8070,
            displayName = "Nairobi (Westlands / Central)",
            isGpsActive = false
        )

        fun calculateDistanceKm(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return (results[0] / 1000.0).toDouble()
        }

        fun getPresetLocations() = KenyaPropertySeed.KENYA_PRESET_LOCATIONS
    }
}
