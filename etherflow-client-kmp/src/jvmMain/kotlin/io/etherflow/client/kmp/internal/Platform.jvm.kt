package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.HttpClientEngine
import io.etherflow.client.kmp.HttpRequest
import io.etherflow.client.kmp.HttpResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

actual fun platformEngine(): HttpClientEngine = OkHttpEngine

internal object OkHttpEngine : HttpClientEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .readTimeout(java.time.Duration.ofSeconds(30))
        .build()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        val reqBuilder = Request.Builder().url(request.url)
        for ((name, value) in request.headers) {
            reqBuilder.addHeader(name, value)
        }

        val body = request.body
        if (body != null && body.isNotEmpty()) {
            val mediaType = request.headers["Content-Type"]?.let {
                it.toMediaTypeOrNull()
            } ?: "application/json; charset=utf-8".toMediaTypeOrNull()
            reqBuilder.method(request.method, body.toRequestBody(mediaType))
        } else {
            reqBuilder.method(request.method, null)
        }

        val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(reqBuilder.build()).execute()
        }

        return HttpResponse(
            statusCode = response.code,
            statusText = response.message,
            headers = response.headers.toMap().mapValues { it.value },
            body = response.body?.bytes() ?: ByteArray(0)
        )
    }
}

internal fun Headers.toMap(): Map<String, List<String>> {
    val map = linkedMapOf<String, MutableList<String>>()
    for (i in 0 until size) {
        val name = name(i)
        val value = value(i)
        map.getOrPut(name) { mutableListOf() }.add(value)
    }
    return map
}
