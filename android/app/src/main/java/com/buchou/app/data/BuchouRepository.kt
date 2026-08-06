package com.buchou.app.data

import androidx.room.withTransaction
import com.buchou.app.data.local.AchievementUnlockEntity
import com.buchou.app.data.local.BuchouDatabase
import com.buchou.app.data.local.DailyCheckInEntity
import com.buchou.app.data.local.ProfileEntity
import com.buchou.app.data.local.QuitJourneyEntity
import com.buchou.app.data.local.QuitReasonEntity
import com.buchou.app.data.local.SmokingEventEntity
import com.buchou.app.domain.QuitCalculations
import com.buchou.app.domain.JourneyStats
import com.buchou.app.domain.DailySummary
import com.buchou.app.domain.RecordStats
import com.buchou.app.domain.model.AchievementId
import com.buchou.app.domain.model.DailyCheckIn
import com.buchou.app.domain.model.DailyStatus
import com.buchou.app.domain.model.QuitJourney
import com.buchou.app.domain.model.SmokingEvent
import com.buchou.app.domain.model.SmokingProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BuchouData(
    val profile: ProfileEntity?,
    val journey: QuitJourneyEntity?,
    val checkIns: List<DailyCheckInEntity>,
    val smokingEvents: List<SmokingEventEntity>,
    val reasons: List<QuitReasonEntity>,
    val achievementUnlocks: List<AchievementUnlockEntity>,
) {
    fun hasRecordForDate(localDate: LocalDate): Boolean {
        val date = localDate.toString()
        return checkIns.any { it.localDate == date } || smokingEvents.any { it.localDate == date }
    }

    fun statusForDate(localDate: LocalDate): DailyStatus {
        val date = localDate.toString()
        return when {
            smokingEvents.any { it.localDate == date } -> DailyStatus.SMOKED
            checkIns.any { it.localDate == date } -> DailyStatus.SMOKE_FREE
            else -> DailyStatus.UNRECORDED
        }
    }

    fun currentStreakStartedAtEpochMillis(): Long? = smokingEvents
        .maxOfOrNull(SmokingEventEntity::occurredAtEpochMillis)
        ?: journey?.startedAtEpochMillis

    fun journeyStats(now: Instant): JourneyStats? {
        val profile = profile ?: return null
        val journey = journey ?: return null
        return QuitCalculations.journeyStats(
            journey = QuitJourney(Instant.ofEpochMilli(journey.startedAtEpochMillis)),
            profile = SmokingProfile(
                cigarettesPerDay = profile.cigarettesPerDay,
                cigarettesPerPack = profile.cigarettesPerPack,
                pricePerPack = profile.pricePerPack,
            ),
            smokingEvents = smokingEvents.map(SmokingEventEntity::toDomain),
            now = now,
        )
    }

    fun dailySummary(localDate: LocalDate): DailySummary = QuitCalculations.dailySummary(
        localDate = localDate,
        checkIns = checkIns.map(DailyCheckInEntity::toDomain),
        smokingEvents = smokingEvents.map(SmokingEventEntity::toDomain),
    )

    fun recordStats(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): RecordStats? {
        val journey = journey ?: return null
        return QuitCalculations.recordStats(
            journey = QuitJourney(Instant.ofEpochMilli(journey.startedAtEpochMillis)),
            checkIns = checkIns.map(DailyCheckInEntity::toDomain),
            smokingEvents = smokingEvents.map(SmokingEventEntity::toDomain),
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            today = today,
            now = now,
            zoneId = zoneId,
        )
    }
}

private data class CoreData(
    val profile: ProfileEntity?,
    val journey: QuitJourneyEntity?,
    val checkIns: List<DailyCheckInEntity>,
    val smokingEvents: List<SmokingEventEntity>,
)

class BuchouRepository(
    private val database: BuchouDatabase,
) {
    private val dao = database.dao()

    private val coreData = combine(
        dao.observeProfile(),
        dao.observeJourney(),
        dao.observeCheckIns(),
        dao.observeSmokingEvents(),
    ) { profile, journey, checkIns, smokingEvents ->
        CoreData(profile, journey, checkIns, smokingEvents)
    }

    val data: Flow<BuchouData> = combine(
        coreData,
        dao.observeReasons(),
        dao.observeAchievementUnlocks(),
    ) { core, reasons, achievementUnlocks ->
        BuchouData(
            profile = core.profile,
            journey = core.journey,
            checkIns = core.checkIns,
            smokingEvents = core.smokingEvents,
            reasons = reasons,
            achievementUnlocks = achievementUnlocks,
        )
    }

    suspend fun initialize(
        quitStartedAt: Instant,
        cigarettesPerDay: Int,
        cigarettesPerPack: Int?,
        pricePerPack: Double?,
        customReasons: List<String>,
        locale: Locale = Locale.getDefault(),
    ) {
        require(cigarettesPerDay > 0)
        require(cigarettesPerPack == null || cigarettesPerPack > 0)
        require(pricePerPack == null || pricePerPack >= 0.0)
        require(!quitStartedAt.isAfter(Instant.now()))
        val now = Instant.now()
        val currencyCode = runCatching {
            Currency.getInstance(locale).currencyCode
        }.getOrDefault("CNY")
        val reasons = buildInitialReasons(customReasons, locale, now)

        database.withTransaction {
            check(dao.getProfile() == null && dao.getJourney() == null) {
                "Quit journey is already initialized"
            }
            dao.insertProfile(
                ProfileEntity(
                    cigarettesPerDay = cigarettesPerDay,
                    cigarettesPerPack = cigarettesPerPack,
                    pricePerPack = pricePerPack,
                    currencyCode = currencyCode,
                    createdAtEpochMillis = now.toEpochMilli(),
                ),
            )
            dao.insertJourney(
                QuitJourneyEntity(
                    startedAtEpochMillis = quitStartedAt.toEpochMilli(),
                    createdAtEpochMillis = now.toEpochMilli(),
                ),
            )
            dao.insertReasons(reasons)
        }
    }

    suspend fun updateProfile(
        cigarettesPerDay: Int,
        cigarettesPerPack: Int?,
        pricePerPack: Double?,
    ) {
        require(cigarettesPerDay > 0)
        require(cigarettesPerPack == null || cigarettesPerPack > 0)
        require(pricePerPack == null || pricePerPack >= 0.0)
        val current = checkNotNull(dao.getProfile())
        dao.updateProfile(
            current.copy(
                cigarettesPerDay = cigarettesPerDay,
                cigarettesPerPack = cigarettesPerPack,
                pricePerPack = pricePerPack,
            ),
        )
    }

    suspend fun markSmokeFree(
        cravingIntensity: Int? = null,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        require(cravingIntensity == null || cravingIntensity in 0..5)
        val localDate = now.atZone(zoneId).toLocalDate().toString()
        val recorded = database.withTransaction {
            if (dao.countSmokingEvents(localDate) > 0) return@withTransaction false
            val existing = dao.getCheckIn(localDate)
            dao.upsertCheckIn(
                DailyCheckInEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    localDate = localDate,
                    zoneId = zoneId.id,
                    checkedAtEpochMillis = now.toEpochMilli(),
                    cravingIntensity = cravingIntensity ?: existing?.cravingIntensity,
                ),
            )
            true
        }
        if (recorded) refreshAchievements(now)
        return recorded
    }

    suspend fun recordSmoking(
        cigaretteCount: Int,
        cravingIntensity: Int? = null,
        occurredAt: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        trigger: String? = null,
        note: String? = null,
    ) {
        require(cigaretteCount > 0)
        require(cravingIntensity == null || cravingIntensity in 0..5)
        val journey = checkNotNull(dao.getJourney())
        require(occurredAt.toEpochMilli() >= journey.startedAtEpochMillis)
        refreshAchievements(occurredAt)
        dao.insertSmokingEvent(
            SmokingEventEntity(
                id = UUID.randomUUID().toString(),
                occurredAtEpochMillis = occurredAt.toEpochMilli(),
                localDate = occurredAt.atZone(zoneId).toLocalDate().toString(),
                zoneId = zoneId.id,
                cigaretteCount = cigaretteCount,
                cravingIntensity = cravingIntensity,
                trigger = trigger?.trim()?.takeIf(String::isNotEmpty),
                note = note?.trim()?.takeIf(String::isNotEmpty),
            ),
        )
    }

    suspend fun refreshAchievements(now: Instant = Instant.now()) {
        val journeyEntity = dao.getJourney() ?: return
        val profileEntity = dao.getProfile() ?: return
        val events = dao.getSmokingEvents().map(SmokingEventEntity::toDomain)
        val currentDuration = QuitCalculations.journeyStats(
            journey = QuitJourney(Instant.ofEpochMilli(journeyEntity.startedAtEpochMillis)),
            profile = SmokingProfile(
                cigarettesPerDay = profileEntity.cigarettesPerDay,
                cigarettesPerPack = profileEntity.cigarettesPerPack,
                pricePerPack = profileEntity.pricePerPack,
            ),
            smokingEvents = events,
            now = now,
        ).currentSmokeFreeDuration
        val unlocked = dao.getAchievementUnlocks()
            .mapNotNull { runCatching { AchievementId.valueOf(it.achievementId) }.getOrNull() }
            .toSet()
        val reached = QuitCalculations.newlyReachedAchievements(currentDuration, unlocked)
        if (reached.isNotEmpty()) {
            dao.insertAchievementUnlocks(
                reached.map { AchievementUnlockEntity(it.name, now.toEpochMilli()) },
            )
        }
    }

    suspend fun ensureFiveReasons(locale: Locale = Locale.getDefault()) {
        val current = dao.getReasons()
        if (current.size >= 5) return
        val completed = completeReasonContents(current.map(QuitReasonEntity::content), locale)
        val existingContents = current.mapTo(mutableSetOf()) { it.content.trim() }
        val now = Instant.now().toEpochMilli()
        val nextSortOrder = (current.maxOfOrNull(QuitReasonEntity::sortOrder) ?: -1) + 1
        val additions = completed
            .filterNot(existingContents::contains)
            .take(5 - current.size)
            .mapIndexed { index, content ->
                QuitReasonEntity(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    sortOrder = nextSortOrder + index,
                    createdAtEpochMillis = now,
                )
            }
        if (additions.isNotEmpty()) dao.insertReasons(additions)
    }

    suspend fun updateReasons(
        customReasons: List<String>,
        locale: Locale = Locale.getDefault(),
    ) {
        val entities = buildInitialReasons(customReasons, locale, Instant.now())
        database.withTransaction {
            dao.deleteReasons()
            dao.insertReasons(entities)
        }
    }

    suspend fun injectTestData() {
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val today = now.atZone(zoneId).toLocalDate()
        database.withTransaction {
            dao.deleteCheckIns()
            dao.deleteSmokingEvents()
            // Generate 30 days of data
            for (i in 29 downTo 0) {
                val date = today.minusDays(i.toLong())
                val dateStr = date.toString()
                val dayOfWeek = date.dayOfWeek.value
                // ~60% smoke-free, ~25% smoked, ~15% unrecorded
                val rand = (i * 7 + 13) % 100
                when {
                    i == 0 -> {
                        // Today: no record yet (for testing alarm behavior)
                    }
                    rand < 60 -> {
                        // Smoke-free check-in, some with craving
                        val craving = if (rand % 3 == 0) (rand % 4) else null
                        dao.upsertCheckIn(
                            DailyCheckInEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                localDate = dateStr,
                                zoneId = zoneId.id,
                                checkedAtEpochMillis = date.atStartOfDay(zoneId).plusHours(10).toInstant().toEpochMilli(),
                                cravingIntensity = craving,
                            ),
                        )
                    }
                    rand < 85 -> {
                        // Smoked 1-5 cigarettes
                        val count = ((rand % 5) + 1)
                        val craving = (rand % 6)
                        dao.insertSmokingEvent(
                            SmokingEventEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                occurredAtEpochMillis = date.atStartOfDay(zoneId).plusHours(14).toInstant().toEpochMilli(),
                                localDate = dateStr,
                                zoneId = zoneId.id,
                                cigaretteCount = count,
                                cravingIntensity = craving,
                                trigger = null,
                                note = null,
                            ),
                        )
                    }
                    // else: unrecorded (skip)
                }
            }
        }
        refreshAchievements(now)
    }

    suspend fun restartQuitJourney() {
        val now = Instant.now()
        database.withTransaction {
            dao.deleteCheckIns()
            dao.deleteSmokingEvents()
            dao.deleteJourney()
            dao.insertJourney(
                QuitJourneyEntity(
                    startedAtEpochMillis = now.toEpochMilli(),
                    createdAtEpochMillis = now.toEpochMilli(),
                ),
            )
        }
    }

    suspend fun resetAllData() {
        database.withTransaction {
            dao.deleteProfile()
            dao.deleteJourney()
            dao.deleteCheckIns()
            dao.deleteSmokingEvents()
            dao.deleteReasons()
            dao.deleteAchievementUnlocks()
        }
    }

    private fun buildInitialReasons(
        customReasons: List<String>,
        locale: Locale,
        now: Instant,
    ): List<QuitReasonEntity> {
        val contents = completeReasonContents(customReasons, locale)
        return contents.mapIndexed { index, content ->
            QuitReasonEntity(
                id = UUID.randomUUID().toString(),
                content = content,
                sortOrder = index,
                createdAtEpochMillis = now.toEpochMilli(),
            )
        }
    }

}

private object DefaultQuitReasons {
    private val chinese = listOf(
        "让呼吸更轻松",
        "让家人远离二手烟",
        "把钱花在真正值得的地方",
        "找回更好的精力和状态",
        "重新掌控自己的生活",
    )
    private val english = listOf(
        "Breathe more easily",
        "Keep my family away from secondhand smoke",
        "Spend money on what truly matters",
        "Get my energy and wellbeing back",
        "Take back control of my life",
    )

    fun forLocale(locale: Locale): List<String> = if (locale.language == "zh") chinese else english
}

internal fun initialReasonContents(customReason: String?, locale: Locale): List<String> =
    completeReasonContents(listOfNotNull(customReason), locale)

internal fun completeReasonContents(existing: List<String>, locale: Locale): List<String> =
    (existing.map(String::trim).filter(String::isNotEmpty) + DefaultQuitReasons.forLocale(locale))
        .distinct()
        .take(5)

private fun SmokingEventEntity.toDomain(): SmokingEvent = SmokingEvent(
    id = id,
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
    localDate = LocalDate.parse(localDate),
    zoneId = ZoneId.of(zoneId),
    cigaretteCount = cigaretteCount,
    cravingIntensity = cravingIntensity,
    trigger = trigger,
    note = note,
)

internal fun DailyCheckInEntity.toDomain(): DailyCheckIn = DailyCheckIn(
    id = id,
    localDate = LocalDate.parse(localDate),
    zoneId = ZoneId.of(zoneId),
    checkedAt = Instant.ofEpochMilli(checkedAtEpochMillis),
    cravingIntensity = cravingIntensity,
)
