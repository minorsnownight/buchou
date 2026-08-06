package com.buchou.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class QuitJourney(
    val startedAt: Instant,
)

data class SmokingEvent(
    val id: String,
    val occurredAt: Instant,
    val localDate: LocalDate,
    val zoneId: ZoneId,
    val cigaretteCount: Int,
    val cravingIntensity: Int? = null,
    val trigger: String? = null,
    val note: String? = null,
) {
    init {
        require(cigaretteCount > 0) { "Cigarette count must be positive" }
        require(cravingIntensity == null || cravingIntensity in 0..5) {
            "Craving intensity must be between 0 and 5"
        }
    }
}

data class DailyCheckIn(
    val id: String,
    val localDate: LocalDate,
    val zoneId: ZoneId,
    val checkedAt: Instant,
    val cravingIntensity: Int? = null,
) {
    init {
        require(cravingIntensity == null || cravingIntensity in 0..5) {
            "Craving intensity must be between 0 and 5"
        }
    }
}

enum class DailyStatus {
    UNRECORDED,
    SMOKE_FREE,
    SMOKED,
}

data class SmokingProfile(
    val cigarettesPerDay: Int,
    val cigarettesPerPack: Int? = null,
    val pricePerPack: Double? = null,
) {
    init {
        require(cigarettesPerDay > 0) { "Daily cigarette count must be positive" }
        require(cigarettesPerPack == null || cigarettesPerPack > 0) {
            "Cigarettes per pack must be positive"
        }
        require(pricePerPack == null || pricePerPack >= 0.0) {
            "Price per pack cannot be negative"
        }
    }
}

enum class AchievementId(val requiredDays: Long) {
    ONE_DAY(1),
    THREE_DAYS(3),
    SEVEN_DAYS(7),
    FOURTEEN_DAYS(14),
    THIRTY_DAYS(30),
    NINETY_DAYS(90),
    ONE_HUNDRED_EIGHTY_DAYS(180),
    ONE_YEAR(365),
}
