package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.HttpClientEngine
import io.etherflow.client.kmp.HttpRequest
import io.etherflow.client.kmp.HttpResponse
import io.etherflow.client.kmp.StreamedResponse
import io.etherflow.client.kmp.WebSocketSession
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.channelFlow
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

actual fun platformEngine(): HttpClientEngine = JsEngine

internal object JsEngine : HttpClientEngine {

    private fun getFetch(): dynamic {
        val globalThisFetch = js("typeof globalThis !== 'undefined' ? globalThis.fetch : undefined")
        if (globalThisFetch != null) return globalThisFetch

        val windowFetch = js("typeof window !== 'undefined' ? window.fetch : undefined")
        if (windowFetch != null) return windowFetch

        val nodeFetch = js("typeof fetch !== 'undefined' ? fetch : undefined")
        if (nodeFetch != null) return nodeFetch

        throw IllegalStateException("No fetch implementation found in this JavaScript environment.")
    }

    override suspend fun execute(request: HttpRequest): HttpResponse {
        val options = buildOptions(request)
        val fetchFunc = getFetch()
        val response = try {
            (fetchFunc(request.url, options) as kotlin.js.Promise<dynamic>).await()
        } catch (e: Exception) {
            return suspendCancellableCoroutine { cont ->
                cont.resumeWithException(Exception("Fetch failed: ${e.message}"))
            }
        }

        val statusCode: Int = response.status.toInt()
        val statusText: String = response.statusText ?: ""
        val headers = parseHeaders(response)

        val buffer: ArrayBuffer = try {
            response.arrayBuffer().await()
        } catch (e: Exception) {
            return suspendCancellableCoroutine { cont ->
                cont.resume(HttpResponse(statusCode, statusText, headers, ByteArray(0)))
            }
        }

        val body = Int8Array(buffer).toByteArray()
        return HttpResponse(statusCode, statusText, headers, body)
    }

    override suspend fun executeStreaming(request: HttpRequest): StreamedResponse {
        val options = buildOptions(request)
        val fetchFunc = getFetch()
        val response = try {
            (fetchFunc(request.url, options) as kotlin.js.Promise<dynamic>).await()
        } catch (e: Exception) {
            return suspendCancellableCoroutine { cont ->
                cont.resumeWithException(Exception("Fetch failed: ${e.message}"))
            }
        }

        val statusCode: Int = response.status.toInt()
        val statusText: String = response.statusText ?: ""
        val headers = parseHeaders(response)
        val contentLength = headers["Content-Length"]?.firstOrNull()?.toLongOrNull() ?: -1L

        val chunks = channelFlow {
            val body = response.body
            if (body != null) {
                val reader = body.asDynamic().getReader()
                try {
                    while (true) {
                        val result = reader.read().await()
                        if (result.done) break
                        val uint8 = result.value.asDynamic()
                        val len: Int = uint8.length.toInt()
                        val chunk = ByteArray(len) { i -> uint8[i].toByte() }
                        send(chunk)
                    }
                } finally {
                    reader.cancel()
                }
            }
        }

        return StreamedResponse(statusCode, statusText, headers, contentLength, chunks)
    }

    private fun buildOptions(request: HttpRequest): dynamic {
        val headersObj = js("({})")
        for ((key, value) in request.headers) {
            headersObj[key] = value
        }

        val options = js("({})")
        options["method"] = request.method
        options["headers"] = headersObj

        val bodyBytes = request.body
        if (bodyBytes != null && bodyBytes.isNotEmpty()) {
            options["body"] = bodyBytes.decodeToString()
        }
        return options
    }

    override suspend fun createWebSocket(url: String, headers: Map<String, String>): WebSocketSession {
        return JsWebSocketSession(url, headers)
    }

    private fun parseHeaders(response: dynamic): Map<String, List<String>> {
        val headers = mutableMapOf<String, List<String>>()
        response.headers.asDynamic().forEach { key, value ->
            headers["$key"] = listOf("$value")
        }
        return headers
    }
}

internal fun Int8Array.toByteArray(): ByteArray {
    val len = this.length
    val result = ByteArray(len)
    val self = this.asDynamic()
    for (i in 0 until len) {
        result[i] = self[i].toByte()
    }
    return result
}
