package com.lingion.sleepy.data.jw

import com.lingion.sleepy.util.TimeTableUtils

/** Authoritative CQIE teaching periods supplied by the school timetable. */
object CqieAcademicSchedule {
    const val NODES_PER_DAY = 12

    val rows: List<TimeTableUtils.TimeSlotRow> = listOf(
        TimeTableUtils.TimeSlotRow(1, "08:30", "09:15"),
        TimeTableUtils.TimeSlotRow(2, "09:25", "10:10"),
        TimeTableUtils.TimeSlotRow(3, "10:30", "11:15"),
        TimeTableUtils.TimeSlotRow(4, "11:25", "12:10"),
        TimeTableUtils.TimeSlotRow(5, "14:00", "14:45"),
        TimeTableUtils.TimeSlotRow(6, "14:55", "15:40"),
        TimeTableUtils.TimeSlotRow(7, "16:00", "16:45"),
        TimeTableUtils.TimeSlotRow(8, "16:55", "17:40"),
        TimeTableUtils.TimeSlotRow(9, "19:00", "19:45"),
        TimeTableUtils.TimeSlotRow(10, "19:55", "20:40"),
        TimeTableUtils.TimeSlotRow(11, "20:50", "21:35"),
        TimeTableUtils.TimeSlotRow(12, "21:45", "22:30"),
    )

    val timeJson: String
        get() = TimeTableUtils.buildTimeJsonFromRows(rows)
}
