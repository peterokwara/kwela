package app.kwela.data.remote

import app.kwela.domain.ApiMessages
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
    data class Failure(val message: String) : ApiResult
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

    suspend fun chat(token: String, requestJson: String): ApiResult = call(token, "POST", "/api/chat", requestJson)

    suspend fun saveProfile(token: String, profileJson: String): ApiResult =
        call(token, "PUT", "/api/candidate-profile", """{"profile":$profileJson}""")

    suspend fun deleteAccount(token: String): ApiResult = call(token, "DELETE", "/api/account", null)

    private suspend fun call(token: String, method: String, path: String, jsonBody: String?): ApiResult {
        if (!isConfigured) return ApiResult.Failure("Kwela’s assistant isn’t available in this build.")
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .header("authorization", "Bearer $token")
            .method(method, jsonBody?.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            client.newCall(request).await().use { response ->
                val body = withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
                if (response.isSuccessful) ApiResult.Success(body)
                else ApiResult.Failure(ApiMessages.forStatus(response.code, serverMessage(body)))
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
