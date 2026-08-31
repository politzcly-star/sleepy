package com.lingion.sleepy.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CqieUnscheduledEntityTest {
    @Test
    fun `weeks JSON remains exact for arbitrary discrete weeks`() {
        val entity = CqieUnscheduledEntity(
            tableId = 1,
            courseName = "边界项目",
            weeksJson = "[1,3,5,8]",
            kind = "WHOLE_WEEK",
        )
        assertEquals(listOf(1, 3, 5, 8), entity.weeks())
        assertTrue(entity.inWeek(5))
        assertFalse(entity.inWeek(6))
    }

    @Test
    fun `malformed week JSON is safe and empty`() {
        val entity = CqieUnscheduledEntity(
            tableId = 1,
            courseName = "边界项目",
            weeksJson = "not-json",
            kind = "MISSING_SCHEDULE",
        )
        assertTrue(entity.weeks().isEmpty())
    }
}
