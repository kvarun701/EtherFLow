@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.HttpClientEngine
import io.etherflow.client.kmp.HttpRequest
import io.etherflow.client.kmp.HttpResponse
import io.etherflow.client.kmp.WebSocketSession
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun platformEngine(): HttpClientEngine = IosEngine

internal object IosEngine : HttpClientEngine {

    override suspend fun execute(request: HttpRequest): HttpResponse = suspendCancellableCoroutine { cont ->
        val nsUrl = NSURL(string = request.url)
        if (nsUrl == null) {
            cont.resumeWithException(Exception("Invalid URL: ${request.url}"))
            return@suspendCancellableCoroutine
        }

        val nsRequest = NSMutableURLRequest(nsUrl).apply {
            setHTTPMethod(request.method)
            for ((key, value) in request.headers) {
                setValue(value, forHTTPHeaderField = key)
            }
            val bodyBytes = request.body
            if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                setHTTPBody(bodyBytes.toNSData())
            }
        }

        val task = NSURLSession.sharedSession.dataTaskWithRequest(nsRequest) { data, response, error ->
            if (error != null) {
                cont.resumeWithException(Exception(error.localizedDescription))
                return@dataTaskWithRequest
            }

            val httpResponse = response as? NSHTTPURLResponse
            val statusCode = httpResponse?.statusCode?.toInt() ?: 0
            val statusText = httpResponse?.let {
                NSHTTPURLResponse.localizedStringForStatusCode(it.statusCode)
            } ?: ""

            val headers = mutableMapOf<String, List<String>>()
            httpResponse?.allHeaderFields?.forEach { (key, value) ->
                headers[key.toString()] = listOf(value.toString())
            }

            val body = if (data != null && data.length > 0uL) {
                data.toByteArray()
            } else ByteArray(0)

            cont.resume(HttpResponse(statusCode, statusText, headers, body))
        }
        task.resume()
    }

    override suspend fun createWebSocket(url: String, headers: Map<String, String>): WebSocketSession {
        return IosWebSocketSession(url, headers)
    }
}

internal fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}
