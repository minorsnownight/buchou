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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuitCalculationsTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val journey = QuitJourney(Instant.parse("2026-08-01T00:00:00Z"))

    @Test
    fun `missing check-ins do not interrupt current smoke-free duration`() {
        val stats = QuitCalculations.journeyStats(
            journey = journey,
            profile = SmokingProfile(cigarettesPerDay = 20),
            smokingEvents = emptyList(),
            now = Instant.parse("2026-08-04T00:00:00Z"),
        )

        assertEquals(Duration.ofDays(3), stats.currentSmokeFreeDuration)
    }

    @Test
    fun `latest smoking event resets current duration without resetting cumulative estimate`() {
        val events = listOf(smokingEvent("2026-08-03T12:00:00Z", 2))
        val stats = QuitCalculations.journeyStats(
            journey = journey,
            profile = SmokingProfile(cigarettesPerDay = 20),
            smokingEvents = events,
            now = Instant.parse("2026-08-04T00:00:00Z"),
        )

        assertEquals(Duration.ofHours(12), stats.currentSmokeFreeDuration)
        assertEquals(60.0, stats.expectedCigarettes, 0.0001)
        assertEquals(2, stats.actualCigarettes)
        assertEquals(58.0, stats.avoidedCigarettes, 0.0001)
    }

    @Test
    fun `saved money uses cumulative avoided cigarettes`() {
        val stats = QuitCalculations.journeyStats(
            journey = journey,
            profile = SmokingProfile(20, cigarettesPerPack = 20, pricePerPack = 30.0),
            smokingEvents = listOf(smokingEvent("2026-08-02T00:00:00Z", 4)),
            now = Instant.parse("2026-08-04T00:00:00Z"),
        )

        assertEquals(56.0, stats.avoidedCigarettes, 0.0001)
        assertEquals(84.0, stats.savedMoney!!, 0.0001)
    }

    @Test
    fun `saved money is unavailable when pack baseline is incomplete`() {
        val stats = QuitCalculations.journeyStats(
            journey = journey,
            profile = SmokingProfile(cigarettesPerDay = 20),
            smokingEvents = emptyList(),
            now = Instant.parse("2026-08-02T00:00:00Z"),
        )

        assertNull(stats.savedMoney)
    }

    @Test
    fun `any smoking event overrides a smoke-free check-in for that date`() {
        val date = LocalDate.of(2026, 8, 3)
        val summary = QuitCalculations.dailySummary(
            localDate = date,
            checkIns = listOf(checkIn(date, craving = 1)),
            smokingEvents = listOf(smokingEvent("2026-08-03T12:00:00Z", 2, craving = 4)),
        )

        assertEquals(DailyStatus.SMOKED, summary.status)
        assertEquals(2, summary.cigaretteCount)
        assertEquals(4, summary.cravingIntensity)
    }

    @Test
    fun `multiple smoking events accumulate count and use highest craving`() {
        val date = LocalDate.of(2026, 8, 3)
        val summary = QuitCalculations.dailySummary(
            localDate = date,
            checkIns = emptyList(),
            smokingEvents = listOf(
                smokingEvent("2026-08-03T03:00:00Z", 1, craving = 2),
                smokingEvent("2026-08-03T12:00:00Z", 3, craving = 5),
            ),
        )

        assertEquals(DailyStatus.SMOKED, summary.status)
        assertEquals(4, summary.cigaretteCount)
        assertEquals(5, summary.cravingIntensity)
    }

    @Test
    fun `unrecorded day is distinct from a zero-cigarette day`() {
        val smokeFreeDate = LocalDate.of(2026, 8, 2)
        val unrecordedDate = LocalDate.of(2026, 8, 3)

        assertEquals(
            0,
            QuitCalculations.dailySummary(smokeFreeDate, listOf(checkIn(smokeFreeDate)), emptyList())
                .cigaretteCount,
        )
        assertNull(
            QuitCalculations.dailySummary(unrecordedDate, emptyList(), emptyList()).cigaretteCount,
        )
    }

    @Test
    fun `smoke-free rate excludes unrecorded days and completeness includes them`() {
        val checkIns = listOf(
            checkIn(LocalDate.of(2026, 8, 1)),
            checkIn(LocalDate.of(2026, 8, 2)),
        )
        val events = listOf(smokingEvent("2026-08-03T12:00:00Z", 1))
        val stats = QuitCalculations.recordStats(
            journey = journey,
            checkIns = checkIns,
            smokingEvents = events,
            rangeStart = LocalDate.of(2026, 8, 1),
            rangeEnd = LocalDate.of(2026, 8, 4),
            today = LocalDate.of(2026, 8, 4),
            now = Instant.parse("2026-08-04T12:00:00Z"),
            zoneId = zone,
        )

        assertEquals(2, stats.smokeFreeDays)
        assertEquals(1, stats.smokedDays)
        assertEquals(1, stats.unrecordedDays)
        assertEquals(2.0 / 3.0, stats.smokeFreeRate!!, 0.0001)
        assertEquals(0.75, stats.recordCompleteness, 0.0001)
    }

    @Test
    fun `longest duration compares completed and current intervals`() {
        val events = listOf(
            smokingEvent("2026-08-02T00:00:00Z", 1),
            smokingEvent("2026-08-02T12:00:00Z", 1),
        )

        assertEquals(
            Duration.ofHours(36),
            QuitCalculations.longestSmokeFreeDuration(
                journey,
                events,
                Instant.parse("2026-08-04T00:00:00Z"),
            ),
        )
    }

    @Test
    fun `only new achievements are returned and previous unlocks stay unlocked`() {
        val reached = QuitCalculations.newlyReachedAchievements(
            currentDuration = Duration.ofDays(8),
            alreadyUnlocked = setOf(AchievementId.ONE_DAY),
        )

        assertEquals(
            setOf(AchievementId.THREE_DAYS, AchievementId.SEVEN_DAYS),
            reached,
        )
    }

    private fun checkIn(date: LocalDate, craving: Int? = null) = DailyCheckIn(
        id = date.toString(),
        localDate = date,
        zoneId = zone,
        checkedAt = date.atStartOfDay(zone).toInstant(),
        cravingIntensity = craving,
    )

    private fun smokingEvent(
        instant: String,
        count: Int,
        craving: Int? = null,
    ): SmokingEvent {
        val occurredAt = Instant.parse(instant)
        return SmokingEvent(
            id = instant,
            occurredAt = occurredAt,
            localDate = occurredAt.atZone(zone).toLocalDate(),
            zoneId = zone,
            cigaretteCount = count,
            cravingIntensity = craving,
        )
    }
}
