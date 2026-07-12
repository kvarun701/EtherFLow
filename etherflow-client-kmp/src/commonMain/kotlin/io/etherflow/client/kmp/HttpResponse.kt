package io.etherflow.client.kmp

data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: ByteArray = ByteArray(0)
) {
    val bodyAsString: String get() = body.decodeToString()

    val isSuccess: Boolean get() = statusCode in 200..299
    val isError: Boolean get() = !isSuccess

    fun header(name: String): String? = headers[name]?.firstOrNull()
}
