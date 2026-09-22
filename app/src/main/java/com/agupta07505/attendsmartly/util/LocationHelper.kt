/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(context: Context): Boolean {
        return hasFineLocationPermission(context) || hasCoarseLocationPermission(context)
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            hasLocationPermission(context)
        }
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }

    fun isWithinRange(
        userLat: Double,
        userLng: Double,
        targetLat: Double,
        targetLng: Double,
        radiusMeters: Int
    ): Boolean {
        return distanceMeters(userLat, userLng, targetLat, targetLng) <= radiusMeters
    }

    fun formatCoordinates(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.5f° %s, %.5f° %s", Math.abs(lat), latDir, Math.abs(lng), lngDir)
    }

    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(context)
            resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        return suspendCancellableCoroutine { continuation ->
            try {
                if (!isGooglePlayServicesAvailable(context)) {
                    val fallbackLoc = getLocationFromManager(context)
                    if (continuation.isActive) continuation.resume(fallbackLoc)
                    return@suspendCancellableCoroutine
                }

                val priority = if (hasFineLocationPermission(context)) {
                    Priority.PRIORITY_HIGH_ACCURACY
                } else {
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                }

                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()

                continuation.invokeOnCancellation {
                    try { cts.cancel() } catch (_: Throwable) {}
                }

                fusedClient.getCurrentLocation(priority, cts.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            if (continuation.isActive) continuation.resume(location)
                        } else {
                            try {
                                fusedClient.lastLocation
                                    .addOnSuccessListener { lastLoc ->
                                        val resolved = lastLoc ?: getLocationFromManager(context)
                                        if (continuation.isActive) continuation.resume(resolved)
                                    }
                                    .addOnFailureListener {
                                        val fallbackLoc = getLocationFromManager(context)
                                        if (continuation.isActive) continuation.resume(fallbackLoc)
                                    }
                            } catch (_: Throwable) {
                                val fallbackLoc = getLocationFromManager(context)
                                if (continuation.isActive) continuation.resume(fallbackLoc)
                            }
                        }
                    }
                    .addOnFailureListener {
                        val fallbackLoc = getLocationFromManager(context)
                        if (continuation.isActive) continuation.resume(fallbackLoc)
                    }
            } catch (_: Throwable) {
                val fallbackLoc = getLocationFromManager(context)
                if (continuation.isActive) continuation.resume(fallbackLoc)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationFromManager(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = lm.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                val l = lm.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            } catch (_: Exception) {}
        }
        return bestLocation
    }
}
