package me.elvish.statusreporter.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * HTTP client for reporting app activity and health data.
 * All methods perform synchronous IO — call from background threads only.
 */
class ReportClient(
    private val serverUrl: String,
    private val token: String
) {
    init {
        val uri = URI(serverUrl)
        val scheme = uri.scheme ?: ""
        val host = uri.host ?: ""
        require(
            scheme == "https" ||
            (scheme == "http" && (host == "localhost" || host == "127.0.0.1"))
        ) { "Only HTTPS or http://localhost allowed" }
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun reportApp(
        emoji: String,
        desc: String,
        ttl: Int
    ): Result<Unit> {
        val body = JSONObject().apply {
            put("key", token)
            put("emoji", emoji)
            put("desc", desc)
            put("ttl", ttl)
        }

        return post("${serverUrl.trimEnd('/')}/api/report", body)
    }

    fun testConnection(): Result<Unit> {
        val request = Request.Builder()
            .url("${serverUrl.trimEnd('/')}/api/health")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) Result.success(Unit)
                else Result.failure(IOException("HTTP ${it.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun post(url: String, body: JSONObject): Result<Unit> {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful || it.code == 409) Result.success(Unit)
                else Result.failure(IOException("HTTP ${it.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
