package com.buchou.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val cigarettesPerDay: Int,
    val cigarettesPerPack: Int?,
    val pricePerPack: Double?,
    val currencyCode: String,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "quit_journey")
data class QuitJourneyEntity(
    @PrimaryKey val id: Int = 1,
    val startedAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "daily_check_in",
    indices = [Index(value = ["localDate"], unique = true)],
)
data class DailyCheckInEntity(
    @PrimaryKey val id: String,
    val localDate: String,
    val zoneId: String,
    val checkedAtEpochMillis: Long,
    val cravingIntensity: Int?,
)

@Entity(
    tableName = "smoking_event",
    indices = [Index(value = ["localDate"])],
)
data class SmokingEventEntity(
    @PrimaryKey val id: String,
    val occurredAtEpochMillis: Long,
    val localDate: String,
    val zoneId: String,
    val cigaretteCount: Int,
    val cravingIntensity: Int?,
    val trigger: String?,
    val note: String?,
)

@Entity(tableName = "quit_reason")
data class QuitReasonEntity(
    @PrimaryKey val id: String,
    val content: String,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "achievement_unlock")
data class AchievementUnlockEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAtEpochMillis: Long,
)
