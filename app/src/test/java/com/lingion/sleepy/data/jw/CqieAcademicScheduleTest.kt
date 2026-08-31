package com.lingion.sleepy.data.jw

import com.lingion.sleepy.util.TimeTableUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CqieAcademicScheduleTest {
    @Test
    fun `uses the authoritative twelve CQIE periods`() {
        assertEquals(12, CqieAcademicSchedule.NODES_PER_DAY)
        assertEquals(
            listOf(
                "08:30-09:15",
                "09:25-10:10",
                "10:30-11:15",
                "11:25-12:10",
                "14:00-14:45",
                "14:55-15:40",
                "16:00-16:45",
                "16:55-17:40",
                "19:00-19:45",
                "19:55-20:40",
                "20:50-21:35",
                "21:45-22:30",
            ),
            CqieAcademicSchedule.rows.map { "${it.start}-${it.end}" },
        )
        assertEquals(
            (1..12).toList(),
            CqieAcademicSchedule.rows.map { it.node },
        )
    }

    @Test
    fun `CQIE period JSON round trips without falling back to global defaults`() {
        assertEquals(
            CqieAcademicSchedule.rows,
            TimeTableUtils.parseTimeSlotRows(CqieAcademicSchedule.timeJson),
        )
        assertEquals("08:30", CqieAcademicSchedule.rows.first().start)
        assertEquals("08:00", TimeTableUtils.parseTimeSlotRows(TimeTableUtils.DEFAULT_TIME_JSON).first().start)
    }
}
