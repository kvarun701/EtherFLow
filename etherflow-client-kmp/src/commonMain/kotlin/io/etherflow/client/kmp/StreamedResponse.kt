package io.etherflow.client.kmp

import kotlinx.coroutines.flow.Flow

class StreamedResponse(
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, List<String>>,
    val contentLength: Long,
    val chunks: Flow<ByteArray>
) {
    val isSuccess: Boolean get() = statusCode in 200..299

    fun header(name: String): String? = headers[name]?.firstOrNull()
}
