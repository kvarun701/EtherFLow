package io.etherflow.client.kmp

import io.etherflow.client.kmp.internal.platformEngine

fun platformHttpClient(config: HttpClientConfig.() -> Unit = {}): HttpClient {
    val client = createHttpClient(HttpClientConfig().apply(config))
    client.install(platformEngine())
    return client
}
