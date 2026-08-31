package com.lingion.sleepy.data.jw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CqieSchoolEntryTest {

    private fun loadSchools(): List<JwSchoolInfo> = JwImportViewModel.parseSchoolsJson(
        File("src/main/assets/schools.json").readText(Charsets.UTF_8)
    )

    @Test
    fun `CQIE opens the approved timetable page without claiming an HTML protocol`() {
        val cqie = loadSchools().single { it.name == "重庆工程学院" }

        assertEquals("C", cqie.sortKey)
        assertEquals("chongqinggongchengxueyuan", cqie.sortKeyFull)
        assertEquals(
            "https://njw.cqie.edu.cn/enroll/CourseStuSelectionList",
            cqie.url
        )
        assertNull(cqie.type)
        assertTrue(cqie.isSupported)
        assertTrue(cqie.aliases.containsAll(listOf("cqie", "njw.cqie.edu.cn")))
    }
}
