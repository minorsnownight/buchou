package com.buchou.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BuchouDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("SELECT * FROM quit_journey WHERE id = 1")
    fun observeJourney(): Flow<QuitJourneyEntity?>

    @Query("SELECT * FROM quit_journey WHERE id = 1")
    suspend fun getJourney(): QuitJourneyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: QuitJourneyEntity)

    @Query("SELECT * FROM daily_check_in ORDER BY localDate DESC")
    fun observeCheckIns(): Flow<List<DailyCheckInEntity>>

    @Query("SELECT * FROM daily_check_in WHERE localDate = :localDate LIMIT 1")
    suspend fun getCheckIn(localDate: String): DailyCheckInEntity?

    @Query("SELECT * FROM daily_check_in ORDER BY localDate DESC")
    suspend fun getAllCheckIns(): List<DailyCheckInEntity>

    @Upsert
    suspend fun upsertCheckIn(checkIn: DailyCheckInEntity)

    @Query("SELECT * FROM smoking_event ORDER BY occurredAtEpochMillis DESC")
    fun observeSmokingEvents(): Flow<List<SmokingEventEntity>>

    @Query("SELECT * FROM smoking_event ORDER BY occurredAtEpochMillis DESC")
    suspend fun getSmokingEvents(): List<SmokingEventEntity>

    @Query("SELECT COUNT(*) FROM smoking_event WHERE localDate = :localDate")
    suspend fun countSmokingEvents(localDate: String): Int

    @Insert
    suspend fun insertSmokingEvent(event: SmokingEventEntity)

    @Query("SELECT * FROM quit_reason ORDER BY sortOrder, createdAtEpochMillis")
    fun observeReasons(): Flow<List<QuitReasonEntity>>

    @Query("SELECT * FROM quit_reason ORDER BY sortOrder, createdAtEpochMillis")
    suspend fun getReasons(): List<QuitReasonEntity>

    @Insert
    suspend fun insertReasons(reasons: List<QuitReasonEntity>)

    @Query("SELECT * FROM achievement_unlock ORDER BY unlockedAtEpochMillis")
    fun observeAchievementUnlocks(): Flow<List<AchievementUnlockEntity>>

    @Query("SELECT * FROM achievement_unlock")
    suspend fun getAchievementUnlocks(): List<AchievementUnlockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievementUnlocks(unlocks: List<AchievementUnlockEntity>)

    @Query("DELETE FROM profile")
    suspend fun deleteProfile()

    @Query("DELETE FROM quit_journey")
    suspend fun deleteJourney()

    @Query("DELETE FROM daily_check_in")
    suspend fun deleteCheckIns()

    @Query("DELETE FROM smoking_event")
    suspend fun deleteSmokingEvents()

    @Query("DELETE FROM quit_reason")
    suspend fun deleteReasons()

    @Query("DELETE FROM achievement_unlock")
    suspend fun deleteAchievementUnlocks()
}
