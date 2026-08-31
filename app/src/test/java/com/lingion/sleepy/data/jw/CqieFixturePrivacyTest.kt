package com.lingion.sleepy.data.jw

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CqieFixturePrivacyTest {

    private val fixture = File("src/test/resources/jw_fixtures/cqie/timetable_success.json")

    @Test
    fun `CQIE fixture is synthetic and contains no authentication material`() {
        val text = fixture.readText(Charsets.UTF_8)
        val root = JSONObject(text)
        val rows = root.getJSONArray("data")

        assertEquals(4, rows.length())
        assertFalse(Regex("(?i)Bearer\\s+[A-Za-z0-9_.-]{20,}").containsMatchIn(text))
        assertFalse(Regex("[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]{8,}").containsMatchIn(text))
        assertFalse(Regex("(?i)\\\"(token|cookie|password|username|studentName|studentCode)\\\"\\s*:").containsMatchIn(text))

        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            assertTrue(row.getString("courseName").matches(Regex("(示例课程|无节次项目|整周项目)[A-Z]")))
            for (key in listOf("campusId", "classId", "classNbr", "courseCode", "id", "roomId")) {
                val value = row.optString(key, "")
                assertTrue("$key must be synthetic: $value", value.isBlank() || value.contains("demo", ignoreCase = true) || value.matches(Regex("00[1-4]")))
            }
            val instructors = row.optJSONArray("classTimetableInstrVOList") ?: continue
            for (teacherIndex in 0 until instructors.length()) {
                val teacher = instructors.getJSONObject(teacherIndex)
                assertTrue(teacher.optString("instructorName").matches(Regex("教师[A-Z]")))
                for (key in listOf("classInstructorId", "instructorCode", "instructorId", "timetableId")) {
                    assertTrue(teacher.optString(key).contains("demo", ignoreCase = true))
                }
            }
        }
    }
}
