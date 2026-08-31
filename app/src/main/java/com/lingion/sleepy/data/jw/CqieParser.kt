package com.lingion.sleepy.data.jw

import org.json.JSONArray
import org.json.JSONObject

enum class CqieUnscheduledKind {
    WHOLE_WEEK,
    NO_TIME_AND_ROOM,
    MISSING_SCHEDULE,
}

data class CqieUnscheduledCourse(
    val name: String,
    val courseCode: String,
    val teacher: String,
    val room: String,
    val weeks: List<Int>,
    val kind: CqieUnscheduledKind,
)

data class CqieDroppedRow(
    val rowIndex: Int,
    val reason: String,
)

data class CqieParseResult(
    val scheduled: List<JwCourse>,
    val unscheduled: List<CqieUnscheduledCourse>,
    val sourceRowCount: Int,
    val droppedRows: List<CqieDroppedRow>,
) {
    val validCourseCount: Int get() = scheduled.size + unscheduled.size
}

class CqieParseException(message: String) : IllegalArgumentException(message)

/** CQIE timetable JSON parser. This protocol never accepts HTML or another school's JSON shape. */
class CqieParser(source: String) : JwParser(source) {
    override fun generateCourseList(): List<JwCourse> = parse().scheduled

    override fun confidence(): Int = if (looksLikeCqieEnvelope(source)) 100 else 0

    override fun matchedFeatures(): List<String> = if (looksLikeCqieEnvelope(source)) {
        listOf("CQIE:data[]+teachingWeek+period")
    } else {
        emptyList()
    }

    fun parse(): CqieParseResult {
        if (source.isBlank()) throw CqieParseException("CQIE 响应为空")
        val root = try {
            JSONObject(source)
        } catch (_: Exception) {
            throw CqieParseException("CQIE 响应不是有效 JSON")
        }
        if (!root.optString("status").equals("success", ignoreCase = true)) {
            throw CqieParseException("CQIE 接口未返回成功状态")
        }
        val rows = root.optJSONArray("data")
            ?: throw CqieParseException("CQIE 响应缺少 data 数组")
        val scheduled = mutableListOf<JwCourse>()
        val unscheduled = mutableListOf<CqieUnscheduledCourse>()
        val dropped = mutableListOf<CqieDroppedRow>()

        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index)
            if (row == null) {
                dropped += CqieDroppedRow(index, "课程项不是对象")
                continue
            }
            val name = row.optString("courseName").trim()
            if (name.isBlank()) {
                dropped += CqieDroppedRow(index, "课程名为空")
                continue
            }
            val weeks = bitmapPositions(row.optNullableString("teachingWeek"))
            if (weeks.isEmpty()) {
                dropped += CqieDroppedRow(index, "周次为空或无效")
                continue
            }
            val teacher = parseTeachers(row.optJSONArray("classTimetableInstrVOList"))
            val room = buildRoom(row)
            val wholeWeek = row.optBoolean("wholeWeekOccupy", false)
            val noTime = row.optBoolean("notArrangeTimeAndRoom", false)
            val day = row.optNullableString("weekDay")?.toIntOrNull()
            val periods = bitmapPositions(row.optNullableString("period"))

            if (wholeWeek || noTime || day !in 1..7 || periods.isEmpty()) {
                val kind = when {
                    wholeWeek -> CqieUnscheduledKind.WHOLE_WEEK
                    noTime -> CqieUnscheduledKind.NO_TIME_AND_ROOM
                    else -> CqieUnscheduledKind.MISSING_SCHEDULE
                }
                unscheduled += CqieUnscheduledCourse(
                    name = name,
                    courseCode = row.optString("courseCode").trim(),
                    teacher = teacher,
                    room = room,
                    weeks = weeks,
                    kind = kind,
                )
                continue
            }

            val weekRuns = encodeWeeks(weeks)
            val periodRuns = continuousRuns(periods)
            for (week in weekRuns) {
                for (period in periodRuns) {
                    scheduled += JwCourse(
                        name = name,
                        room = room,
                        teacher = teacher,
                        day = day!!,
                        startNode = period.first,
                        endNode = period.last,
                        startWeek = week.start,
                        endWeek = week.end,
                        type = week.type,
                    )
                }
            }
        }

        return CqieParseResult(scheduled, unscheduled, rows.length(), dropped)
    }

    private data class WeekRun(val start: Int, val end: Int, val type: Int)

    private fun encodeWeeks(weeks: List<Int>): List<WeekRun> {
        if (weeks.size == 1) return listOf(WeekRun(weeks.first(), weeks.first(), 0))
        if (weeks.zipWithNext().all { (a, b) -> b == a + 1 }) {
            return listOf(WeekRun(weeks.first(), weeks.last(), 0))
        }
        if (weeks.zipWithNext().all { (a, b) -> b == a + 2 }) {
            val type = if (weeks.first() % 2 == 1) 1 else 2
            return listOf(WeekRun(weeks.first(), weeks.last(), type))
        }
        return continuousRuns(weeks).map { WeekRun(it.first, it.last, 0) }
    }

    private fun continuousRuns(values: List<Int>): List<IntRange> {
        if (values.isEmpty()) return emptyList()
        val result = mutableListOf<IntRange>()
        var start = values.first()
        var previous = start
        for (value in values.drop(1)) {
            if (value != previous + 1) {
                result += start..previous
                start = value
            }
            previous = value
        }
        result += start..previous
        return result
    }

    private fun bitmapPositions(raw: String?): List<Int> {
        val bitmap = raw?.trim().orEmpty()
        if (bitmap.isEmpty() || bitmap.any { it != '0' && it != '1' }) return emptyList()
        return bitmap.mapIndexedNotNull { index, value -> if (value == '1') index + 1 else null }
    }

    private fun parseTeachers(array: JSONArray?): String {
        if (array == null) return ""
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.optString("instructorName")?.trim()?.takeIf { it.isNotBlank() }
        }.distinct().joinToString("、")
    }

    private fun buildRoom(row: JSONObject): String {
        val campus = row.optString("roomBuildingCampusName").trim()
        val room = row.optString("roomName").trim()
        return listOf(campus, room).filter { it.isNotBlank() }.distinct().joinToString(" ")
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() }
    }

    companion object {
        internal fun looksLikeCqieEnvelope(source: String): Boolean = runCatching {
            val rows = JSONObject(source).optJSONArray("data") ?: return@runCatching false
            (0 until rows.length()).any { index ->
                val row = rows.optJSONObject(index)
                row != null && row.has("teachingWeek") &&
                    (row.has("period") || row.has("wholeWeekOccupy") || row.has("notArrangeTimeAndRoom"))
            }
        }.getOrDefault(false)
    }
}
