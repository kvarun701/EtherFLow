package io.etherflow.client.kmp

import java.io.File
import java.io.FileOutputStream

actual fun ByteArray.writeToFile(path: String) {
    val file = File(path)
    file.parentFile?.mkdirs()
    FileOutputStream(file).use { it.write(this) }
}
