package io.etherflow.client.kmp

sealed class Part {
    data class FieldPart(val name: String, val value: String) : Part()
    data class FilePart(
        val name: String,
        val filename: String,
        val data: ByteArray,
        val contentType: String
    ) : Part()
}

class MultipartBuilder {
    private val parts = mutableListOf<Part>()

    fun field(name: String, value: String): MultipartBuilder = apply {
        parts.add(Part.FieldPart(name, value))
    }

    fun file(name: String, filename: String, data: ByteArray, contentType: String = "application/octet-stream"): MultipartBuilder = apply {
        parts.add(Part.FilePart(name, filename, data, contentType))
    }

    fun build(): MultipartBody = MultipartBody(parts.toList())
}

class MultipartBody internal constructor(
    val parts: List<Part>
) {
    val boundary: String by lazy { generateBoundary() }
    val contentType: String by lazy { "multipart/form-data; boundary=$boundary" }

    fun toByteArray(): ByteArray {
        val boundaryBytes = "--$boundary".encodeToByteArray()
        val crlf = "\r\n".encodeToByteArray()
        val dashes = "--".encodeToByteArray()

        val buffer = mutableListOf<Byte>()

        for (part in parts) {
            buffer.addAll(boundaryBytes.toTypedArray())
            buffer.addAll(crlf.toTypedArray())

            when (part) {
                is Part.FieldPart -> {
                    val disposition = "Content-Disposition: form-data; name=\"${part.name}\""
                    buffer.addAll(disposition.encodeToByteArray().toTypedArray())
                    buffer.addAll(crlf.toTypedArray())
                    buffer.addAll(crlf.toTypedArray())
                    buffer.addAll(part.value.encodeToByteArray().toTypedArray())
                    buffer.addAll(crlf.toTypedArray())
                }
                is Part.FilePart -> {
                    val disposition = "Content-Disposition: form-data; name=\"${part.name}\"; filename=\"${part.filename}\""
                    buffer.addAll(disposition.encodeToByteArray().toTypedArray())
                    buffer.addAll(crlf.toTypedArray())
                    val ct = "Content-Type: ${part.contentType}"
                    buffer.addAll(ct.encodeToByteArray().toTypedArray())
                    buffer.addAll(crlf.toTypedArray())
                    buffer.addAll(crlf.toTypedArray())
                    buffer.addAll(part.data.toTypedArray())
                    buffer.addAll(crlf.toTypedArray())
                }
            }
        }

        buffer.addAll(dashes.toTypedArray())
        buffer.addAll(boundaryBytes.toTypedArray())
        buffer.addAll(dashes.toTypedArray())
        buffer.addAll(crlf.toTypedArray())

        return buffer.toByteArray()
    }

    private fun generateBoundary(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder("EtherFlow-boundary-")
        repeat(32) { sb.append(chars.random()) }
        return sb.toString()
    }
}
