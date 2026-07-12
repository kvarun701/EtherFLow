@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.etherflow.client.kmp.internal

import io.etherflow.client.kmp.WebSocketMessage
import io.etherflow.client.kmp.WebSocketSession
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class IosWebSocketSession(
    url: String,
    headers: Map<String, String>
) : WebSocketSession {

    private val session = NSURLSession.sharedSession
    private val nsUrl = NSURL(string = url)!!
    private val nsRequest = NSMutableURLRequest(nsUrl).apply {
        for ((key, value) in headers) {
            setValue(value, forHTTPHeaderField = key)
        }
    }
    private val task: NSURLSessionWebSocketTask = session.webSocketTaskWithRequest(nsRequest)

    override val incoming: Flow<WebSocketMessage> = callbackFlow {
        task.resume()

        fun receiveNext() {
            task.receiveMessageWithCompletionHandler { message, error ->
                if (error != null) {
                    close(Exception(error.localizedDescription))
                    return@receiveMessageWithCompletionHandler
                }
                if (message != null) {
                    if (message.string() != null) {
                        trySend(WebSocketMessage.Text(message.string()!!))
                    } else if (message.data() != null) {
                        trySend(WebSocketMessage.Binary(message.data()!!.toByteArray()))
                    }
                    receiveNext()
                }
            }
        }
        receiveNext()

        awaitClose {
            task.cancelWithCloseCode(1000L, null)
        }
    }

    override suspend fun send(message: String) = suspendCancellableCoroutine { cont ->
        val wsMessage = NSURLSessionWebSocketMessage(message)
        task.sendMessage(wsMessage) { error ->
            if (error != null) {
                cont.resumeWithException(Exception(error.localizedDescription))
            } else {
                cont.resume(Unit)
            }
        }
    }

    override suspend fun send(data: ByteArray) = suspendCancellableCoroutine { cont ->
        val nsData = data.toNSData()
        val wsMessage = NSURLSessionWebSocketMessage(nsData)
        task.sendMessage(wsMessage) { error ->
            if (error != null) {
                cont.resumeWithException(Exception(error.localizedDescription))
            } else {
                cont.resume(Unit)
            }
        }
    }

    override suspend fun close() {
        task.cancelWithCloseCode(1000L, null)
    }
}
