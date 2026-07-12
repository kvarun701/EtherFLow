@file:Suppress("DEPRECATION")
package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.WebSocketMessage
import io.etherflow.client.kmp.WebSocketSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okio.ByteString

internal class JvmWebSocketSession(
    url: String,
    headers: Map<String, String>,
    client: OkHttpClient
) : WebSocketSession {

    private val wsDeferred = CompletableDeferred<WebSocket>()

    override val incoming: Flow<WebSocketMessage> = callbackFlow {
        val reqBuilder = Request.Builder().url(url)
        for ((name, value) in headers) {
            reqBuilder.addHeader(name, value)
        }

        val ws = client.newWebSocket(reqBuilder.build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(WebSocketMessage.Text(text))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                trySend(WebSocketMessage.Binary(bytes.toByteArray()))
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        })
        wsDeferred.complete(ws)
        awaitClose { ws.close(1000, "client closing") }
    }

    override suspend fun send(message: String) {
        wsDeferred.await().send(message)
    }

    override suspend fun send(data: ByteArray) {
        wsDeferred.await().send(ByteString.of(*data))
    }

    override suspend fun close() {
        wsDeferred.await().close(1000, "client closing")
    }
}
