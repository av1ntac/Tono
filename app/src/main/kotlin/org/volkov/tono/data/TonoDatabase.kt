package org.volkov.tono.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class], version = 2, exportSchema = false)
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

        @Volatile
        private var instance: TonoDatabase? = null

        fun getInstance(context: Context): TonoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TonoDatabase::class.java,
                    "tono.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
