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
}
