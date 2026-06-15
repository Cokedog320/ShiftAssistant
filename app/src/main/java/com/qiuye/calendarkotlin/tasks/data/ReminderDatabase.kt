package com.qiuye.calendarkotlin.tasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qiuye.calendarkotlin.diary.data.DiaryDao
import com.qiuye.calendarkotlin.diary.data.DiaryEntity

@Database(
    entities = [ReminderEntity::class, DiaryEntity::class],
    version = 4,
    exportSchema = true
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var instance: ReminderDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE reminders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        note TEXT NOT NULL,
                        scheduledAtMillis INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO reminders_new (
                        id,
                        title,
                        note,
                        scheduledAtMillis,
                        isCompleted,
                        createdAtMillis,
                        updatedAtMillis
                    )
                    SELECT
                        id,
                        COALESCE(title, ''),
                        COALESCE(note, ''),
                        scheduledAtMillis,
                        isCompleted,
                        createdAtMillis,
                        updatedAtMillis
                    FROM reminders
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE reminders")
                db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS diary_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dateKey TEXT NOT NULL,
                        content TEXT NOT NULL,
                        mood TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_diary_entries_dateKey ON diary_entries (dateKey)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        fun getInstance(context: Context): ReminderDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminders.db"
                )
                .addMigrations(*ALL_MIGRATIONS)
                .build().also { instance = it }
            }
        }
    }
}


