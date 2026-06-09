package com.example.logic

import android.location.Location
import com.example.data.DailySchedule
import com.example.data.Gym
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek

object CheckInValidator {
    
    fun calculateDistance(currentLocation: Location?, gym: Gym): Float {
        if (currentLocation == null) return Float.MAX_VALUE
        val gymLocation = Location("").apply {
            latitude = gym.latitude
            longitude = gym.longitude
        }
        return currentLocation.distanceTo(gymLocation)
    }

    // Check if the given location is within the gym's radius
    fun isLocationValid(currentLocation: Location?, gym: Gym, toleranceMeters: Float = 50f): Boolean {
        if (currentLocation == null) return false
        val distance = calculateDistance(currentLocation, gym)
        return distance <= (gym.radiusMeters + toleranceMeters)
    }

    // Check if any scheduled time is valid across all schedules
    fun isAnyTimeValid(now: LocalDateTime, schedules: List<DailySchedule>, bufferMinutes: Long = 15): Boolean {
        return schedules.any { isTimeValidForSchedule(now, it, bufferMinutes) }
    }

    fun isTimeValidForSchedule(now: LocalDateTime, schedule: DailySchedule, bufferMinutes: Long = 15): Boolean {
        if (schedule.dayOfWeek != now.dayOfWeek) return false
        val currentMinutes = now.toLocalTime().toSecondOfDay() / 60
        val startMinutes = schedule.startTime.toSecondOfDay() / 60 - bufferMinutes
        val endMinutes = schedule.endTime.toSecondOfDay() / 60 + bufferMinutes
        return currentMinutes in startMinutes..endMinutes
    }

    fun validateCheckIn(now: LocalDateTime, currentLocation: Location?, schedules: List<DailySchedule>, gyms: List<Gym>, bufferMinutes: Long = 15): Boolean {
        if (currentLocation == null) return false

        // Find active schedules right now
        val activeSchedules = schedules.filter { isTimeValidForSchedule(now, it, bufferMinutes) }
        
        // For each active schedule, check if we are at its gym
        for (schedule in activeSchedules) {
            val gym = gyms.find { it.id == schedule.gymId } ?: continue
            if (isLocationValid(currentLocation, gym)) {
                return true
            }
        }
        return false
    }
    
    // Check if there was a missed schedule (i.e. a schedule passed, but last check-in was not registered for it)
    fun hasMissedSchedule(now: LocalDateTime, lastCheckIn: LocalDateTime, schedules: List<DailySchedule>): Boolean {
        if (schedules.isEmpty()) return false
        
        val daysBetween = java.time.Duration.between(lastCheckIn, now).toDays()
        if (daysBetween > 7) {
            return true
        }

        var checkDate = lastCheckIn.toLocalDate()
        val endDate = now.toLocalDate()

        while (!checkDate.isAfter(endDate)) {
            val day = checkDate.dayOfWeek
            for (schedule in schedules) {
                if (schedule.dayOfWeek == day) {
                    val scheduleStartTime = LocalDateTime.of(checkDate, schedule.startTime).minusMinutes(15)
                    val scheduleEndTime = LocalDateTime.of(checkDate, schedule.endTime).plusMinutes(15)
                    
                    if (now.isAfter(scheduleEndTime)) {
                        if (lastCheckIn.isBefore(scheduleStartTime)) {
                            return true
                        }
                    }
                }
            }
            checkDate = checkDate.plusDays(1)
        }
        return false
    }

    fun isOnCooldown(nowMillis: Long, lastCheckInMillis: Long): Boolean {
        if (lastCheckInMillis <= 0) return false
        val cooldownMillis = 12 * 60 * 60 * 1000L
        return (nowMillis - lastCheckInMillis) < cooldownMillis
    }

    fun getRemainingCooldownHours(nowMillis: Long, lastCheckInMillis: Long): Long {
        if (lastCheckInMillis <= 0) return 0
        val cooldownMillis = 12 * 60 * 60 * 1000L
        val diff = nowMillis - lastCheckInMillis
        if (diff >= cooldownMillis) return 0
        val remainingMillis = cooldownMillis - diff
        val hours = remainingMillis / (60 * 60 * 1000L)
        val minutes = (remainingMillis % (60 * 60 * 1000L)) / (60 * 1000L)
        return if (minutes > 0) hours + 1 else hours
    }
}
