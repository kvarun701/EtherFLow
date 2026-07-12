package io.etherflow.client.kmp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun ByteArray.writeToFile(path: String) {
    usePinned { pinned ->
        val nsData = NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        nsData.writeToFile(path, atomically = true)
    }
}
