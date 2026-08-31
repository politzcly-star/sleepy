package com.lingion.sleepy.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(
    tableName = "cqie_unscheduled_courses",
    indices = [Index("tableId")],
    foreignKeys = [
        ForeignKey(
            entity = TimeTableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class CqieUnscheduledEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "tableId") val tableId: Long,
    @ColumnInfo(name = "courseName") val courseName: String,
    @ColumnInfo(name = "courseCode") val courseCode: String = "",
    @ColumnInfo(name = "teacher") val teacher: String = "",
    @ColumnInfo(name = "room") val room: String = "",
    @ColumnInfo(name = "weeksJson") val weeksJson: String,
    @ColumnInfo(name = "kind") val kind: String,
) {
    fun weeks(): List<Int> = runCatching {
        val array = JSONArray(weeksJson)
        (0 until array.length()).mapNotNull { index ->
            array.optInt(index, 0).takeIf { it > 0 }
        }.distinct().sorted()
    }.getOrDefault(emptyList())

    fun inWeek(week: Int): Boolean = week in weeks()
}
