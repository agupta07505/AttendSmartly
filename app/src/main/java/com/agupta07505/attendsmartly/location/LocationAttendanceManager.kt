/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.location

import android.content.Context
import com.agupta07505.attendsmartly.AttendSmartlyApplication
import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.notification.NotificationHelper
import com.agupta07505.attendsmartly.util.DateUtils
import com.agupta07505.attendsmartly.util.LocationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalTime

sealed class LocationCheckResult {
    object NoPermission : LocationCheckResult()
    object Disabled : LocationCheckResult()
    object NoOngoingClass : LocationCheckResult()
    data class OutsideRange(val subjectName: String, val distanceMeters: Float, val radiusMeters: Int) : LocationCheckResult()
    data class Dwelling(val subjectName: String, val elapsedSeconds: Long, val targetSeconds: Long) : LocationCheckResult()
    data class AutoMarked(val subjectName: String, val room: String) : LocationCheckResult()
    data class AlreadyMarked(val subjectName: String) : LocationCheckResult()
}

object LocationAttendanceManager {

    private const val PREFS_NAME = "location_attendance_tracker"

    suspend fun checkOngoingClassPresence(context: Context): LocationCheckResult {
        val app = context.applicationContext as AttendSmartlyApplication
        val prefs = app.userPreferencesRepository.userPreferencesFlow.first()

        if (!prefs.autoAttendanceEnabled) {
            return LocationCheckResult.Disabled
        }

        if (!LocationHelper.hasLocationPermission(context)) {
            return LocationCheckResult.NoPermission
        }

        val todayIso = DateUtils.todayIso()

        if (prefs.trackBySemester) {
            if (prefs.semesterStartDate.isNotBlank() && todayIso < prefs.semesterStartDate) {
                return LocationCheckResult.NoOngoingClass
            }
            if (prefs.semesterEndDate.isNotBlank() && todayIso > prefs.semesterEndDate) {
                return LocationCheckResult.NoOngoingClass
            }
        }

        val repository = app.repository
        val dayOfWeek = DateUtils.getDayOfWeekInt(todayIso)
        val timetable = repository.getTimetableForDayAndDate(dayOfWeek, todayIso).first()
        val allSubjects = repository.allSubjects.first().associateBy { it.id }
        val now = LocalTime.now()

        val ongoingEntries = timetable.filter { entry ->
            try {
                val start = LocalTime.parse(entry.startTime, DateUtils.timeFormatter24)
                val end = LocalTime.parse(entry.endTime, DateUtils.timeFormatter24)
                !now.isBefore(start) && !now.isAfter(end)
            } catch (_: Exception) {
                false
            }
        }

        if (ongoingEntries.isEmpty()) {
            return LocationCheckResult.NoOngoingClass
        }

        val userLocation = LocationHelper.getCurrentLocation(context) ?: return LocationCheckResult.NoOngoingClass

        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        for (entry in ongoingEntries) {
            val subject = allSubjects[entry.subjectId] ?: continue
            val targetLat = entry.latitude ?: subject.latitude
            val targetLng = entry.longitude ?: subject.longitude
            val targetRadius = entry.locationRadiusMeters.takeIf { it > 0 }
                ?: subject.locationRadiusMeters.takeIf { it > 0 }
                ?: prefs.autoAttendanceRadiusMeters

            if (targetLat == null || targetLng == null) continue

            val distance = LocationHelper.distanceMeters(
                userLocation.latitude,
                userLocation.longitude,
                targetLat,
                targetLng
            )

            val trackerKey = "first_seen_${entry.id}_${todayIso}"

            if (distance <= targetRadius) {
                // Check if already marked present
                var session = repository.getSessionForTimetableAndDate(entry.id, todayIso)
                if (session != null) {
                    val units = repository.getUnitsForSession(session.id).first()
                    if (units.isNotEmpty() && units.all { it.status == AttendanceStatus.PRESENT.name }) {
                        return LocationCheckResult.AlreadyMarked(subject.name)
                    }
                }

                val firstSeen = sharedPrefs.getLong(trackerKey, 0L)
                val currentTime = System.currentTimeMillis()
                val targetDwellMillis = prefs.autoAttendanceDwellMinutes * 60 * 1000L

                if (firstSeen == 0L) {
                    sharedPrefs.edit().putLong(trackerKey, currentTime).apply()
                    return LocationCheckResult.Dwelling(
                        subjectName = subject.name,
                        elapsedSeconds = 0,
                        targetSeconds = targetDwellMillis / 1000
                    )
                } else {
                    val elapsedMillis = currentTime - firstSeen
                    if (elapsedMillis >= targetDwellMillis) {
                        // Mark session present!
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

                        sharedPrefs.edit().remove(trackerKey).apply()

                        val room = if (entry.roomOverride.isNotBlank()) entry.roomOverride else subject.room
                        NotificationHelper.showAutoAttendanceNotification(
                            context = context,
                            notificationId = (entry.id * 100 + 77).toInt(),
                            subjectName = subject.name,
                            room = room,
                            minutesDwell = prefs.autoAttendanceDwellMinutes
                        )

                        return LocationCheckResult.AutoMarked(subject.name, room)
                    } else {
                        return LocationCheckResult.Dwelling(
                            subjectName = subject.name,
                            elapsedSeconds = elapsedMillis / 1000,
                            targetSeconds = targetDwellMillis / 1000
                        )
                    }
                }
            } else {
                // Outside range: reset tracker for this entry
                sharedPrefs.edit().remove(trackerKey).apply()
                return LocationCheckResult.OutsideRange(subject.name, distance, targetRadius)
            }
        }

        return LocationCheckResult.NoOngoingClass
    }
}
