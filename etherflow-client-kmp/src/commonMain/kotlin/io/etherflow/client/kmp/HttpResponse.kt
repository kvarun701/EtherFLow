package io.etherflow.client.kmp

data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: ByteArray = ByteArray(0)
) {
    val bodyAsString: String get() = body.decodeToString()
    val bodyAsBytes: ByteArray get() = body

    val isSuccess: Boolean get() = statusCode in 200..299
    val isError: Boolean get() = !isSuccess

    val contentLength: Long get() = body.size.toLong()

    fun header(name: String): String? = headers[name]?.firstOrNull()
}
