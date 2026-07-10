package io.etherflow.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class FluxTest {

    @Test
    void just() {
        List<String> items = new ArrayList<>();
        Flux.just("a", "b", "c").subscribe(items::add);
        assertEquals(List.of("a", "b", "c"), items);
    }

    @Test
    void fromIterable() {
        List<String> items = new ArrayList<>();
        Flux.fromIterable(List.of("x", "y")).subscribe(items::add);
        assertEquals(List.of("x", "y"), items);
    }

    @Test
    void range() {
        List<Integer> items = new ArrayList<>();
        Flux.range(1, 5).subscribe(items::add);
        assertEquals(List.of(1, 2, 3, 4, 5), items);
    }

    @Test
    void map() {
        List<String> items = new ArrayList<>();
        Flux.just(1, 2, 3).map(i -> "n" + i).subscribe(items::add);
        assertEquals(List.of("n1", "n2", "n3"), items);
    }

    @Test
    void filter() {
        List<Integer> items = new ArrayList<>();
        Flux.range(1, 5).filter(i -> i % 2 == 0).subscribe(items::add);
        assertEquals(List.of(2, 4), items);
    }

    @Test
    void empty() {
        List<Integer> items = new ArrayList<>();
        Flux.<Integer>empty().subscribe(items::add);
        assertTrue(items.isEmpty());
    }

    @Test
    void completed() {
        AtomicInteger completions = new AtomicInteger();
        Flux.just("a").subscribe(v -> {}, e -> {}, completions::incrementAndGet);
        assertEquals(1, completions.get());
    }

    @Test
    void flatMap() {
        List<String> items = new ArrayList<>();
        Flux.just("a", "b")
                .flatMap(s -> Flux.just(s + "1", s + "2"))
                .subscribe(items::add);
        assertEquals(List.of("a1", "a2", "b1", "b2"), items);
    }

    @Test
    void subscribeOnNext() {
        List<String> items = new ArrayList<>();
        Flux.just("a", "b").subscribe(items::add);
        assertEquals(List.of("a", "b"), items);
    }

    @Test
    void thenMono() {
        Void result = Flux.just("a", "b").then().block();
        assertNull(result);
    }
}
