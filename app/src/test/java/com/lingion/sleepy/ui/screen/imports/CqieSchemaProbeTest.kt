package com.lingion.sleepy.ui.screen.imports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CqieFetchJsTest {

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
    fun `production fetch is fixed-origin fixed-endpoint and keeps token inside WebView`() {
        assertEquals("https://njw.cqie.edu.cn", CQIE_ORIGIN)
        assertEquals("/api/enrollment/timetable/student", CQIE_TIMETABLE_ENDPOINT)
        assertTrue(CQIE_FETCH_JS.contains("location.origin !== ORIGIN"))
        assertTrue(CQIE_FETCH_JS.contains("fetch(ENDPOINT"))
        assertTrue(CQIE_FETCH_JS.contains("localStorage.getItem('cqu_edu_ACCESS_TOKEN')"))
        assertTrue(CQIE_FETCH_JS.contains("var decoded = JSON.parse(stored)"))
        assertTrue(CQIE_FETCH_JS.contains("'Authorization':'Bearer ' + accessToken"))
        assertTrue(CQIE_FETCH_JS.contains("redirect:'manual'"))
        assertTrue(CQIE_FETCH_JS.contains("{ok:true,kind:'SUCCESS',data:text}"))
        assertFalse(CQIE_FETCH_JS.contains("cqu_edu_CURRENT_TOKEN"))
        assertFalse(CQIE_FETCH_JS.contains("accessToken:accessToken"))
        assertFalse(CQIE_FETCH_JS.contains("authorization:accessToken"))
        assertFalse(CQIE_FETCH_JS.contains("console."))
    }

    @Test
    fun `CQIE webview source keeps strict TLS branches`() {
        val source = java.io.File(
            "src/main/java/com/lingion/sleepy/ui/screen/imports/JwWebViewLoginScreen.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("strictTls = isCqieSchool(school)"))
        assertTrue(source.contains("WebSettings.MIXED_CONTENT_NEVER_ALLOW"))
        assertTrue(source.contains("if (strictTls) handler.cancel() else handler.proceed()"))
        assertTrue(source.contains("console[${'$'}{msg?.messageLevel()}]: message suppressed"))
    }
}
