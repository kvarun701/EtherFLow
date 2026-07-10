package io.etherflow.core;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class MonoTest {

    @Test
    void just() {
        assertEquals("hello", Mono.just("hello").block());
    }

    @Test
    void empty() {
        assertNull(Mono.empty().block());
    }

    @Test
    void map() {
        String result = Mono.just(42)
                .map(i -> "number: " + i)
                .block();
        assertEquals("number: 42", result);
    }

    @Test
    void flatMap() {
        String result = Mono.just("hello")
                .flatMap(s -> Mono.just(s + " world"))
                .block();
        assertEquals("hello world", result);
    }

    @Test
    void filter_accepts() {
        String result = Mono.just("hello")
                .filter(s -> s.length() > 2)
                .block();
        assertEquals("hello", result);
    }

    @Test
    void filter_rejects() {
        String result = Mono.just("ab")
                .filter(s -> s.length() > 5)
                .block();
        assertNull(result);
    }

    @Test
    void doOnSuccess() {
        AtomicReference<String> seen = new AtomicReference<>();
        String result = Mono.just("hello")
                .doOnSuccess(seen::set)
                .block();
        assertEquals("hello", result);
        assertEquals("hello", seen.get());
    }

    @Test
    void switchIfEmpty_fallback() {
        String result = Mono.<String>empty()
                .switchIfEmpty(() -> Mono.just("fallback"))
                .block();
        assertEquals("fallback", result);
    }

    @Test
    void switchIfEmpty_noFallback() {
        String result = Mono.just("original")
                .switchIfEmpty(() -> Mono.just("fallback"))
                .block();
        assertEquals("original", result);
    }

    @Test
    void thenReturn() {
        String result = Mono.just("ignored")
                .thenReturn("final")
                .block();
        assertEquals("final", result);
    }

    @Test
    void error() {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Mono.error(new RuntimeException("boom"))
                .subscribe(v -> {}, captured::set);
        assertNotNull(captured.get());
        assertEquals("boom", captured.get().getMessage());
    }

    @Test
    void fromCallable() {
        String result = Mono.fromCallable(() -> "called").block();
        assertEquals("called", result);
    }

    @Test
    void defer() {
        String result = Mono.defer(() -> Mono.just("deferred")).block();
        assertEquals("deferred", result);
    }

    @Test
    void subscribe_onNext() {
        AtomicReference<String> captured = new AtomicReference<>();
        Mono.just("hello").subscribe(captured::set);
        assertEquals("hello", captured.get());
    }

    @Test
    void block_throwsOnError() {
        assertThrows(RuntimeException.class, () ->
                Mono.error(new RuntimeException("fail")).block());
    }
}
