package com.lingion.sleepy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lingion.sleepy.data.dao.CqieUnscheduledDao
import com.lingion.sleepy.data.dao.CourseDao
import com.lingion.sleepy.data.dao.TimeTableDao
import com.lingion.sleepy.data.entity.CourseEntity
import com.lingion.sleepy.data.entity.CqieUnscheduledEntity
import com.lingion.sleepy.data.entity.TimeTableEntity

@Database(
    entities = [CourseEntity::class, TimeTableEntity::class, CqieUnscheduledEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun timeTableDao(): TimeTableDao
    abstract fun cqieUnscheduledDao(): CqieUnscheduledDao

    companion object {
        private const val DB_NAME = "sleepy.db"

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `cqie_unscheduled_courses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tableId` INTEGER NOT NULL,
                        `courseName` TEXT NOT NULL,
                        `courseCode` TEXT NOT NULL,
                        `teacher` TEXT NOT NULL,
                        `room` TEXT NOT NULL,
                        `weeksJson` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        FOREIGN KEY(`tableId`) REFERENCES `time_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cqie_unscheduled_courses_tableId` " +
                        "ON `cqie_unscheduled_courses` (`tableId`)"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
