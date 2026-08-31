package com.lingion.sleepy.data.jw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CqieFetchResultTest {
    @Test
    fun `success keeps response body only in success result`() {
        val result = CqieFetchResultCodec.decode(
            """{"ok":true,"kind":"SUCCESS","data":"{\"data\":[]}"}"""
        )
        assertTrue(result is CqieFetchResult.Success)
        assertEquals("""{"data":[]}""", (result as CqieFetchResult.Success).body)
    }

    @Test
    fun `all production failures decode without response body`() {
        val kinds = listOf(
            "WRONG_ORIGIN", "SESSION_EXPIRED", "LOGIN_REDIRECT", "LOGIN_PAGE", "NETWORK",
            "EMPTY", "MALFORMED_JSON", "HTTP_ERROR", "BRIDGE_ERROR"
        )
        for (kind in kinds) {
            val result = CqieFetchResultCodec.decode(
                """{"ok":false,"kind":"$kind","status":401,"data":"must-not-surface"}"""
            )
            assertTrue("kind=$kind", result is CqieFetchResult.Failure)
            result as CqieFetchResult.Failure
            assertEquals(CqieFetchErrorKind.valueOf(kind), result.kind)
            assertEquals(401, result.status)
        }
    }

    @Test
    fun `malformed bridge messages become bridge error`() {
        val malformed = CqieFetchResultCodec.decode("not-json") as CqieFetchResult.Failure
        assertEquals(CqieFetchErrorKind.BRIDGE_ERROR, malformed.kind)
        val emptySuccess = CqieFetchResultCodec.decode(
            """{"ok":true,"kind":"SUCCESS","data":""}"""
        ) as CqieFetchResult.Failure
        assertEquals(CqieFetchErrorKind.EMPTY, emptySuccess.kind)
    }
}
