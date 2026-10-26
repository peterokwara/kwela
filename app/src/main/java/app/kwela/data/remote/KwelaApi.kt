package app.kwela.data.remote

import app.kwela.domain.ApiMessages
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

sealed interface ApiResult {
    data class Success(val body: String) : ApiResult
    data class Failure(val message: String, val status: Int? = null) : ApiResult
}

/** Talks to the Kwela API Worker. An empty [baseUrl] means no backend is configured for this build. */
class KwelaApi(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()

    suspend fun requestCode(email: String): ApiResult =
        call(null, "POST", "/auth/request-code", buildJsonObject { put("email", email) }.toString())

    /** Tells the server whether this account allows usage statistics. Off stops the server's events for it too. */
    suspend fun setAnalytics(token: String, enabled: Boolean): ApiResult =
        call(token, "PUT", "/api/analytics", buildJsonObject { put("enabled", enabled) }.toString())

    /** Signs in with the ID token Google's sign-in sheet gave the phone. */
    suspend fun googleSignIn(idToken: String): ApiResult =
        call(null, "POST", "/auth/google", buildJsonObject { put("idToken", idToken) }.toString())

    suspend fun verifyCode(email: String, code: String): ApiResult =
        call(null, "POST", "/auth/verify", buildJsonObject { put("email", email); put("code", code) }.toString())

    /** Asks the server for the full-design PDF of a CV or cover letter. Returns null when it can't be made. */
    suspend fun renderPdf(token: String, type: String, documentJson: String): ByteArray? {
        if (!isConfigured) return null
        val body = """{"type":"$type","document":$documentJson}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/api/documents/pdf").header("authorization", "Bearer $token").post(body).build()
        return try {
            client.newBuilder().callTimeout(30, TimeUnit.SECONDS).build().newCall(request).await().use { response ->
                if (response.isSuccessful && response.header("content-type")?.startsWith("application/pdf") == true) withContext(Dispatchers.IO) { response.body?.bytes() } else null
            }
        } catch (_: IOException) {
            null
        }
    }

    suspend fun signOut(token: String): ApiResult = call(token, "POST", "/auth/sign-out", "{}")

    suspend fun chat(token: String, requestJson: String): ApiResult = call(token, "POST", "/api/chat", requestJson)

    /** Starts or reconnects to the durable run associated with one user message. */
    suspend fun startChatRun(token: String, idempotencyKey: String, requestJson: String, retryKey: String? = null): ApiResult =
        call(token, "POST", "/api/runs", requestJson, idempotencyKey, retryKey)

    /** Reads the durable run without resending its inputs. */
    suspend fun chatRun(token: String, runId: String): ApiResult = call(token, "GET", "/api/runs/$runId", null)

    /** Clears the temporary backend copy after the reply has been saved on this phone. */
    suspend fun acknowledgeChatRun(token: String, runId: String): ApiResult = call(token, "POST", "/api/runs/$runId/acknowledge", "{}")

    suspend fun saveProfile(token: String, profileJson: String): ApiResult =
        call(token, "PUT", "/api/candidate-profile", """{"profile":$profileJson}""")

    suspend fun deleteAccount(token: String): ApiResult = call(token, "DELETE", "/api/account", null)

    private suspend fun call(token: String?, method: String, path: String, jsonBody: String?, idempotencyKey: String? = null, retryKey: String? = null): ApiResult {
        if (!isConfigured) return ApiResult.Failure("Kwela’s assistant isn’t available in this build.")
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .apply { if (token != null) header("authorization", "Bearer $token") }
            .apply { if (idempotencyKey != null) header("idempotency-key", idempotencyKey) }
            .apply { if (retryKey != null) header("retry-key", retryKey) }
            .method(method, jsonBody?.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            client.newCall(request).await().use { response ->
                val body = withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
                if (response.isSuccessful) ApiResult.Success(body)
                else ApiResult.Failure(ApiMessages.forStatus(response.code, serverMessage(body)), response.code)
            }
        } catch (_: IOException) {
            ApiResult.Failure(ApiMessages.offline())
        }
    }

    /** Runs the call without blocking a thread, and cancels the request when the coroutine is cancelled. */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) = continuation.resume(response)
        })
    }

    private fun serverMessage(body: String): String? =
        runCatching { (Json.parseToJsonElement(body) as? JsonObject)?.get("message") as? JsonPrimitive }.getOrNull()?.takeIf { it.isString }?.content
}
