package org.volkov.tono.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(entities = [Task::class, TaskHistory::class], version = 3, exportSchema = false)
abstract class TonoDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        /** v2 adds the months view; every pre-existing row is a weeks-view task. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN bucket TEXT NOT NULL DEFAULT '$BUCKET_WEEK'"
                )
            }
        }

        /**
         * v3 adds task age tracking. Existing rows have no recorded start, so they begin their
         * clock on the day of the upgrade rather than reporting an age nobody can verify.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN createdAt INTEGER NOT NULL " +
                        "DEFAULT ${LocalDate.now().toEpochDay()}"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_history` (" +
                        "`normalized` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                        "`firstSeen` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`normalized`))"
                )
            }
        }

        @Volatile
        private var instance: TonoDatabase? = null

        fun getInstance(context: Context): TonoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TonoDatabase::class.java,
                    "tono.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
