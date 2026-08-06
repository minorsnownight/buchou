package com.buchou.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.ZoneId

@Database(
    entities = [
        ProfileEntity::class,
        QuitJourneyEntity::class,
        DailyCheckInEntity::class,
        SmokingEventEntity::class,
        QuitReasonEntity::class,
        AchievementUnlockEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class BuchouDatabase : RoomDatabase() {
    abstract fun dao(): BuchouDao

    companion object {
        fun create(context: Context): BuchouDatabase = Room.databaseBuilder(
            context.applicationContext,
            BuchouDatabase::class.java,
            "buchou.db",
        )
            .addMigrations(migration1To2())
            .build()

        private fun migration1To2(): Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val zoneId = ZoneId.systemDefault().id.replace("'", "''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quit_journey` (
                        `id` INTEGER NOT NULL,
                        `startedAtEpochMillis` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `quit_journey` (`id`, `startedAtEpochMillis`, `createdAtEpochMillis`)
                    SELECT 1, MIN(`startedAtEpochMillis`), MIN(`startedAtEpochMillis`)
                    FROM `quit_attempt`
                    HAVING COUNT(*) > 0
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `quit_attempt`")

                db.execSQL(
                    """
                    CREATE TABLE `daily_check_in_new` (
                        `id` TEXT NOT NULL,
                        `localDate` TEXT NOT NULL,
                        `zoneId` TEXT NOT NULL,
                        `checkedAtEpochMillis` INTEGER NOT NULL,
                        `cravingIntensity` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `daily_check_in_new`
                        (`id`, `localDate`, `zoneId`, `checkedAtEpochMillis`, `cravingIntensity`)
                    SELECT `id`, `localDate`, `zoneId`, `checkedAtEpochMillis`, `cravingIntensity`
                    FROM `daily_check_in`
                    WHERE `status` = 'SMOKE_FREE'
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `daily_check_in`")
                db.execSQL("ALTER TABLE `daily_check_in_new` RENAME TO `daily_check_in`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_check_in_localDate` " +
                        "ON `daily_check_in` (`localDate`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE `smoking_event_new` (
                        `id` TEXT NOT NULL,
                        `occurredAtEpochMillis` INTEGER NOT NULL,
                        `localDate` TEXT NOT NULL,
                        `zoneId` TEXT NOT NULL,
                        `cigaretteCount` INTEGER NOT NULL,
                        `cravingIntensity` INTEGER,
                        `trigger` TEXT,
                        `note` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `smoking_event_new`
                        (`id`, `occurredAtEpochMillis`, `localDate`, `zoneId`, `cigaretteCount`,
                         `cravingIntensity`, `trigger`, `note`)
                    SELECT `id`, `occurredAtEpochMillis`,
                        strftime('%Y-%m-%d', `occurredAtEpochMillis` / 1000, 'unixepoch', 'localtime'),
                        '$zoneId', `cigaretteCount`, `cravingIntensity`, `trigger`, `note`
                    FROM `smoking_event`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `smoking_event`")
                db.execSQL("ALTER TABLE `smoking_event_new` RENAME TO `smoking_event`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_smoking_event_localDate` " +
                        "ON `smoking_event` (`localDate`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `achievement_unlock` (
                        `achievementId` TEXT NOT NULL,
                        `unlockedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`achievementId`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
