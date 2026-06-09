package com.example.ui.viewmodels

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.location.LocationTracker
import com.example.logic.CheckInValidator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.getDatabase(application)
    private val repository = DojoRepository(database.dojoDao())
    private val locationTracker = LocationTracker(application)

    val allGyms: StateFlow<List<Gym>> = repository.allGyms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings?> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentGrowthStage: StateFlow<Int> = userSettings.map { settings ->
        val streak = settings?.currentStreak ?: 0
        when {
            streak >= 30 -> 5
            streak >= 15 -> 4
            streak >= 10 -> 3
            streak >= 5 -> 2
            else -> 1
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _isTimeValid = MutableStateFlow(false)
    val isTimeValid: StateFlow<Boolean> = _isTimeValid

    private val _isLocationValid = MutableStateFlow(false)
    val isLocationValid: StateFlow<Boolean> = _isLocationValid

    private val _locationStatusMsg = MutableStateFlow<String>("Ожидание получения локации...")
    val locationStatusMsg: StateFlow<String> = _locationStatusMsg

    private val _nextTrainingSummary = MutableStateFlow<String?>(null)
    val nextTrainingSummary: StateFlow<String?> = _nextTrainingSummary

    init {
        viewModelScope.launch {
            repository.checkAndPopulateMockGyms()
        }
        
        // Evaluate missed schedules and current validity automatically when settings or time changes
        viewModelScope.launch {
            userSettings.collectLatest { settings ->
                evaluateConditions(settings)
            }
        }
    }

    fun fetchLocation() {
        _locationStatusMsg.value = "Поиск координат GPS..."
        viewModelScope.launch {
            if (!locationTracker.isLocationEnabled()) {
                _locationStatusMsg.value = "Геопозиция (GPS) ВЫКЛЮЧЕНА. Пожалуйста, включите её."
                _currentLocation.value = null
                evaluateConditions(userSettings.value, null)
                return@launch
            }

            val loc = locationTracker.getCurrentLocation()
            if (loc != null) {
                _locationStatusMsg.value = "Локация найдена!"
            } else {
                _locationStatusMsg.value = "Доступ к геопозиции получен, ожидание сигнала GPS..."
            }
            _currentLocation.value = loc
            evaluateConditions(userSettings.value, loc)
        }
    }

    private fun evaluateConditions(settings: UserSettings?, loc: Location? = _currentLocation.value) {
        if (settings == null) return

        val now = LocalDateTime.now()
        val schedules = ScheduleParser.parse(settings.scheduleCsv)
        val gyms = allGyms.value

        // Check if missed schedule
        if (settings.lastCheckInMillis > 0) {
            val lastCheckInTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(settings.lastCheckInMillis), ZoneId.systemDefault())
            if (CheckInValidator.hasMissedSchedule(now, lastCheckInTime, schedules)) {
                viewModelScope.launch { repository.resetStreak() }
            }
        }

        val activeTime = CheckInValidator.isAnyTimeValid(now, schedules)
        _isTimeValid.value = activeTime

        if (loc != null) {
            var atAnyGym = false

            // Check if at *any* gym for the UI indicator "At Dojo Location"
            for (schedule in schedules) {
                 val gym = gyms.find { it.id == schedule.gymId } ?: continue
                 if (CheckInValidator.isLocationValid(loc, gym)) {
                     atAnyGym = true
                 }
            }

            _isLocationValid.value = atAnyGym
            
            var debugStr = "Локация найдена!\nОтладка GPS:\n"
            debugStr += "- Пользователь: ${"%.6f".format(loc.latitude)}, ${"%.6f".format(loc.longitude)}\n"

            var minDistance = Float.MAX_VALUE
            var closestGym: Gym? = null
            
            // We want to check against the currently active gym first
            val activeTimeSchedules = schedules.filter { CheckInValidator.isTimeValidForSchedule(now, it) }
            val gymsToCheck = if (activeTimeSchedules.isNotEmpty()) {
                val activeIds = activeTimeSchedules.map { it.gymId }
                gyms.filter { it.id in activeIds }
            } else {
                val userGymIds = schedules.map { it.gymId }.distinct()
                gyms.filter { it.id in userGymIds }
            }
            
            for (gym in gymsToCheck) {
                 val dist = CheckInValidator.calculateDistance(loc, gym)
                 if (dist < minDistance) {
                     minDistance = dist
                     closestGym = gym
                 }
            }

            if (closestGym != null) {
                 debugStr += "- Зал: ${"%.6f".format(closestGym.latitude)}, ${"%.6f".format(closestGym.longitude)}\n"
                 val threshold = closestGym.radiusMeters + 50f
                 debugStr += "- Рассчитанная дистанция: ${minDistance.toInt()} метров (Допуск: ${threshold.toInt()}м)"
            } else {
                 debugStr += "- Активные залы не выбраны."
            }
            
            _locationStatusMsg.value = debugStr
            
        } else {
            _isLocationValid.value = false
            // Keep the previous message if it's about GPS being off
            if (!_locationStatusMsg.value.contains("ВЫКЛЮЧЕНА")) {
                _locationStatusMsg.value = "Доступ к геопозиции получен, ожидание сигнала GPS..."
            }
        }
        
        _nextTrainingSummary.value = calculateNextTraining(now, schedules, gyms)
    }

    private fun calculateNextTraining(now: LocalDateTime, schedules: List<DailySchedule>, gyms: List<Gym>): String? {
        if (schedules.isEmpty()) return "Расписание не настроено."
        
        var minDiff = Long.MAX_VALUE
        var nextGymName = ""
        var nextTimeStr = ""
        var nextDayStr = ""

        val currentDayVal = now.dayOfWeek.value
        val currentTimeInMin = now.toLocalTime().toSecondOfDay() / 60

        for (schedule in schedules) {
            val dayDiff = (schedule.dayOfWeek.value - currentDayVal + 7) % 7
            val schedTimeMin = schedule.startTime.toSecondOfDay() / 60

            val totalDiffMin = if (dayDiff == 0 && schedTimeMin > currentTimeInMin) {
                (schedTimeMin - currentTimeInMin).toLong()
            } else if (dayDiff > 0) {
                (dayDiff * 24 * 60 + schedTimeMin - currentTimeInMin).toLong()
            } else if (dayDiff == 0 && schedTimeMin <= currentTimeInMin) {
                (7 * 24 * 60 + schedTimeMin - currentTimeInMin).toLong()
            } else {
                Long.MAX_VALUE
            }

            if (totalDiffMin < minDiff) {
                minDiff = totalDiffMin
                val gym = gyms.find { it.id == schedule.gymId }
                nextGymName = gym?.name ?: "Неизвестный зал"
                val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                nextTimeStr = schedule.startTime.format(formatter)
                nextDayStr = when (schedule.dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> "ПН"
                    java.time.DayOfWeek.TUESDAY -> "ВТ"
                    java.time.DayOfWeek.WEDNESDAY -> "СР"
                    java.time.DayOfWeek.THURSDAY -> "ЧТ"
                    java.time.DayOfWeek.FRIDAY -> "ПТ"
                    java.time.DayOfWeek.SATURDAY -> "СБ"
                    java.time.DayOfWeek.SUNDAY -> "ВС"
                }
            }
        }
        
        if (minDiff == Long.MAX_VALUE) return null
        return if (minDiff < 24 * 60) {
           "Следующая: Сегодня в $nextTimeStr, $nextGymName"
        } else {
           "Следующая: $nextDayStr в $nextTimeStr, $nextGymName"
        }
    }

    fun saveSetup(schedules: List<DailySchedule>) {
        viewModelScope.launch {
            repository.updateSetup(schedules)
        }
    }

    fun performCheckIn() {
        viewModelScope.launch {
            repository.performCheckIn()
            evaluateConditions(userSettings.value)
        }
    }
}
