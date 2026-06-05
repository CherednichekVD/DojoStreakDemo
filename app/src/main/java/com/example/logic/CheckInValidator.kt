package com.example.logic

import android.location.Location
import com.example.data.DailySchedule
import com.example.data.Gym
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek

object CheckInValidator {
    
    // Check if the given location is within the gym's radius
    fun isLocationValid(currentLocation: Location?, gym: Gym): Boolean {
        if (currentLocation == null) return false
        
        val gymLocation = Location("").apply {
            latitude = gym.latitude
            longitude = gym.longitude
        }
        val distance = currentLocation.distanceTo(gymLocation)
        return distance <= gym.radiusMeters
    }

    // Check if the current time is within any of the scheduled classes, plus buffer
    fun isTimeValid(now: LocalDateTime, schedules: List<DailySchedule>, bufferMinutes: Long = 15): Boolean {
        val currentDay = now.dayOfWeek
        val currentTime = now.toLocalTime()
        val currentMinutes = currentTime.toSecondOfDay() / 60

        return schedules.any { schedule ->
            if (schedule.dayOfWeek == currentDay) {
                // simple comparison considering buffer
                val startMinutes = schedule.startTime.toSecondOfDay() / 60 - bufferMinutes
                val endMinutes = schedule.endTime.toSecondOfDay() / 60 + bufferMinutes
                
                currentMinutes in startMinutes..endMinutes
            } else {
                false
            }
        }
    }
    
    // Check if there was a missed schedule (i.e. a schedule passed, but last check-in was before it)
    fun hasMissedSchedule(now: LocalDateTime, lastCheckIn: LocalDateTime, schedules: List<DailySchedule>): Boolean {
        // This is a simplified check. A full check would iterate over all days between lastCheckIn and now.
        // For MVP, limit to checking up to 7 days back.
        if (schedules.isEmpty()) return false
        
        val daysBetween = java.time.Duration.between(lastCheckIn, now).toDays()
        if (daysBetween > 7) {
            // More than a week without checking in, definitely missed at least one schedule (assuming >=1 schedule/week)
            return schedules.isNotEmpty()
        }

        var checkTime = lastCheckIn
        while (checkTime.isBefore(now)) {
            val day = checkTime.dayOfWeek
            for (schedule in schedules) {
                if (schedule.dayOfWeek == day) {
                    val scheduleEndTime = LocalDateTime.of(checkTime.toLocalDate(), schedule.endTime)
                    if (scheduleEndTime.isAfter(lastCheckIn) && scheduleEndTime.isBefore(now)) {
                        return true // found a schedule that ended between last check-in and now
                    }
                }
            }
            checkTime = checkTime.plusDays(1).with(LocalTime.MIN)
        }
        return false
    }
}
