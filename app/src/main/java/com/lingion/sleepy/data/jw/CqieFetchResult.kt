package com.lingion.sleepy.data.jw

import org.json.JSONObject

enum class CqieFetchErrorKind {
    WRONG_ORIGIN,
    SESSION_EXPIRED,
    LOGIN_REDIRECT,
    LOGIN_PAGE,
    NETWORK,
    EMPTY,
    MALFORMED_JSON,
    HTTP_ERROR,
    BRIDGE_ERROR,
}

sealed class CqieFetchResult {
    data class Success(val body: String) : CqieFetchResult()
    data class Failure(val kind: CqieFetchErrorKind, val status: Int?) : CqieFetchResult()
}

object CqieFetchResultCodec {
    fun decode(message: String): CqieFetchResult {
        val obj = try {
            JSONObject(message)
        } catch (_: Exception) {
            return CqieFetchResult.Failure(CqieFetchErrorKind.BRIDGE_ERROR, null)
        }
        if (obj.optBoolean("ok", false) && obj.optString("kind") == "SUCCESS") {
            val body = obj.optString("data", "")
            return if (body.isNotBlank()) {
                CqieFetchResult.Success(body)
            } else {
                CqieFetchResult.Failure(CqieFetchErrorKind.EMPTY, null)
            }
        }
        val kind = runCatching {
            CqieFetchErrorKind.valueOf(obj.optString("kind"))
        }.getOrDefault(CqieFetchErrorKind.BRIDGE_ERROR)
        val status = obj.optInt("status", 0).takeIf { it > 0 }
        return CqieFetchResult.Failure(kind, status)
    }
}
