package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.WebSocketMessage
import io.etherflow.client.kmp.WebSocketSession
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

internal class JsWebSocketSession(
    url: String,
    headers: Map<String, String>
) : WebSocketSession {

    private val ws: dynamic = js("new WebSocket(url)")
    private val openDeferred = CompletableDeferred<Unit>()

    override val incoming: Flow<WebSocketMessage> = callbackFlow {
        ws.onopen = {
            openDeferred.complete(Unit)
        }
        ws.onmessage = { event: dynamic ->
            val data: dynamic = event.data
            if (js("typeof data") == "string") {
                trySend(WebSocketMessage.Text(data as String))
            } else {
                trySend(WebSocketMessage.Binary(Int8Array(data as ArrayBuffer).toByteArray()))
            }
        }
        ws.onerror = { _: dynamic ->
            close(js("Error('WebSocket error')") as Throwable)
        }
        ws.onclose = { _: dynamic ->
            close()
        }

        awaitClose { ws.close() }
    }

    override suspend fun send(message: String) {
        openDeferred.await()
        ws.send(message)
    }

    override suspend fun send(data: ByteArray) {
        openDeferred.await()
        val buffer = ArrayBuffer(data.size)
        val view = Int8Array(buffer).asDynamic()
        for (i in data.indices) {
            view[i] = data[i]
        }
        ws.send(buffer)
    }

    override suspend fun close() {
        ws.close()
    }
}
