package com.qiuye.calendarkotlin.tasks.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.qiuye.calendarkotlin.BaseUnitTest
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class RoomMigrationTest : BaseUnitTest() {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ReminderDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1)

        db.execSQL(
            """
            INSERT INTO reminders (id, title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis)
            VALUES (1, 'Test V1', 'Note V1', 1000, 0, 100, 100)
            """
        )
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, ReminderDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT * FROM reminders WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Test V1", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        var db = helper.createDatabase(TEST_DB, 2)
        
        db.execSQL(
            """
            INSERT INTO reminders (id, title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis)
            VALUES (1, 'Test V2', 'Note V2', 1000, 0, 100, 100)
            """
        )
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, ReminderDatabase.MIGRATION_2_3)

        val cursor = db.query("SELECT * FROM diary_entries")
        assertFalse(cursor.moveToFirst())
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(TEST_DB, 3)

        db.execSQL(
            """
            INSERT INTO reminders (id, title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis)
            VALUES (1, 'Test V3', 'Note V3', 1000, 0, 100, 100)
            """
        )
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, ReminderDatabase.MIGRATION_3_4)

        val cursor = db.query("SELECT * FROM reminders WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("default", cursor.getString(cursor.getColumnIndexOrThrow("profileId")))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        var db = helper.createDatabase(TEST_DB, 4)

        db.execSQL(
            """
            INSERT INTO reminders (id, title, note, scheduledAtMillis, isCompleted, createdAtMillis, updatedAtMillis, profileId)
            VALUES (1, 'Test V4', 'Note V4', 1000, 0, 100, 100, 'default')
            """
        )
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, ReminderDatabase.MIGRATION_4_5)

        val cursor = db.query("SELECT * FROM reminders WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isArchived")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("archivedAtMillis")))
        cursor.close()
    }
}
