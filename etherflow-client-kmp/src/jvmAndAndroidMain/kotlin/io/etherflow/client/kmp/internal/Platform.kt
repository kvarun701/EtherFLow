package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.HttpClientEngine
import io.etherflow.client.kmp.HttpRequest
import io.etherflow.client.kmp.HttpResponse
import io.etherflow.client.kmp.StreamedResponse
import io.etherflow.client.kmp.WebSocketSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

actual fun platformEngine(): HttpClientEngine = OkHttpEngine

internal object OkHttpEngine : HttpClientEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .readTimeout(java.time.Duration.ofSeconds(30))
        .build()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        val reqBuilder = buildRequest(request)
        val response = withContext(Dispatchers.IO) {
            client.newCall(reqBuilder.build()).execute()
        }

        return HttpResponse(
            statusCode = response.code,
            statusText = response.message,
            headers = response.headers.toMap().mapValues { it.value },
            body = response.body?.bytes() ?: ByteArray(0)
        )
    }

    override suspend fun executeStreaming(request: HttpRequest): StreamedResponse {
        val reqBuilder = buildRequest(request)
        val response = withContext(Dispatchers.IO) {
            client.newCall(reqBuilder.build()).execute()
        }

        val statusCode = response.code
        val statusText = response.message
        val headers = response.headers.toMap().mapValues { it.value }
        val contentLength = response.body?.contentLength() ?: -1L
        val body = response.body

        val chunks = channelFlow {
            if (body != null) {
                val stream = body.byteStream()
                try {
                    val buffer = ByteArray(8192)
                    var bytesRead = stream.read(buffer)
                    while (bytesRead >= 0) {
                        send(buffer.copyOf(bytesRead))
                        bytesRead = stream.read(buffer)
                    }
                } finally {
                    stream.close()
                    body.close()
                }
            }
        }

        return StreamedResponse(
            statusCode = statusCode,
            statusText = statusText,
            headers = headers,
            contentLength = contentLength,
            chunks = chunks
        )
    }

    override suspend fun createWebSocket(url: String, headers: Map<String, String>): WebSocketSession {
        return JvmWebSocketSession(url, headers, client)
    }

    private fun buildRequest(request: HttpRequest): Request.Builder {
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
        return reqBuilder
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
