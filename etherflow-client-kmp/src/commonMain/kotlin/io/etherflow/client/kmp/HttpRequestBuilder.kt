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

    fun multipart(block: MultipartBuilder.() -> Unit): HttpRequestBuilder = apply {
        val multipart = MultipartBuilder().apply(block).build()
        contentType(multipart.contentType)
        bodyBytes = multipart.toByteArray()
    }

    suspend fun execute(): HttpResponse {
        val fullUrl = resolveUrl()
        val request = HttpRequest(method, fullUrl, headers.toMap(), bodyBytes)
        return engine.execute(request)
    }

    suspend fun body(): HttpResponse = execute()

    suspend fun bodyJson(json: String): HttpResponse {
        if (!headers.containsKey("Content-Type")) {
            headers["Content-Type"] = "application/json"
        }
        bodyBytes = json.encodeToByteArray()
        return execute()
    }

    suspend fun bodyText(text: String): HttpResponse {
        if (!headers.containsKey("Content-Type")) {
            headers["Content-Type"] = "text/plain"
        }
        bodyBytes = text.encodeToByteArray()
        return execute()
    }

    suspend fun bodyBytes(data: ByteArray): HttpResponse {
        bodyBytes = data
        return execute()
    }

    suspend inline fun <reified T> body(value: T): HttpResponse {
        if (!headers.containsKey("Content-Type")) {
            headers["Content-Type"] = "application/json"
        }
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        bodyBytes = json.encodeToString(serializer<T>(), value).encodeToByteArray()
        return execute()
    }

    suspend inline fun <reified T> bodyAs(): T {
        val response = execute()
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString(serializer<T>(), response.bodyAsString)
    }

    suspend fun bodyAsBytes(): ByteArray {
        val response = execute()
        return response.body
    }

    suspend fun downloadTo(filePath: String): Long {
        val response = execute()
        response.body.writeToFile(filePath)
        return response.body.size.toLong()
    }

    suspend fun stream(): StreamedResponse {
        val fullUrl = resolveUrl()
        val request = HttpRequest(method, fullUrl, headers.toMap(), bodyBytes)
        return engine.executeStreaming(request)
    }

    private fun resolveUrl(): String {
        var url = if (urlTemplate.startsWith("http")) urlTemplate
                  else config.baseUrl.trimEnd('/') + "/" + urlTemplate.trimStart('/')
        return url
    }
}
