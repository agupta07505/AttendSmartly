/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.agupta07505.attendsmartly.AttendSmartlyApplication
import com.agupta07505.attendsmartly.receiver.GeofenceBroadcastReceiver
import com.agupta07505.attendsmartly.util.DateUtils
import com.agupta07505.attendsmartly.util.LocationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.first

object ClassGeofenceManager {

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    @SuppressLint("MissingPermission")
    suspend fun registerGeofencesForDay(context: Context, dateIso: String = DateUtils.todayIso()) {
        try {
            if (!LocationHelper.isGooglePlayServicesAvailable(context)) return
            if (!LocationHelper.hasFineLocationPermission(context)) return
            if (!LocationHelper.hasBackgroundLocationPermission(context)) return

            val app = context.applicationContext as AttendSmartlyApplication
            val prefs = app.userPreferencesRepository.userPreferencesFlow.first()
            if (!prefs.autoAttendanceEnabled) {
                removeGeofences(context)
                return
            }

            val repository = app.repository
            val dayOfWeek = DateUtils.getDayOfWeekInt(dateIso)
            val entries = repository.getTimetableForDayAndDate(dayOfWeek, dateIso).first()
            val allSubjects = repository.allSubjects.first().associateBy { it.id }

            val geofenceList = mutableListOf<Geofence>()

            for (entry in entries) {
                val subject = allSubjects[entry.subjectId] ?: continue
                val lat = entry.latitude ?: subject.latitude
                val lng = entry.longitude ?: subject.longitude
                val radius = entry.locationRadiusMeters.takeIf { it > 0 }
                    ?: subject.locationRadiusMeters.takeIf { it > 0 }
                    ?: prefs.autoAttendanceRadiusMeters

                if (lat != null && lng != null) {
                    val requestId = "${entry.id}_${entry.subjectId}_${dateIso}"
                    val dwellMinutes = prefs.autoAttendanceDwellMinutes.coerceAtLeast(1)

                    val geofence = Geofence.Builder()
                        .setRequestId(requestId)
                        .setCircularRegion(lat, lng, radius.toFloat())
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER)
                        .setLoiteringDelay(dwellMinutes * 60 * 1000)
                        .build()

                    geofenceList.add(geofence)
                }
            }

            if (geofenceList.isEmpty()) {
                removeGeofences(context)
                return
            }

            val geofencingClient = LocationServices.getGeofencingClient(context)
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build()

            geofencingClient.addGeofences(request, getGeofencePendingIntent(context))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun removeGeofences(context: Context) {
        try {
            if (!LocationHelper.isGooglePlayServicesAvailable(context)) return
            val geofencingClient = LocationServices.getGeofencingClient(context)
            geofencingClient.removeGeofences(getGeofencePendingIntent(context))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
