package io.etherflow.client.kmp

import io.etherflow.client.kmp.internal.platformEngine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@Serializable
data class Post(val userId: Int, val id: Int, val title: String, val body: String)

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

}
