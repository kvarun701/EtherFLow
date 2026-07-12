package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.HttpClientEngine
import io.etherflow.client.kmp.HttpRequest
import io.etherflow.client.kmp.HttpResponse
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

actual fun platformEngine(): HttpClientEngine = JsEngine

internal object JsEngine : HttpClientEngine {

    override suspend fun execute(request: HttpRequest): HttpResponse {
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

        val response = try {
            window.fetch(request.url, options).await()
        } catch (e: Exception) {
            return suspendCancellableCoroutine { cont ->
                cont.resumeWithException(Exception("Fetch failed: ${e.message}"))
            }
        }

        val statusCode: Int = response.status.toInt()
        val statusText: String = response.statusText ?: ""

        val headers = mutableMapOf<String, List<String>>()
        response.headers.asDynamic().forEach { key, value ->
            headers["$key"] = listOf("$value")
        }

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
}

private fun Int8Array.toByteArray(): ByteArray {
    val len = this.length
    return ByteArray(len) { i -> this[i] }
}
