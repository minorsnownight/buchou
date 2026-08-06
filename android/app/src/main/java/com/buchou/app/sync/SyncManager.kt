package com.buchou.app.sync

import android.content.Context
import com.buchou.app.data.local.AchievementUnlockEntity
import com.buchou.app.data.local.BuchouDatabase
import com.buchou.app.data.local.DailyCheckInEntity
import com.buchou.app.data.local.ProfileEntity
import com.buchou.app.data.local.QuitJourneyEntity
import com.buchou.app.data.local.QuitReasonEntity
import androidx.room.withTransaction
import com.buchou.app.data.local.SmokingEventEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(
    private val database: BuchouDatabase,
    private val syncPreferences: SyncPreferences,
) {
    private val remotePath = "buchou/backup.json"

    suspend fun upload(): Result<Long> = withContext(Dispatchers.IO) {
        val config = syncPreferences.state.value.config
        if (config.url.isBlank()) return@withContext Result.failure(IllegalStateException("No WebDAV URL configured"))

        syncPreferences.setSyncing(true)
        return@withContext try {
            val client = WebDavClient(config)
            try { client.mkcol("buchou/") } catch (_: Exception) {}
            val json = exportToJson()
            val success = client.put(remotePath, json.toByteArray(Charsets.UTF_8))
            if (success) {
                val now = System.currentTimeMillis()
                syncPreferences.setSyncResult(now, null)
                Result.success(now)
            } else {
                val msg = "Upload failed (HTTP ${client.lastResponseCode})"
                syncPreferences.setSyncResult(null, msg)
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            syncPreferences.setSyncResult(null, e.message ?: e.javaClass.simpleName)
            Result.failure(e)
        }
    }

    suspend fun download(): Result<Long> = withContext(Dispatchers.IO) {
        val config = syncPreferences.state.value.config
        if (config.url.isBlank()) return@withContext Result.failure(IllegalStateException("No WebDAV URL configured"))

        syncPreferences.setSyncing(true)
        return@withContext try {
            val client = WebDavClient(config)
            val data = client.get(remotePath)
            if (data == null) {
                syncPreferences.setSyncResult(null, "No backup found (HTTP ${client.lastResponseCode})")
                return@withContext Result.failure(IllegalStateException("No backup found"))
            }
            val json = String(data, Charsets.UTF_8)
            importFromJson(json)
            val now = System.currentTimeMillis()
            syncPreferences.setSyncResult(now, null)
            Result.success(now)
        } catch (e: Exception) {
            syncPreferences.setSyncResult(null, e.message ?: e.javaClass.simpleName)
            Result.failure(e)
        }
    }

    private suspend fun exportToJson(): String {
        val dao = database.dao()
        val json = JSONObject()
        json.put("version", 2)

        dao.getProfile()?.let { p ->
            json.put("profile", JSONObject().apply {
                put("cigarettesPerDay", p.cigarettesPerDay)
                put("cigarettesPerPack", p.cigarettesPerPack)
                put("pricePerPack", p.pricePerPack)
                put("currencyCode", p.currencyCode)
                put("createdAtEpochMillis", p.createdAtEpochMillis)
            })
        }

        dao.getJourney()?.let { j ->
            json.put("journey", JSONObject().apply {
                put("startedAtEpochMillis", j.startedAtEpochMillis)
                put("createdAtEpochMillis", j.createdAtEpochMillis)
            })
        }

        val checkIns = dao.getAllCheckIns()
        json.put("checkIns", JSONArray().apply {
            checkIns.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("localDate", c.localDate)
                    put("zoneId", c.zoneId)
                    put("checkedAtEpochMillis", c.checkedAtEpochMillis)
                    put("cravingIntensity", c.cravingIntensity)
                })
            }
        })

        val events = dao.getSmokingEvents()
        json.put("smokingEvents", JSONArray().apply {
            events.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("occurredAtEpochMillis", e.occurredAtEpochMillis)
                    put("localDate", e.localDate)
                    put("zoneId", e.zoneId)
                    put("cigaretteCount", e.cigaretteCount)
                    put("cravingIntensity", e.cravingIntensity)
                    put("trigger", e.trigger)
                    put("note", e.note)
                })
            }
        })

        val reasons = dao.getReasons()
        json.put("reasons", JSONArray().apply {
            reasons.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("content", r.content)
                    put("sortOrder", r.sortOrder)
                    put("createdAtEpochMillis", r.createdAtEpochMillis)
                })
            }
        })

        val unlocks = dao.getAchievementUnlocks()
        json.put("achievementUnlocks", JSONArray().apply {
            unlocks.forEach { a ->
                put(JSONObject().apply {
                    put("achievementId", a.achievementId)
                    put("unlockedAtEpochMillis", a.unlockedAtEpochMillis)
                })
            }
        })

        return json.toString(2)
    }

    private suspend fun importFromJson(jsonStr: String) {
        val json = JSONObject(jsonStr)
        val dao = database.dao()

        database.withTransaction {
            // Clear existing data
            dao.deleteProfile()
            dao.deleteJourney()
            dao.deleteCheckIns()
            dao.deleteSmokingEvents()
            dao.deleteReasons()
            dao.deleteAchievementUnlocks()

            json.optJSONObject("profile")?.let { p ->
                dao.insertProfile(
                    ProfileEntity(
                        cigarettesPerDay = p.getInt("cigarettesPerDay"),
                        cigarettesPerPack = if (p.has("cigarettesPerPack") && !p.isNull("cigarettesPerPack")) p.getInt("cigarettesPerPack") else null,
                        pricePerPack = if (p.has("pricePerPack") && !p.isNull("pricePerPack")) p.getDouble("pricePerPack") else null,
                        currencyCode = p.optString("currencyCode", "CNY"),
                        createdAtEpochMillis = p.getLong("createdAtEpochMillis"),
                    ),
                )
            }

            json.optJSONObject("journey")?.let { j ->
                dao.insertJourney(
                    QuitJourneyEntity(
                        startedAtEpochMillis = j.getLong("startedAtEpochMillis"),
                        createdAtEpochMillis = j.getLong("createdAtEpochMillis"),
                    ),
                )
            }

            json.optJSONArray("checkIns")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    dao.upsertCheckIn(
                        DailyCheckInEntity(
                            id = c.getString("id"),
                            localDate = c.getString("localDate"),
                            zoneId = c.getString("zoneId"),
                            checkedAtEpochMillis = c.getLong("checkedAtEpochMillis"),
                            cravingIntensity = if (c.has("cravingIntensity") && !c.isNull("cravingIntensity")) c.getInt("cravingIntensity") else null,
                        ),
                    )
                }
            }

            json.optJSONArray("smokingEvents")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    dao.insertSmokingEvent(
                        SmokingEventEntity(
                            id = e.getString("id"),
                            occurredAtEpochMillis = e.getLong("occurredAtEpochMillis"),
                            localDate = e.getString("localDate"),
                            zoneId = e.getString("zoneId"),
                            cigaretteCount = e.getInt("cigaretteCount"),
                            cravingIntensity = if (e.has("cravingIntensity") && !e.isNull("cravingIntensity")) e.getInt("cravingIntensity") else null,
                            trigger = if (e.has("trigger") && !e.isNull("trigger")) e.getString("trigger") else null,
                            note = if (e.has("note") && !e.isNull("note")) e.getString("note") else null,
                        ),
                    )
                }
            }

            json.optJSONArray("reasons")?.let { arr ->
                val reasonEntities = (0 until arr.length()).map { i ->
                    val r = arr.getJSONObject(i)
                    QuitReasonEntity(
                        id = r.getString("id"),
                        content = r.getString("content"),
                        sortOrder = r.getInt("sortOrder"),
                        createdAtEpochMillis = r.getLong("createdAtEpochMillis"),
                    )
                }
                if (reasonEntities.isNotEmpty()) dao.insertReasons(reasonEntities)
            }

            json.optJSONArray("achievementUnlocks")?.let { arr ->
                val unlockEntities = (0 until arr.length()).map { i ->
                    val a = arr.getJSONObject(i)
                    AchievementUnlockEntity(
                        achievementId = a.getString("achievementId"),
                        unlockedAtEpochMillis = a.getLong("unlockedAtEpochMillis"),
                    )
                }
                if (unlockEntities.isNotEmpty()) dao.insertAchievementUnlocks(unlockEntities)
            }
        }
    }
}
