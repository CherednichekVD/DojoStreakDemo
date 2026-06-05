package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

class DojoRepository(private val dojoDao: DojoDao) {

    val allGyms: Flow<List<Gym>> = dojoDao.getAllGyms()
    val userSettings: Flow<UserSettings?> = dojoDao.getUserSettings()
    
    suspend fun checkAndPopulateMockGyms() {
        if (dojoDao.getGymCount() == 0) {
            val mockGyms = listOf(
                Gym("gym1", "Dragon Martial Arts", 37.4220, -122.0840, 100f),
                Gym("gym2", "Cobra Kai Dojo", 34.0522, -118.2437, 50f),
                Gym("gym3", "Miyagi-Do", 34.0620, -118.2500, 50f)
            )
            dojoDao.insertGyms(mockGyms)
        }
    }

    suspend fun updateSetup(gymId: String, schedules: List<DailySchedule>) {
        val current = dojoDao.getUserSettingsSync()
        val newSettings = current?.copy(
            selectedGymId = gymId,
            scheduleCsv = ScheduleParser.serialize(schedules)
        ) ?: UserSettings(
            selectedGymId = gymId,
            scheduleCsv = ScheduleParser.serialize(schedules),
            currentStreak = 0,
            lastCheckInMillis = 0L
        )
        dojoDao.saveUserSettings(newSettings)
    }

    suspend fun performCheckIn() {
        val current = dojoDao.getUserSettingsSync()
        if (current != null) {
            val newStreak = current.currentStreak + 1
            dojoDao.updateCheckIn(newStreak, System.currentTimeMillis())
        }
    }

    suspend fun resetStreak() {
        dojoDao.resetStreak()
    }
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "dojostreak_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
