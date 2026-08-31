package com.lingion.sleepy.ui.screen.imports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CqieSchemaProbeTest {

    @Test
    fun `webview log URL strips query and fragment`() {
        assertEquals(
            "https://njw.cqie.edu.cn/enroll/token-index",
            privacySafeWebViewUrl(
                "https://njw.cqie.edu.cn/enroll/token-index?code=sensitive#fragment"
            )
        )
        assertFalse(privacySafeWebViewUrl("https://example.edu/?token=secret").contains("secret"))
    }

    @Test
    fun `CQIE origin requires exact https host and default port`() {
        assertTrue(isCqieOrigin("https://njw.cqie.edu.cn/enroll/CourseStuSelectionList?x=1"))
        assertFalse(isCqieOrigin("http://njw.cqie.edu.cn/enroll/CourseStuSelectionList"))
        assertFalse(isCqieOrigin("https://njw.cqie.edu.cn.evil.example/path"))
        assertFalse(isCqieOrigin("https://njw.cqie.edu.cn:444/path"))
    }

    @Test
    fun `schema probe is fixed-origin fixed-endpoint and never returns raw text`() {
        assertEquals("https://njw.cqie.edu.cn", CQIE_ORIGIN)
        assertEquals("/api/enrollment/timetable/student", CQIE_TIMETABLE_ENDPOINT)
        assertTrue(CQIE_SCHEMA_PROBE_JS.contains("location.origin !== ORIGIN"))
        assertTrue(CQIE_SCHEMA_PROBE_JS.contains("fetch(ENDPOINT"))
        assertTrue(CQIE_SCHEMA_PROBE_JS.contains("localStorage.getItem('cqu_edu_ACCESS_TOKEN')"))
        assertTrue(CQIE_SCHEMA_PROBE_JS.contains("var decoded = JSON.parse(stored)"))
        assertTrue(CQIE_SCHEMA_PROBE_JS.contains("headers['Authorization'] = 'Bearer ' + accessToken"))
        assertTrue(CQIE_SCHEMA_PROBE_JS.contains("meta.projection = project(parsed"))
        assertFalse(CQIE_SCHEMA_PROBE_JS.contains("data:text"))
        assertFalse(CQIE_SCHEMA_PROBE_JS.contains("projection:text"))
        assertFalse(CQIE_SCHEMA_PROBE_JS.contains("accessToken:accessToken"))
        assertFalse(CQIE_SCHEMA_PROBE_JS.contains("authorization:accessToken"))
    }

    @Test
    fun `CQIE webview source keeps strict TLS branches`() {
        val source = java.io.File(
            "src/main/java/com/lingion/sleepy/ui/screen/imports/JwWebViewLoginScreen.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("strictTls = isCqieSchool(school)"))
        assertTrue(source.contains("WebSettings.MIXED_CONTENT_NEVER_ALLOW"))
        assertTrue(source.contains("if (strictTls) handler.cancel() else handler.proceed()"))
    }
}
