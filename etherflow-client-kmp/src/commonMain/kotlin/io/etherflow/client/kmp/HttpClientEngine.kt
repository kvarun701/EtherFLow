package io.etherflow.client.kmp

interface HttpClientEngine {
    suspend fun execute(request: HttpRequest): HttpResponse
}
