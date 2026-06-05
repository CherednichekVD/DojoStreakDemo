package com.example.data

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DailySchedule(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime
)

object ScheduleParser {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parse(csv: String): List<DailySchedule> {
        if (csv.isBlank()) return emptyList()
        return csv.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 3) {
                try {
                    DailySchedule(
                        dayOfWeek = DayOfWeek.valueOf(parts[0]),
                        startTime = LocalTime.parse(parts[1], formatter),
                        endTime = LocalTime.parse(parts[2], formatter)
                    )
                } catch (e: Exception) { null }
            } else null
        }
    }

    fun serialize(schedules: List<DailySchedule>): String {
        return schedules.joinToString(";") {
            "${it.dayOfWeek.name},${it.startTime.format(formatter)},${it.endTime.format(formatter)}"
        }
    }
}
