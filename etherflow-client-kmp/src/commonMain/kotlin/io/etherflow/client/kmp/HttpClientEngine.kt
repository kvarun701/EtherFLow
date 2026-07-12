package io.etherflow.client.kmp

import kotlinx.coroutines.flow.flowOf

interface HttpClientEngine {
    suspend fun execute(request: HttpRequest): HttpResponse

    suspend fun executeStreaming(request: HttpRequest): StreamedResponse {
        val response = execute(request)
        return StreamedResponse(
            statusCode = response.statusCode,
            statusText = response.statusText,
            headers = response.headers,
            contentLength = response.body.size.toLong(),
            chunks = flowOf(response.body)
        )
    }
}
