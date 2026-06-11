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
                Gym("gym1", "Главный зал (Пермь)", 58.049688, 56.217047, 100f),
                Gym("gym2", "Династия", 58.021585, 56.289715, 100f),
                Gym("gym3", "Академия", 58.015032, 56.238319, 100f),
                Gym("gym4", "Мастерград", 58.038260, 56.119567, 100f),
                Gym("gym5", "Гимназия №7", 58.056592, 56.350562, 100f),
                Gym("gym6", "ДК им. Гагарина", 57.977132, 56.185568, 100f),
                Gym("gym7", "Фитнес-Центр «Друзья»", 57.997880, 56.136063, 100f),
                Gym("gym8", "МАОУ СОШ №42", 57.992840, 56.245049, 100f),
                Gym("gym9", "Лицей «Дельта»", 58.009304, 56.301405, 100f),
                Gym("gym10", "Гимназия №31", 57.995719, 56.160403, 100f),
                Gym("gym11", "МАОУ СОШ №55", 58.045974, 56.104141, 100f),
                Gym("gym12", "Школа «Приоритет» к.2", 57.995671, 56.223011, 100f),
                Gym("gym13", "Школа дизайна «Точка»", 57.992256, 56.264222, 100f),
                Gym("gym14", "МАОУ СОШ «Флагман»", 57.997669, 56.211596, 100f),
                Gym("gym15", "Лицей № 10", 58.011366, 56.333567, 100f),
                Gym("gym16", "МАОУ СОШ №93", 57.997639, 56.258408, 100f),
                Gym("gym17", "Култаевская средняя школа", 57.896803, 55.935493, 100f),
                Gym("gym18", "Гимназия №33, к.2", 58.003997, 56.269829, 100f),
                Gym("gym19", "«Точка» на Крупской, 66", 58.001116, 56.290765, 100f),
                Gym("gym20", "Кондратовская школа «Сфера»", 57.986221, 56.124736, 100f)
            )
            dojoDao.insertGyms(mockGyms)
        }
    }

    suspend fun updateSetup(schedules: List<DailySchedule>) {
        val current = dojoDao.getUserSettingsSync()
        val newSettings = current?.copy(
            scheduleCsv = ScheduleParser.serialize(schedules)
        ) ?: UserSettings(
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
            ).fallbackToDestructiveMigration().build()
            INSTANCE = instance
            instance
        }
    }
}
