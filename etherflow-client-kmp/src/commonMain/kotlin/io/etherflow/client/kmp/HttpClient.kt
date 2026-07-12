package io.etherflow.client.kmp

fun httpClient(block: HttpClientConfig.() -> Unit): HttpClient = HttpClient(HttpClientConfig().apply(block))

fun createHttpClient(config: HttpClientConfig = HttpClientConfig()): HttpClient = HttpClient(config)

class HttpClient(val config: HttpClientConfig) {

    private var engine: HttpClientEngine? = null

    fun install(engine: HttpClientEngine) {
        this.engine = engine
    }

    private fun resolveEngine(): HttpClientEngine {
        return engine ?: throw IllegalStateException(
            "No engine installed. Use client.install(platformEngine()) before making requests."
        )
    }

    fun get(url: String, vararg pathParams: Any): HttpRequestBuilder {
        return request("GET", url, *pathParams)
    }

    fun post(url: String, vararg pathParams: Any): HttpRequestBuilder {
        return request("POST", url, *pathParams)
    }

    fun put(url: String, vararg pathParams: Any): HttpRequestBuilder {
        return request("PUT", url, *pathParams)
    }

    fun delete(url: String, vararg pathParams: Any): HttpRequestBuilder {
        return request("DELETE", url, *pathParams)
    }

    fun patch(url: String, vararg pathParams: Any): HttpRequestBuilder {
        return request("PATCH", url, *pathParams)
    }

    private fun request(method: String, url: String, vararg pathParams: Any): HttpRequestBuilder {
        val resolved = resolvePathParams(url, pathParams)
        return HttpRequestBuilder(resolveEngine(), method, resolved, config)
    }

    suspend fun webSocket(url: String, headers: Map<String, String> = emptyMap()): WebSocketSession {
        return resolveEngine().createWebSocket(url, headers)
    }

    private fun resolvePathParams(template: String, params: Array<out Any>): String {
        var result = template
        for (param in params) {
            val start = result.indexOf('{')
            val end = result.indexOf('}', start)
            if (start >= 0 && end > start) {
                result = result.substring(0, start) + param.toString() + result.substring(end + 1)
            }
        }
        return result
    }
}
