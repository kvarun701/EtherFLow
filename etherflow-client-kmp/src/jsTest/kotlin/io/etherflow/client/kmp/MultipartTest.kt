package io.etherflow.client.kmp

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

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
    fun `MultipartBody encodes fields and files together`() {
        val body = MultipartBuilder()
            .field("description", "my photo")
            .file("file", "img.png", "pngdata".encodeToByteArray())
            .build()

        val bodyStr = body.toByteArray().decodeToString()
        assertTrue(bodyStr.contains("description"))
        assertTrue(bodyStr.contains("my photo"))
        assertTrue(bodyStr.contains("img.png"))
        assertTrue(bodyStr.contains("pngdata"))
        assertTrue(bodyStr.contains("--${body.boundary}--"))
    }

    @Test
    fun `writeToFile throws on JS`() {
        val data = "test".encodeToByteArray()
        var threw = false
        try {
            data.writeToFile("/tmp/test.txt")
        } catch (e: UnsupportedOperationException) {
            threw = true
        }
        assertTrue(threw)
    }
}
