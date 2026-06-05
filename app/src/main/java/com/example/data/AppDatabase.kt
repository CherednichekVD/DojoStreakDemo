package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import java.time.DayOfWeek

@Entity(tableName = "gyms")
data class Gym(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val selectedGymId: String?,
    // Store schedules as JSON string or a simple delimited string for MVP: "MONDAY,18:00,19:30;WEDNESDAY..."
    val scheduleCsv: String,
    val currentStreak: Int,
    val lastCheckInMillis: Long
)

@Dao
interface DojoDao {
    @Query("SELECT * FROM gyms")
    fun getAllGyms(): Flow<List<Gym>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGyms(gyms: List<Gym>)

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsSync(): UserSettings?

    @Query("SELECT COUNT(*) FROM gyms")
    suspend fun getGymCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettings)
    
    @Query("UPDATE user_settings SET currentStreak = :streak, lastCheckInMillis = :timestamp WHERE id = 1")
    suspend fun updateCheckIn(streak: Int, timestamp: Long)

    @Query("UPDATE user_settings SET currentStreak = 0 WHERE id = 1")
    suspend fun resetStreak()
}

@Database(entities = [Gym::class, UserSettings::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dojoDao(): DojoDao
}
