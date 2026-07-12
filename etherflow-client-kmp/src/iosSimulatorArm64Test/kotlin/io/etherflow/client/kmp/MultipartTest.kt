@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.etherflow.client.kmp

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

class MultipartTest {

    @Test
    fun `MultipartBody produces correct boundary header`() {
        val body = MultipartBuilder()
            .field("name", "Alice")
            .build()

        assertTrue(body.contentType.startsWith("multipart/form-data; boundary="))
        assertTrue(body.boundary.isNotEmpty())
    }

    @Test
    fun `MultipartBody encodes fields`() {
        val body = MultipartBuilder()
            .field("user", "bob")
            .field("action", "upload")
            .build()

        val bodyStr = body.toByteArray().decodeToString()
        assertTrue(bodyStr.contains("Content-Disposition: form-data; name=\"user\""))
        assertTrue(bodyStr.contains("Content-Disposition: form-data; name=\"action\""))
        assertTrue(bodyStr.contains("bob"))
        assertTrue(bodyStr.contains("upload"))
    }

    @Test
    fun `MultipartBody encodes files`() {
        val fileData = "image-data".encodeToByteArray()
        val body = MultipartBuilder()
            .file("avatar", "photo.jpg", fileData, "image/jpeg")
            .build()

        val bodyStr = body.toByteArray().decodeToString()
        assertTrue(bodyStr.contains("Content-Disposition: form-data; name=\"avatar\"; filename=\"photo.jpg\""))
        assertTrue(bodyStr.contains("Content-Type: image/jpeg"))
        assertTrue(bodyStr.contains("image-data"))
    }

    @Test
    fun `writeToFile creates file on iOS`() {
        val data = "Hello iOS from EtherFlow!".encodeToByteArray()
        val path = NSTemporaryDirectory() + "/etherflow-test-ios.txt"
        data.writeToFile(path)
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(path))
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}
