package com.buchou.app.domain

import com.buchou.app.domain.model.AchievementId
import com.buchou.app.domain.model.DailyCheckIn
import com.buchou.app.domain.model.DailyStatus
import com.buchou.app.domain.model.QuitJourney
import com.buchou.app.domain.model.SmokingEvent
import com.buchou.app.domain.model.SmokingProfile
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

data class JourneyStats(
    val currentSmokeFreeDuration: Duration,
    val expectedCigarettes: Double,
    val actualCigarettes: Int,
    val avoidedCigarettes: Double,
    val savedMoney: Double?,
)

data class DailySummary(
    val localDate: LocalDate,
    val status: DailyStatus,
    val cigaretteCount: Int?,
    val cravingIntensity: Int?,
)

data class RecordStats(
    val smokeFreeDays: Int,
    val smokedDays: Int,
    val unrecordedDays: Int,
    val smokeFreeRate: Double?,
    val recordCompleteness: Double,
    val longestSmokeFreeDuration: Duration,
)

object QuitCalculations {
    fun journeyStats(
        journey: QuitJourney,
        profile: SmokingProfile,
        smokingEvents: List<SmokingEvent>,
        now: Instant,
    ): JourneyStats {
        val elapsed = durationBetween(journey.startedAt, now)
        val relevantEvents = smokingEvents.filter {
            !it.occurredAt.isBefore(journey.startedAt) && !it.occurredAt.isAfter(now)
        }
        val latestSmokingAt = relevantEvents.maxOfOrNull(SmokingEvent::occurredAt)
        val currentDuration = durationBetween(latestSmokingAt ?: journey.startedAt, now)
        val expected = profile.cigarettesPerDay * elapsed.toMillis().toDouble() /
            Duration.ofDays(1).toMillis()
        val actual = relevantEvents.sumOf(SmokingEvent::cigaretteCount)
        val avoided = max(expected - actual, 0.0)
        val saved = if (profile.cigarettesPerPack != null && profile.pricePerPack != null) {
            avoided / profile.cigarettesPerPack * profile.pricePerPack
        } else {
            null
        }

        return JourneyStats(
            currentSmokeFreeDuration = currentDuration,
            expectedCigarettes = max(expected, 0.0),
            actualCigarettes = actual,
            avoidedCigarettes = avoided,
            savedMoney = saved?.let { max(it, 0.0) },
        )
    }

    fun dailySummary(
        localDate: LocalDate,
        checkIns: List<DailyCheckIn>,
        smokingEvents: List<SmokingEvent>,
    ): DailySummary {
        val dailyEvents = smokingEvents.filter { it.localDate == localDate }
        val checkIn = checkIns.lastOrNull { it.localDate == localDate }
        val status = when {
            dailyEvents.isNotEmpty() -> DailyStatus.SMOKED
            checkIn != null -> DailyStatus.SMOKE_FREE
            else -> DailyStatus.UNRECORDED
        }
        val craving = if (dailyEvents.isNotEmpty()) {
            dailyEvents.mapNotNull(SmokingEvent::cravingIntensity).maxOrNull()
        } else {
            checkIn?.cravingIntensity
        }

        return DailySummary(
            localDate = localDate,
            status = status,
            cigaretteCount = when (status) {
                DailyStatus.SMOKED -> dailyEvents.sumOf(SmokingEvent::cigaretteCount)
                DailyStatus.SMOKE_FREE -> 0
                DailyStatus.UNRECORDED -> null
            },
            cravingIntensity = craving,
        )
    }

    fun recordStats(
        journey: QuitJourney,
        checkIns: List<DailyCheckIn>,
        smokingEvents: List<SmokingEvent>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        now: Instant,
        zoneId: ZoneId,
    ): RecordStats {
        val journeyStartDate = journey.startedAt.atZone(zoneId).toLocalDate()
        val effectiveStart = maxOf(rangeStart, journeyStartDate)
        val effectiveEnd = minOf(rangeEnd, today)
        val dates = if (effectiveEnd.isBefore(effectiveStart)) {
            emptyList()
        } else {
            generateSequence(effectiveStart) { current ->
                current.plusDays(1).takeUnless { it.isAfter(effectiveEnd) }
            }.toList()
        }
        val summaries = dates.map { dailySummary(it, checkIns, smokingEvents) }
        val smokeFreeDays = summaries.count { it.status == DailyStatus.SMOKE_FREE }
        val smokedDays = summaries.count { it.status == DailyStatus.SMOKED }
        val unrecordedDays = summaries.count { it.status == DailyStatus.UNRECORDED }
        val recordedDays = smokeFreeDays + smokedDays

        return RecordStats(
            smokeFreeDays = smokeFreeDays,
            smokedDays = smokedDays,
            unrecordedDays = unrecordedDays,
            smokeFreeRate = if (recordedDays == 0) null else smokeFreeDays.toDouble() / recordedDays,
            recordCompleteness = if (dates.isEmpty()) 0.0 else recordedDays.toDouble() / dates.size,
            longestSmokeFreeDuration = longestSmokeFreeDuration(journey, smokingEvents, now),
        )
    }

    fun longestSmokeFreeDuration(
        journey: QuitJourney,
        smokingEvents: List<SmokingEvent>,
        now: Instant,
    ): Duration {
        val eventTimes = smokingEvents
            .map(SmokingEvent::occurredAt)
            .filter { !it.isBefore(journey.startedAt) && !it.isAfter(now) }
            .sorted()
        var intervalStart = journey.startedAt
        var longest = Duration.ZERO
        for (eventTime in eventTimes) {
            longest = maxOf(longest, durationBetween(intervalStart, eventTime))
            intervalStart = eventTime
        }
        return maxOf(longest, durationBetween(intervalStart, now))
    }

    fun newlyReachedAchievements(
        currentDuration: Duration,
        alreadyUnlocked: Set<AchievementId>,
    ): Set<AchievementId> = AchievementId.entries
        .filterTo(linkedSetOf()) {
            it !in alreadyUnlocked && currentDuration >= Duration.ofDays(it.requiredDays)
        }

    private fun durationBetween(start: Instant, end: Instant): Duration =
        if (end.isBefore(start)) Duration.ZERO else Duration.between(start, end)
}
