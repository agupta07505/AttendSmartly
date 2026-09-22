/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agupta07505.attendsmartly.AttendSmartlyApplication
import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.notification.NotificationHelper
import com.agupta07505.attendsmartly.util.DateUtils
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = try {
            GeofencingEvent.fromIntent(intent)
        } catch (_: Throwable) {
            null
        } ?: return

        if (geofencingEvent.hasError()) return

        val transitionType = geofencingEvent.geofenceTransition
        if (transitionType != Geofence.GEOFENCE_TRANSITION_DWELL) {
            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        val app = context.applicationContext as AttendSmartlyApplication
        val repository = app.repository
        val prefsRepo = app.userPreferencesRepository

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = prefsRepo.userPreferencesFlow.first()
                if (!prefs.autoAttendanceEnabled) return@launch

                val todayIso = DateUtils.todayIso()
                val now = LocalTime.now()

                for (geofence in triggeringGeofences) {
                    val parts = geofence.requestId.split("_")
                    if (parts.size < 3) continue
                    val timetableEntryId = parts[0].toLongOrNull() ?: continue
                    val sessionDate = parts[2]

                    if (sessionDate != todayIso) continue

                    val entry = repository.getTimetableEntryById(timetableEntryId) ?: continue
                    val startTime = try {
                        LocalTime.parse(entry.startTime, DateUtils.timeFormatter24)
                    } catch (_: Exception) { null } ?: continue

                    val endTime = try {
                        LocalTime.parse(entry.endTime, DateUtils.timeFormatter24)
                    } catch (_: Exception) { null } ?: continue

                    // Check if current time is within class hours (with small 5 min grace window)
                    if (now.isBefore(startTime.minusMinutes(5)) || now.isAfter(endTime.plusMinutes(5))) {
                        continue
                    }

                    // Check semester boundaries if enabled
                    if (prefs.trackBySemester) {
                        if (prefs.semesterStartDate.isNotBlank() && todayIso < prefs.semesterStartDate) continue
                        if (prefs.semesterEndDate.isNotBlank() && todayIso > prefs.semesterEndDate) continue
                    }

                    var session = repository.getSessionForTimetableAndDate(entry.id, todayIso)
                    val sessionId = if (session != null) {
                        session.id
                    } else {
                        val newSession = AttendanceSessionEntity(
                            subjectId = entry.subjectId,
                            timetableEntryId = entry.id,
                            sessionDate = todayIso,
                            startTime = entry.startTime,
                            endTime = entry.endTime,
                            expectedUnitCount = entry.attendanceUnitCount,
                            autoMarked = true,
                            notes = "Auto-marked Present via location (present 5+ mins)"
                        )
                        val units = (0 until entry.attendanceUnitCount).map { idx ->
                            AttendanceUnitEntity(
                                sessionId = 0,
                                unitIndex = idx,
                                status = AttendanceStatus.UNMARKED.name
                            )
                        }
                        repository.createOrUpdateSessionWithUnits(newSession, units)
                    }

                    val currentUnits = repository.getUnitsForSession(sessionId).first()
                    val alreadyPresent = currentUnits.isNotEmpty() && currentUnits.all { it.status == AttendanceStatus.PRESENT.name }

                    if (!alreadyPresent) {
                        repository.markCompleteSessionStatus(sessionId, AttendanceStatus.PRESENT)

                        val updatedSession = repository.getSessionById(sessionId)
                        if (updatedSession != null) {
                            repository.createOrUpdateSessionWithUnits(
                                updatedSession.copy(
                                    autoMarked = true,
                                    notes = if (updatedSession.notes.isBlank()) "Auto-marked Present via location (present 5+ mins)" else updatedSession.notes
                                ),
                                currentUnits.map { it.copy(status = AttendanceStatus.PRESENT.name) }
                            )
                        }

                        val subject = repository.getSubjectById(entry.subjectId)
                        val room = if (entry.roomOverride.isNotBlank()) entry.roomOverride else (subject?.room ?: "")
                        NotificationHelper.showAutoAttendanceNotification(
                            context = context,
                            notificationId = (entry.id * 100 + 77).toInt(),
                            subjectName = subject?.name ?: "Class",
                            room = room,
                            minutesDwell = prefs.autoAttendanceDwellMinutes
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "com.agupta07505.attendsmartly.ACTION_GEOFENCE_EVENT"
    }
}
