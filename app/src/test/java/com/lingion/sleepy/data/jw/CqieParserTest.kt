package com.lingion.sleepy.data.jw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CqieParserTest {
    private val fixture by lazy {
        File("src/test/resources/jw_fixtures/cqie/timetable_success.json").readText(Charsets.UTF_8)
    }

    @Test
    fun `fixture preserves scheduled no-time and whole-week courses`() {
        val result = CqieParser(fixture).parse()

        assertEquals(4, result.sourceRowCount)
        assertEquals(2, result.scheduled.size)
        assertEquals(2, result.unscheduled.size)
        assertEquals(4, result.validCourseCount)
        assertTrue(result.droppedRows.isEmpty())

        val continuous = result.scheduled.single { it.name == "示例课程A" }
        assertEquals(1, continuous.startWeek)
        assertEquals(8, continuous.endWeek)
        assertEquals(0, continuous.type)
        assertEquals(3, continuous.startNode)
        assertEquals(4, continuous.endNode)
        assertEquals("示例校区 教学楼A101", continuous.room)
        assertEquals("教师A", continuous.teacher)

        val odd = result.scheduled.single { it.name == "示例课程B" }
        assertEquals(1, odd.startWeek)
        assertEquals(7, odd.endWeek)
        assertEquals(1, odd.type)
        assertEquals(CqieUnscheduledKind.NO_TIME_AND_ROOM,
            result.unscheduled.single { it.name == "无节次项目A" }.kind)
        assertEquals(CqieUnscheduledKind.WHOLE_WEEK,
            result.unscheduled.single { it.name == "整周项目A" }.kind)
    }

    @Test
    fun `even weeks use even type`() {
        val result = parseSingle(weeks = "01010101", periods = "11")
        assertEquals(listOf(2, 8, 2), result.scheduled.single().let { listOf(it.startWeek, it.endWeek, it.type) })
    }

    @Test
    fun `arbitrary discrete weeks split into exact continuous runs`() {
        val result = parseSingle(weeks = "11001011", periods = "11")
        assertEquals(
            listOf(Triple(1, 2, 0), Triple(5, 5, 0), Triple(7, 8, 0)),
            result.scheduled.map { Triple(it.startWeek, it.endWeek, it.type) }
        )
    }

    @Test
    fun `discrete periods split without widening occupied time`() {
        val result = parseSingle(weeks = "1111", periods = "110011")
        assertEquals(listOf(1 to 2, 5 to 6), result.scheduled.map { it.startNode to it.endNode })
    }

    @Test
    fun `missing schedule is preserved as unscheduled instead of sentinel nodes`() {
        val result = parseSingle(weeks = "1111", periods = null, day = null)
        assertTrue(result.scheduled.isEmpty())
        assertEquals(CqieUnscheduledKind.MISSING_SCHEDULE, result.unscheduled.single().kind)
    }

    @Test(expected = CqieParseException::class)
    fun `malformed json is rejected`() {
        CqieParser("{not-json").parse()
    }

    @Test(expected = CqieParseException::class)
    fun `missing data array is rejected`() {
        CqieParser("""{"status":"success"}""").parse()
    }

    @Test(expected = CqieParseException::class)
    fun `backend failure status is rejected even when data is present`() {
        CqieParser("""{"status":"error","data":[]}""").parse()
    }

    private fun parseSingle(weeks: String, periods: String?, day: String? = "1"): CqieParseResult {
        val periodJson = periods?.let { "\"$it\"" } ?: "null"
        val dayJson = day?.let { "\"$it\"" } ?: "null"
        val json = """{
          "status":"success",
          "data":[{
            "courseName":"边界课程",
            "courseCode":"BOUNDARY-001",
            "classTimetableInstrVOList":[],
            "teachingWeek":"$weeks",
            "period":$periodJson,
            "weekDay":$dayJson,
            "wholeWeekOccupy":false,
            "notArrangeTimeAndRoom":false
          }]
        }"""
        return CqieParser(json).parse()
    }
}
