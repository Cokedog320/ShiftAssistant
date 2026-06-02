package com.qiuye.calendarkotlin.tasks.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.qiuye.calendarkotlin.BaseUnitTest
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import java.io.IOException

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
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndexOrThrow("title")) == "Test V1")
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
        assert(!cursor.moveToFirst())
        cursor.close()
    }
}
