package io.etherflow.client.kmp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class HttpRequestBuilder internal constructor(
    @PublishedApi internal val engine: HttpClientEngine,
    val method: String,
    @PublishedApi internal val urlTemplate: String,
    @PublishedApi internal val config: HttpClientConfig
) {
    @PublishedApi internal val headers: MutableMap<String, String> = config.defaultHeaders.toMutableMap()
    @PublishedApi internal var bodyBytes: ByteArray? = null

    fun header(name: String, value: String): HttpRequestBuilder = apply {
        headers[name] = value
    }

    fun bearerAuth(token: String): HttpRequestBuilder = apply {
        headers["Authorization"] = "Bearer $token"
    }

    fun contentType(type: String): HttpRequestBuilder = apply {
        headers["Content-Type"] = type
    }

    suspend fun body(): HttpResponse {
        val fullUrl = resolveUrl()
        val request = HttpRequest(method, fullUrl, headers.toMap(), bodyBytes)
        return engine.execute(request)
    }

    suspend inline fun <reified T> body(value: T): HttpResponse {
        headers.putIfAbsent("Content-Type", "application/json")
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        bodyBytes = json.encodeToString(serializer<T>(), value).encodeToByteArray()
        return body()
    }

    suspend inline fun <reified T> bodyAs(): T {
        val response = body()
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString(serializer<T>(), response.bodyAsString)
    }

    private fun resolveUrl(): String {
        var url = if (urlTemplate.startsWith("http")) urlTemplate
                  else config.baseUrl.trimEnd('/') + "/" + urlTemplate.trimStart('/')
        return url
    }
}
