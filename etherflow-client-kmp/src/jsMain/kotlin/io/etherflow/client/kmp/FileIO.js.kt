package io.etherflow.client.kmp

actual fun ByteArray.writeToFile(path: String) {
    throw UnsupportedOperationException(
        "writeToFile is not supported on JS. Use browser APIs (Blob, URL.createObjectURL) instead."
    )
}
