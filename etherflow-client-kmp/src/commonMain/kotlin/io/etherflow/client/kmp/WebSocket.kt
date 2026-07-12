package io.etherflow.client.kmp

import kotlinx.coroutines.flow.Flow

sealed class WebSocketMessage {
    data class Text(val text: String) : WebSocketMessage()
    data class Binary(val data: ByteArray) : WebSocketMessage()
}

interface WebSocketSession {
    val incoming: Flow<WebSocketMessage>
    suspend fun send(message: String)
    suspend fun send(data: ByteArray)
    suspend fun close()
}
