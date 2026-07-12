package io.etherflow.client.kmp

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class HttpClientConfig {
    var baseUrl: String = ""
    var retryCount: Int = 3
    var connectTimeout: Duration = 10.seconds
    var readTimeout: Duration = 30.seconds
    var defaultHeaders: Map<String, String> = emptyMap()
}
