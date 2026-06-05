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

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _isTimeValid = MutableStateFlow(false)
    val isTimeValid: StateFlow<Boolean> = _isTimeValid

    private val _isLocationValid = MutableStateFlow(false)
    val isLocationValid: StateFlow<Boolean> = _isLocationValid

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
        viewModelScope.launch {
            val loc = locationTracker.getCurrentLocation()
            _currentLocation.value = loc
            evaluateConditions(userSettings.value, loc)
        }
    }

    private fun evaluateConditions(settings: UserSettings?, loc: Location? = _currentLocation.value) {
        if (settings == null) return

        val now = LocalDateTime.now()
        val schedules = ScheduleParser.parse(settings.scheduleCsv)
        
        // Check if missed schedule
        if (settings.lastCheckInMillis > 0) {
            val lastCheckInTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(settings.lastCheckInMillis), ZoneId.systemDefault())
            if (CheckInValidator.hasMissedSchedule(now, lastCheckInTime, schedules)) {
                viewModelScope.launch { repository.resetStreak() }
            }
        }

        // Validate current time
        _isTimeValid.value = CheckInValidator.isTimeValid(now, schedules)

        // Validate location
        if (loc != null) {
            val gym = allGyms.value.find { it.id == settings.selectedGymId }
            if (gym != null) {
                _isLocationValid.value = CheckInValidator.isLocationValid(loc, gym)
            } else {
                _isLocationValid.value = false
            }
        } else {
            _isLocationValid.value = false
        }
    }

    fun saveSetup(gymId: String, schedules: List<DailySchedule>) {
        viewModelScope.launch {
            repository.updateSetup(gymId, schedules)
        }
    }

    fun performCheckIn() {
        viewModelScope.launch {
            repository.performCheckIn()
            evaluateConditions(userSettings.value)
        }
    }
}
