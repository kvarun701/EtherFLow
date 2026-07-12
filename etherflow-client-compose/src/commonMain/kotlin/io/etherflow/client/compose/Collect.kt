package io.etherflow.client.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.etherflow.client.kmp.HttpClient
import io.etherflow.client.kmp.HttpClientConfig
import io.etherflow.client.kmp.HttpRequestBuilder
import io.etherflow.client.kmp.platformHttpClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

sealed class HttpRequestState<out T> {
    object Loading : HttpRequestState<Nothing>()
    data class Success<T>(val data: T) : HttpRequestState<T>()
    data class Error(val exception: Throwable) : HttpRequestState<Nothing>()
}

@Composable
fun rememberHttpClient(config: HttpClientConfig.() -> Unit = {}): HttpClient {
    return remember {
        platformHttpClient(config)
    }
}

@Composable
fun <T> produceHttpState(
    key: Any? = Unit,
    fetch: suspend () -> T
): State<HttpRequestState<T>> {
    val state = remember { mutableStateOf<HttpRequestState<T>>(HttpRequestState.Loading) }
    LaunchedEffect(key) {
        try {
            state.value = HttpRequestState.Success(fetch())
        } catch (e: Throwable) {
            state.value = HttpRequestState.Error(e)
        }
    }
    return state
}

@Composable
fun <T> httpGetAs(
    client: HttpClient,
    url: String,
    vararg pathParams: Any,
    key: Any? = url,
    serializer: KSerializer<T>,
    builder: HttpRequestBuilder.() -> Unit = {}
): State<HttpRequestState<T>> = produceHttpState(key) {
    val json = Json { ignoreUnknownKeys = true }
    client.get(url, *pathParams).apply(builder).execute().let { response ->
        json.decodeFromString(serializer, response.bodyAsString)
    }
}

@Composable
fun <T> httpPostAs(
    client: HttpClient,
    url: String,
    vararg pathParams: Any,
    key: Any? = url,
    serializer: KSerializer<T>,
    builder: HttpRequestBuilder.() -> Unit
): State<HttpRequestState<T>> = produceHttpState(key) {
    val json = Json { ignoreUnknownKeys = true }
    client.post(url, *pathParams).apply(builder).execute().let { response ->
        json.decodeFromString(serializer, response.bodyAsString)
    }
}
