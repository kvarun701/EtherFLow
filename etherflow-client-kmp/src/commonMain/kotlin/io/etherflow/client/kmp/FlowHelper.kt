package io.etherflow.client.kmp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowCollector<T>(private val flow: Flow<T>) {
    fun collect(
        onEach: (T) -> Unit,
        onCompletion: (Throwable?) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                flow.collect { onEach(it) }
                onCompletion(null)
            } catch (e: Throwable) {
                onCompletion(e)
            }
        }
    }
}

fun <T> Flow<T>.asHelper(): FlowCollector<T> = FlowCollector(this)
