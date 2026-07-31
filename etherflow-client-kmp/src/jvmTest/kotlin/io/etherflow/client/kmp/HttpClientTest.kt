package io.etherflow.client.kmp

import io.etherflow.client.kmp.internal.platformEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

@Serializable
data class Post(var userId: Int = 0, var id: Int = 0, var title: String = "", var body: String = "")

class HttpClientTest {

    @Test
    fun `GET request returns response`() = runBlocking {
        val client = httpClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
            retryCount = 2
        }
        client.install(platformEngine())

        val response = client.get("/posts/1").body()
        assertTrue(response.isSuccess)
        assertNotNull(response.bodyAsString)
    }

    @Test
    fun `GET request deserializes to data class`() = runBlocking {
        val client = httpClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
        }
        client.install(platformEngine())

        val post: Post = client.get("/posts/1").bodyAs<Post>()
        assertNotNull(post.title)
        assertTrue(post.id == 1)
    }

    @Test
    fun `MultipartBody produces correct boundary header`() {
        val body = MultipartBuilder()
            .field("name", "Alice")
            .build()

        assertTrue(body.contentType.startsWith("multipart/form-data; boundary="))
        assertTrue(body.boundary.isNotEmpty())
    }

    @Test
    fun `multipart DSL sets correct headers and body on request builder`() {
        val client = httpClient { baseUrl = "http://localhost" }
        client.install(platformEngine())
        val builder = client.post("/test")
            .multipart {
                field("user", "alice")
                file("doc", "file.txt", "content".encodeToByteArray())
            }

        val ct = builder.headers["Content-Type"] ?: ""
        assertTrue(ct.startsWith("multipart/form-data; boundary="))
        assertNotNull(builder.bodyBytes)
        val body = builder.bodyBytes!!.decodeToString()
        assertTrue(body.contains("alice"))
        assertTrue(body.contains("file.txt"))
    }

    @Test
    fun `MultipartBody encodes fields and files`() {
        val fileData = "image-data".encodeToByteArray()
        val body = MultipartBuilder()
            .field("user", "bob")
            .field("action", "upload")
            .file("avatar", "photo.jpg", fileData, "image/jpeg")
            .build()

        val bytes = body.toByteArray()
        val bodyStr = bytes.decodeToString()

        assertTrue(bodyStr.contains("Content-Disposition: form-data; name=\"user\""))
        assertTrue(bodyStr.contains("Content-Disposition: form-data; name=\"action\""))
        assertTrue(bodyStr.contains("Content-Disposition: form-data; name=\"avatar\"; filename=\"photo.jpg\""))
        assertTrue(bodyStr.contains("Content-Type: image/jpeg"))
        assertTrue(bodyStr.contains("image-data"))
        assertTrue(bodyStr.contains("--${body.boundary}--"))
    }

    @Test
    fun `bodyAsBytes returns raw binary data`() = runBlocking {
        val client = httpClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
        }
        client.install(platformEngine())

        val bytes = client.get("/posts/1").bodyAsBytes()
        assertTrue(bytes.isNotEmpty())
        assertTrue(bytes.decodeToString().contains("userId"))
    }

    @Test
    fun `writeToFile writes bytes to disk`() {
        val data = "Hello EtherFlow!".encodeToByteArray()
        val file = File.createTempFile("etherflow-test-", ".txt")
        try {
            data.writeToFile(file.absolutePath)
            assertTrue(file.exists())
            assertEquals("Hello EtherFlow!", file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `downloadTo saves response to file`() = runBlocking {
        val client = httpClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
        }
        client.install(platformEngine())

        val file = File.createTempFile("etherflow-download-", ".json")
        try {
            val size = client.get("/posts/1").downloadTo(file.absolutePath)
            assertTrue(size > 0)
            assertTrue(file.exists())
            assertTrue(file.length() > 0)
            assertTrue(file.readText().contains("userId"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `contentLength returns body size`() = runBlocking {
        val client = httpClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
        }
        client.install(platformEngine())

        val response = client.get("/posts/1").body()
        assertTrue(response.contentLength > 0)
        assertEquals(response.body.size.toLong(), response.contentLength)
    }

    @Test
    fun `stream returns streamed response with status code`() = runBlocking {
        val client = httpClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
        }
        client.install(platformEngine())

        val streamed = client.get("/posts/1").stream()
        assertTrue(streamed.isSuccess)
        assertEquals(200, streamed.statusCode)
    }

    @Test
    fun `stream emits all chunks that reassemble the body`() = runBlocking {
        val client = httpClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
        }
        client.install(platformEngine())

        val buffered = client.get("/posts/1").body()
        val streamed = client.get("/posts/1").stream()

        val chunks = streamed.chunks.toList()
        assertTrue(chunks.isNotEmpty())

        var total = 0
        for (c in chunks) total += c.size
        val assembled = ByteArray(total)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(assembled, offset)
            offset += chunk.size
        }
        assertArrayEquals(buffered.body, assembled)
    }
}
