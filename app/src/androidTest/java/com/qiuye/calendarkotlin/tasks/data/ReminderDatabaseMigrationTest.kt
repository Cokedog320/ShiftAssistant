package com.qiuye.calendarkotlin.tasks.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ReminderDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ReminderDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1)

        // db has schema version 1. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        db.execSQL(
            """
            INSERT INTO reminders (title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis) 
            VALUES ('Test Title', 'Test Note', 1000, 0, 500, 500)
            """.trimIndent()
        )

        // Nullable test for v1 (as title and note were String? or implicit nulls allowed)
        db.execSQL(
            """
            INSERT INTO reminders (title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis) 
            VALUES (null, null, 2000, 1, 600, 600)
            """.trimIndent()
        )

        // Prepare for the next version.
        db.close()

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, ReminderDatabase.MIGRATION_1_2)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        val cursor = db.query("SELECT * FROM reminders ORDER BY id ASC")
        
        assertEquals(2, cursor.count)
        
        cursor.moveToFirst()
        assertEquals("Test Title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("Test Note", cursor.getString(cursor.getColumnIndexOrThrow("note")))
        assertEquals(1000L, cursor.getLong(cursor.getColumnIndexOrThrow("scheduledAtMillis")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isCompleted")))

        cursor.moveToNext()
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("title"))) // COALESCE(title, '')
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("note"))) // COALESCE(note, '')
        assertEquals(2000L, cursor.getLong(cursor.getColumnIndexOrThrow("scheduledAtMillis")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isCompleted")))
        
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        var db = helper.createDatabase(TEST_DB, 2)

        // Insert reminder data in version 2
        db.execSQL(
            """
            INSERT INTO reminders (title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis)
            VALUES ('V2 Title', 'V2 Note', 1500, 0, 800, 800)
            """.trimIndent()
        )

        db.close()

        // Migrate to version 3
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, ReminderDatabase.MIGRATION_2_3)

        // Verify reminder data is preserved
        var cursor = db.query("SELECT * FROM reminders ORDER BY id ASC")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("V2 Title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("V2 Note", cursor.getString(cursor.getColumnIndexOrThrow("note")))
        assertEquals(1500L, cursor.getLong(cursor.getColumnIndexOrThrow("scheduledAtMillis")))
        cursor.close()

        // Verify diary_entries table exists and we can insert into it
        db.execSQL(
            """
            INSERT INTO diary_entries (dateKey, content, mood, createdAtMillis, updatedAtMillis)
            VALUES ('2026-05-21', 'Test diary content', '😊', 1000, 1000)
            """.trimIndent()
        )

        cursor = db.query("SELECT * FROM diary_entries")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("2026-05-21", cursor.getString(cursor.getColumnIndexOrThrow("dateKey")))
        assertEquals("Test diary content", cursor.getString(cursor.getColumnIndexOrThrow("content")))
        assertEquals("😊", cursor.getString(cursor.getColumnIndexOrThrow("mood")))
        cursor.close()

        // Verify unique constraint on dateKey works (inserting duplicate dateKey should fail)
        var threwUniqueConstraintException = false
        try {
            db.execSQL(
                """
                INSERT INTO diary_entries (dateKey, content, mood, createdAtMillis, updatedAtMillis)
                VALUES ('2026-05-21', 'Another content', '😐', 1001, 1001)
                """.trimIndent()
            )
        } catch (e: Exception) {
            threwUniqueConstraintException = true
        }
        org.junit.Assert.assertTrue("Should throw unique constraint exception for duplicate dateKey", threwUniqueConstraintException)

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To3() {
        var db = helper.createDatabase(TEST_DB, 1)

        // Insert reminder data in version 1
        db.execSQL(
            """
            INSERT INTO reminders (title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis)
            VALUES ('V1 Title', 'V1 Note', 1200, 0, 700, 700)
            """.trimIndent()
        )

        db.close()

        // Migrate from 1 to 3
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            ReminderDatabase.MIGRATION_1_2,
            ReminderDatabase.MIGRATION_2_3
        )

        // Verify reminder data is preserved
        var cursor = db.query("SELECT * FROM reminders ORDER BY id ASC")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("V1 Title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("V1 Note", cursor.getString(cursor.getColumnIndexOrThrow("note")))
        cursor.close()

        // Verify diary_entries table exists
        cursor = db.query("SELECT * FROM diary_entries")
        assertEquals(0, cursor.count)
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(TEST_DB, 3)

        // Insert reminder data in version 3
        db.execSQL(
            """
            INSERT INTO reminders (title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis)
            VALUES ('V3 Title', 'V3 Note', 1800, 0, 900, 900)
            """.trimIndent()
        )

        db.close()

        // Migrate to version 4
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, ReminderDatabase.MIGRATION_3_4)

        // Verify reminder data is preserved and has default profileId
        val cursor = db.query("SELECT * FROM reminders ORDER BY id ASC")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("V3 Title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("default", cursor.getString(cursor.getColumnIndexOrThrow("profileId")))
        cursor.close()

        db.close()
    }
}
