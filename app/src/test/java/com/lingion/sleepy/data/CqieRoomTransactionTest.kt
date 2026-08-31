package com.lingion.sleepy.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.lingion.sleepy.SleepyApp
import com.lingion.sleepy.data.entity.CourseEntity
import com.lingion.sleepy.data.entity.CqieUnscheduledEntity
import com.lingion.sleepy.data.entity.TimeTableEntity
import com.lingion.sleepy.data.repository.ScheduleRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = SleepyApp::class)
class CqieRoomTransactionTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun persistenceFailureRollsBackTableScheduledAndUnscheduledSnapshot() = runBlocking {
        val tableId = db.timeTableDao().insert(table("旧课表", "2026-01-05"))
        db.courseDao().insertAll(listOf(course(tableId, "旧定时课")))
        db.cqieUnscheduledDao().insertAll(listOf(unscheduled(tableId, "旧整周项目")))
        db.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER force_cqie_failure BEFORE INSERT ON cqie_unscheduled_courses
               BEGIN SELECT RAISE(ABORT, 'forced persistence failure'); END"""
        )

        val error = runCatching {
            ScheduleRepository(db).replaceCqieSnapshot(
                targetTableId = tableId,
                tableTemplate = table("新模板", "2026-08-31"),
                courses = listOf(course(0, "新定时课")),
                unscheduled = listOf(unscheduled(0, "新整周项目")),
            )
        }.exceptionOrNull()

        assertTrue("forced insert must fail", error != null)
        assertEquals("2026-01-05", db.timeTableDao().getById(tableId)!!.startDate)
        assertEquals(listOf("旧定时课"), db.courseDao().getByTable(tableId).map { it.courseName })
        assertEquals(
            listOf("旧整周项目"),
            db.cqieUnscheduledDao().getByTable(tableId).map { it.courseName },
        )
    }

    @Test
    fun successfulRefreshReusesTargetAndReplacesBothCollections() = runBlocking {
        val tableId = db.timeTableDao().insert(table("用户保留的表名", "2026-01-05"))
        db.courseDao().insertAll(listOf(course(tableId, "旧定时课")))
        db.cqieUnscheduledDao().insertAll(listOf(unscheduled(tableId, "旧整周项目")))

        val resultId = ScheduleRepository(db).replaceCqieSnapshot(
            targetTableId = tableId,
            tableTemplate = table("不会覆盖用户表名", "2026-08-31"),
            courses = listOf(course(0, "新定时课")),
            unscheduled = listOf(unscheduled(0, "新整周项目")),
        )

        assertEquals(tableId, resultId)
        val updated = db.timeTableDao().getById(tableId)!!
        assertEquals("用户保留的表名", updated.name)
        assertEquals("2026-08-31", updated.startDate)
        assertEquals(listOf("新定时课"), db.courseDao().getByTable(tableId).map { it.courseName })
        assertEquals(
            listOf("新整周项目"),
            db.cqieUnscheduledDao().getByTable(tableId).map { it.courseName },
        )
    }

    @Test
    fun successfulFirstImportCreatesDefaultTargetWithBothCollections() = runBlocking {
        val resultId = ScheduleRepository(db).replaceCqieSnapshot(
            targetTableId = null,
            tableTemplate = table("Sleepy CQIE", "2026-08-31"),
            courses = listOf(course(0, "新定时课")),
            unscheduled = listOf(unscheduled(0, "新整周项目")),
        )

        assertTrue(resultId > 0)
        assertEquals(resultId, db.timeTableDao().getDefault()!!.id)
        assertEquals("Sleepy CQIE", db.timeTableDao().getById(resultId)!!.name)
        assertEquals(listOf("新定时课"), db.courseDao().getByTable(resultId).map { it.courseName })
        assertEquals(
            listOf("新整周项目"),
            db.cqieUnscheduledDao().getByTable(resultId).map { it.courseName },
        )
    }

    @Test
    fun zeroValidRowsAreRejectedBeforeOldSnapshotChanges() = runBlocking {
        val tableId = db.timeTableDao().insert(table("旧课表", "2026-01-05"))
        db.courseDao().insertAll(listOf(course(tableId, "旧定时课")))
        db.cqieUnscheduledDao().insertAll(listOf(unscheduled(tableId, "旧整周项目")))

        val error = runCatching {
            ScheduleRepository(db).replaceCqieSnapshot(
                targetTableId = tableId,
                tableTemplate = table("新模板", "2026-08-31"),
                courses = emptyList(),
                unscheduled = emptyList(),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("2026-01-05", db.timeTableDao().getById(tableId)!!.startDate)
        assertEquals(listOf("旧定时课"), db.courseDao().getByTable(tableId).map { it.courseName })
        assertEquals(
            listOf("旧整周项目"),
            db.cqieUnscheduledDao().getByTable(tableId).map { it.courseName },
        )
    }

    @Test
    fun migrationThreeToFourKeepsExistingTableAndCourses() {
        db.close()
        val name = "cqie-migration-${System.nanoTime()}.db"
        context.deleteDatabase(name)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(sqlDb: SupportSQLiteDatabase) {
                    createV3Schema(sqlDb)
                    sqlDb.execSQL(
                        """INSERT INTO time_tables
                           (id,name,startDate,maxWeek,nodesPerDay,timeJson,color,isDefault,smartConfigJson,createdAt)
                           VALUES (1,'迁移样本','2026-01-05',20,12,'[]','#FF6750A4',1,'',1)"""
                    )
                    sqlDb.execSQL(
                        """INSERT INTO courses
                           (id,groupId,tableId,courseName,teacher,room,note,day,startNode,step,startWeek,endWeek,type,color,ownTime,startTime,endTime,credit,level)
                           VALUES (1,'g1',1,'迁移前课程','','','',1,1,2,1,16,0,'#FF6750A4',0,'','',0,0)"""
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
            helper.writableDatabase
        }

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            migrated.openHelper.writableDatabase
            assertEquals("迁移样本", runBlocking { migrated.timeTableDao().getById(1) }!!.name)
            assertEquals("迁移前课程", runBlocking { migrated.courseDao().getByTable(1) }.single().courseName)
            assertTrue(runBlocking { migrated.cqieUnscheduledDao().getByTable(1) }.isEmpty())
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    private fun table(name: String, startDate: String) = TimeTableEntity(
        name = name,
        startDate = startDate,
        isDefault = true,
    )

    private fun course(tableId: Long, name: String) = CourseEntity(
        groupId = "group-$name",
        tableId = tableId,
        courseName = name,
        day = 1,
        startNode = 1,
        step = 2,
        startWeek = 1,
        endWeek = 16,
        color = "#FF6750A4",
    )

    private fun unscheduled(tableId: Long, name: String) = CqieUnscheduledEntity(
        tableId = tableId,
        courseName = name,
        weeksJson = "[1,2,3,4]",
        kind = "WHOLE_WEEK",
    )

    private fun createV3Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `time_tables` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL,
                `startDate` TEXT NOT NULL, `maxWeek` INTEGER NOT NULL, `nodesPerDay` INTEGER NOT NULL,
                `timeJson` TEXT NOT NULL, `color` TEXT NOT NULL, `isDefault` INTEGER NOT NULL,
                `smartConfigJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `courses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` TEXT NOT NULL,
                `tableId` INTEGER NOT NULL, `courseName` TEXT NOT NULL, `teacher` TEXT NOT NULL,
                `room` TEXT NOT NULL, `note` TEXT NOT NULL, `day` INTEGER NOT NULL,
                `startNode` INTEGER NOT NULL, `step` INTEGER NOT NULL, `startWeek` INTEGER NOT NULL,
                `endWeek` INTEGER NOT NULL, `type` INTEGER NOT NULL, `color` TEXT NOT NULL,
                `ownTime` INTEGER NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL,
                `credit` REAL NOT NULL, `level` INTEGER NOT NULL,
                FOREIGN KEY(`tableId`) REFERENCES `time_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"""
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_tableId` ON `courses` (`tableId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_day` ON `courses` (`day`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_courses_startWeek_endWeek` ON `courses` (`startWeek`, `endWeek`)"
        )
    }
}
