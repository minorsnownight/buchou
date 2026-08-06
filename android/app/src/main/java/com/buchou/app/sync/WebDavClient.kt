package com.buchou.app.sync

import java.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class WebDavClient(private val config: WebDavConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(30_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    private val authHeader = "Basic " + Base64.getEncoder()
        .encodeToString("${config.username}:${config.password}".toByteArray())

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    private fun buildUrl(path: String): String =
        ensureTrailingSlash(config.url) + path

    var lastResponseCode: Int = 0
        private set

    fun mkcol(path: String): Boolean {
        val request = Request.Builder()
            .url(buildUrl(path))
            .method("MKCOL", null)
            .header("Authorization", authHeader)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                lastResponseCode = response.code
                response.code in 200..299 || response.code == 405
            }
        } catch (_: Exception) {
            false
        }
    }

    fun put(path: String, data: ByteArray): Boolean {
        val body = data.toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder()
            .url(buildUrl(path))
            .put(body)
            .header("Authorization", authHeader)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                lastResponseCode = response.code
                response.code in 200..299
            }
        } catch (e: Exception) {
            lastResponseCode = -1
            throw e
        }
    }

    fun get(path: String): ByteArray? {
        val request = Request.Builder()
            .url(buildUrl(path))
            .get()
            .header("Authorization", authHeader)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                lastResponseCode = response.code
                if (response.code in 200..299) {
                    response.body?.bytes()
                } else null
            }
        } catch (e: Exception) {
            lastResponseCode = -1
            throw e
        }
    }
}
