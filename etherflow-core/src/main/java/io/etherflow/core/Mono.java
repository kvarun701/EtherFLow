package io.etherflow.core;

import io.etherflow.streams.*;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.*;

public abstract class Mono<T> implements Publisher<T> {

    /**
     * Creates a Mono that bridges a callback-based API.
     * The emitter can call {@link MonoSink#success(Object)} or
     * throw a RuntimeException mapped to {@link MonoSink#error(Throwable)}.
     *
     * <pre>{@code
     * Mono.create(sink -> {
     *     asyncApi.call(new Callback() {
     *         void onResult(T v) { sink.success(v); }
     *         void onError(Throwable t) { sink.error(t); }
     *     });
     * });
     * }</pre>
     */
    public static <T> Mono<T> create(Consumer<MonoSink<T>> emitter) {
        Objects.requireNonNull(emitter);
        return new MonoCreate<>(emitter);
    }

    public static <T> Mono<T> just(T value) {
        Objects.requireNonNull(value);
        return new MonoJust<>(value);
    }

    public static <T> Mono<T> empty() {
        return new MonoEmpty<>();
    }

    public static <T> Mono<T> error(Throwable error) {
        Objects.requireNonNull(error);
        return new MonoError<>(error);
    }

    public static <T> Mono<T> fromCallable(Callable<? extends T> callable) {
        Objects.requireNonNull(callable);
        return new MonoCallable<>(callable);
    }

    public static <T> Mono<T> defer(Supplier<? extends Mono<? extends T>> supplier) {
        Objects.requireNonNull(supplier);
        return new MonoDefer<>(supplier);
    }

    public final <R> Mono<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return new MonoMap<>(this, mapper);
    }

    public final <R> Mono<R> flatMap(Function<? super T, ? extends Mono<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        return new MonoFlatMap<>(this, mapper);
    }

    public final Mono<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return new MonoFilter<>(this, predicate);
    }

    public final Mono<T> doOnSuccess(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        return new MonoDoOnSuccess<>(this, consumer);
    }

    public final Mono<T> doOnError(Consumer<? super Throwable> consumer) {
        Objects.requireNonNull(consumer);
        return new MonoDoOnError<>(this, consumer);
    }

    public final Mono<T> switchIfEmpty(Supplier<? extends Mono<? extends T>> supplier) {
        Objects.requireNonNull(supplier);
        return new MonoSwitchIfEmpty<>(this, supplier);
    }

    public final Mono<T> onErrorResume(Function<? super Throwable, ? extends Mono<? extends T>> fallback) {
        Objects.requireNonNull(fallback);
        return new MonoOnErrorResume<>(this, fallback);
    }

    public final Mono<Void> then() {
        return new MonoThenVoid<>(this);
    }

    public final <R> Mono<R> thenReturn(R value) {
        Objects.requireNonNull(value);
        return new MonoThenReturn<>(this, value);
    }

    public final Mono<T> subscribeOn(Executor executor) {
        Objects.requireNonNull(executor);
        return new MonoSubscribeOn<>(this, executor);
    }

    public final Mono<T> publishOn(Executor executor) {
        Objects.requireNonNull(executor);
        return new MonoPublishOn<>(this, executor);
    }

    public final T block() {
        BlockingSubscriber<T> subscriber = new BlockingSubscriber<>();
        subscribe(subscriber);
        return subscriber.blockingGet();
    }

    public final void subscribe(Consumer<? super T> onNext) {
        subscribe(new LambdaSubscriber<>(onNext, null, null, null));
    }

    public final void subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        subscribe(new LambdaSubscriber<>(onNext, onError, null, null));
    }

    public final void subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
                                Runnable onComplete) {
        subscribe(new LambdaSubscriber<>(onNext, onError, onComplete, null));
    }

    @Override
    public abstract void subscribe(Subscriber<? super T> subscriber);

    static final class MonoJust<T> extends Mono<T> {
        private final T value;

        MonoJust(T value) {
            this.value = value;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean requested;
                private boolean cancelled;

                @Override
                public void request(long n) {
                    if (cancelled || requested) return;
                    requested = true;
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }
    }

    static final class MonoEmpty<T> extends Mono<T> {
        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean requested;
                private boolean cancelled;

                @Override
                public void request(long n) {
                    if (cancelled || requested) return;
                    requested = true;
                    subscriber.onComplete();
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }
    }

    static final class MonoError<T> extends Mono<T> {
        private final Throwable error;

        MonoError(Throwable error) {
            this.error = error;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onError(error);
                }

                @Override
                public void cancel() {}
            });
        }
    }

    static final class MonoCallable<T> extends Mono<T> {
        private final Callable<? extends T> callable;

        MonoCallable(Callable<? extends T> callable) {
            this.callable = callable;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private volatile boolean cancelled;

                @Override
                public void request(long n) {
                    if (cancelled || n <= 0) return;
                    try {
                        T result = callable.call();
                        if (result == null) {
                            subscriber.onComplete();
                        } else {
                            subscriber.onNext(result);
                            subscriber.onComplete();
                        }
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }
    }

    static final class MonoDefer<T> extends Mono<T> {
        private final Supplier<? extends Mono<? extends T>> supplier;

        MonoDefer(Supplier<? extends Mono<? extends T>> supplier) {
            this.supplier = supplier;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void subscribe(Subscriber<? super T> subscriber) {
            Mono<? extends T> mono;
            try {
                mono = supplier.get();
            } catch (Exception e) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) { subscriber.onError(e); }
                    @Override
                    public void cancel() {}
                });
                return;
            }
            if (mono == null) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) { subscriber.onComplete(); }
                    @Override
                    public void cancel() {}
                });
                return;
            }
            mono.subscribe(subscriber);
        }
    }

    static final class MonoMap<T, R> extends Mono<R> {
        private final Mono<T> source;
        private final Function<? super T, ? extends R> mapper;

        MonoMap(Mono<T> source, Function<? super T, ? extends R> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override
        public void subscribe(Subscriber<? super R> subscriber) {
            source.subscribe(new MapSubscriber<>(subscriber, mapper));
        }

        static class MapSubscriber<T, R> implements Subscriber<T> {
            private final Subscriber<? super R> downstream;
            private final Function<? super T, ? extends R> mapper;

            MapSubscriber(Subscriber<? super R> downstream, Function<? super T, ? extends R> mapper) {
                this.downstream = downstream;
                this.mapper = mapper;
            }

            @Override
            public void onSubscribe(Subscription s) { downstream.onSubscribe(s); }

            @Override
            public void onNext(T item) {
                try {
                    downstream.onNext(mapper.apply(item));
                } catch (Exception e) {
                    downstream.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) { downstream.onError(t); }

            @Override
            public void onComplete() { downstream.onComplete(); }
        }
    }

    static final class MonoFlatMap<T, R> extends Mono<R> {
        private final Mono<T> source;
        private final Function<? super T, ? extends Mono<? extends R>> mapper;

        MonoFlatMap(Mono<T> source, Function<? super T, ? extends Mono<? extends R>> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override
        public void subscribe(Subscriber<? super R> subscriber) {
            source.subscribe(new FlatMapSubscriber<>(subscriber, mapper));
        }

        static class FlatMapSubscriber<T, R> implements Subscriber<T> {
            private final Subscriber<? super R> downstream;
            private final Function<? super T, ? extends Mono<? extends R>> mapper;
            private volatile boolean done;

            FlatMapSubscriber(Subscriber<? super R> downstream,
                              Function<? super T, ? extends Mono<? extends R>> mapper) {
                this.downstream = downstream;
                this.mapper = mapper;
            }

            @Override
            public void onSubscribe(Subscription s) { downstream.onSubscribe(s); }

            @Override
            @SuppressWarnings("unchecked")
            public void onNext(T item) {
                if (done) return;
                try {
                    Mono<? extends R> inner = mapper.apply(item);
                    if (inner == null) {
                        downstream.onComplete();
                        return;
                    }
                    subscribeInner(inner, downstream);
                } catch (Exception e) {
                    downstream.onError(e);
                }
            }

            private static <T> void subscribeInner(Mono<? extends T> mono, Subscriber<? super T> downstream) {
                mono.subscribe(new Subscriber<T>() {
                    @Override
                    public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }

                    @Override
                    public void onNext(T item) { downstream.onNext(item); }

                    @Override
                    public void onError(Throwable t) { downstream.onError(t); }

                    @Override
                    public void onComplete() { downstream.onComplete(); }
                });
            }

            @Override
            public void onError(Throwable t) {
                if (!done) {
                    done = true;
                    downstream.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!done) {
                    done = true;
                    downstream.onComplete();
                }
            }
        }
    }

    static final class MonoFilter<T> extends Mono<T> {
        private final Mono<T> source;
        private final Predicate<? super T> predicate;

        MonoFilter(Mono<T> source, Predicate<? super T> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new FilterSubscriber<>(subscriber, predicate));
        }

        static class FilterSubscriber<T> implements Subscriber<T> {
            private final Subscriber<? super T> downstream;
            private final Predicate<? super T> predicate;

            FilterSubscriber(Subscriber<? super T> downstream, Predicate<? super T> predicate) {
                this.downstream = downstream;
                this.predicate = predicate;
            }

            @Override
            public void onSubscribe(Subscription s) { downstream.onSubscribe(s); }

            @Override
            public void onNext(T item) {
                try {
                    if (predicate.test(item)) {
                        downstream.onNext(item);
                    } else {
                        downstream.onComplete();
                    }
                } catch (Exception e) {
                    downstream.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) { downstream.onError(t); }

            @Override
            public void onComplete() { downstream.onComplete(); }
        }
    }

    static final class MonoDoOnSuccess<T> extends Mono<T> {
        private final Mono<T> source;
        private final Consumer<? super T> consumer;

        MonoDoOnSuccess(Mono<T> source, Consumer<? super T> consumer) {
            this.source = source;
            this.consumer = consumer;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) {
                    try {
                        consumer.accept(item);
                    } catch (Exception ignored) {}
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }
    }

    static final class MonoDoOnError<T> extends Mono<T> {
        private final Mono<T> source;
        private final Consumer<? super Throwable> consumer;

        MonoDoOnError(Mono<T> source, Consumer<? super Throwable> consumer) {
            this.source = source;
            this.consumer = consumer;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) { subscriber.onNext(item); }

                @Override
                public void onError(Throwable t) {
                    try {
                        consumer.accept(t);
                    } catch (Exception ignored) {}
                    subscriber.onError(t);
                }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }
    }

    static final class MonoSubscribeOn<T> extends Mono<T> {
        private final Mono<T> source;
        private final Executor executor;

        MonoSubscribeOn(Mono<T> source, Executor executor) {
            this.source = source;
            this.executor = executor;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            executor.execute(() -> source.subscribe(subscriber));
        }
    }

    static final class MonoPublishOn<T> extends Mono<T> {
        private final Mono<T> source;
        private final Executor executor;

        MonoPublishOn(Mono<T> source, Executor executor) {
            this.source = source;
            this.executor = executor;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) { executor.execute(() -> subscriber.onNext(item)); }

                @Override
                public void onError(Throwable t) { executor.execute(() -> subscriber.onError(t)); }

                @Override
                public void onComplete() { executor.execute(subscriber::onComplete); }
            });
        }
    }

    static final class LambdaSubscriber<T> implements Subscriber<T> {
        private final Consumer<? super T> onNext;
        private final Consumer<? super Throwable> onError;
        private final Runnable onComplete;
        private final SubscriptionConsumer onSubscribe;

        LambdaSubscriber(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
                         Runnable onComplete, SubscriptionConsumer onSubscribe) {
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onSubscribe = onSubscribe;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (onSubscribe != null) onSubscribe.accept(s);
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T item) {
            if (onNext != null) onNext.accept(item);
        }

        @Override
        public void onError(Throwable t) {
            if (onError != null) onError.accept(t);
        }

        @Override
        public void onComplete() {
            if (onComplete != null) onComplete.run();
        }
    }

    @FunctionalInterface
    interface SubscriptionConsumer {
        void accept(Subscription subscription);
    }

    static final class MonoSwitchIfEmpty<T> extends Mono<T> {
        private final Mono<T> source;
        private final Supplier<? extends Mono<? extends T>> supplier;

        MonoSwitchIfEmpty(Mono<T> source, Supplier<? extends Mono<? extends T>> supplier) {
            this.source = source;
            this.supplier = supplier;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new Subscriber<>() {
                private boolean empty = true;

                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) {
                    empty = false;
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() {
                    if (empty) {
                        Mono<? extends T> fallback;
                        try {
                            fallback = supplier.get();
                        } catch (Exception e) {
                            subscriber.onError(e);
                            return;
                        }
                        if (fallback == null) {
                            subscriber.onComplete();
                        } else {
                            subscribeFallback(fallback, subscriber);
                        }
                    } else {
                        subscriber.onComplete();
                    }
                }
            });
        }

        private static <T> void subscribeFallback(Mono<? extends T> fallback, Subscriber<? super T> downstream) {
            fallback.subscribe(new Subscriber<T>() {
                @Override
                public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }

                @Override
                public void onNext(T item) { downstream.onNext(item); }

                @Override
                public void onError(Throwable t) { downstream.onError(t); }

                @Override
                public void onComplete() { downstream.onComplete(); }
            });
        }
    }

    static final class MonoThenVoid<T> extends Mono<Void> {
        private final Mono<T> source;

        MonoThenVoid(Mono<T> source) {
            this.source = source;
        }

        @Override
        public void subscribe(Subscriber<? super Void> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) {}

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }
    }

    static final class MonoThenReturn<T, R> extends Mono<R> {
        private final Mono<T> source;
        private final R value;

        @SuppressWarnings("unchecked")
        MonoThenReturn(Mono<T> source, R value) {
            this.source = source;
            this.value = value;
        }

        @Override
        public void subscribe(Subscriber<? super R> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) {}

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() {
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            });
        }
    }

    static final class MonoOnErrorResume<T> extends Mono<T> {
        private final Mono<T> source;
        private final Function<? super Throwable, ? extends Mono<? extends T>> fallback;

        MonoOnErrorResume(Mono<T> source, Function<? super Throwable, ? extends Mono<? extends T>> fallback) {
            this.source = source;
            this.fallback = fallback;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) { subscriber.onNext(item); }

                @Override
                @SuppressWarnings("unchecked")
                public void onError(Throwable t) {
                    Mono<? extends T> fallbackMono;
                    try {
                        fallbackMono = fallback.apply(t);
                    } catch (Exception e) {
                        subscriber.onError(e);
                        return;
                    }
                    if (fallbackMono == null) {
                        subscriber.onComplete();
                    } else {
                        fallbackMono.subscribe(new Subscriber<T>() {
                            @Override
                            public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }
                            @Override
                            public void onNext(T item) { subscriber.onNext(item); }
                            @Override
                            public void onError(Throwable t) { subscriber.onError(t); }
                            @Override
                            public void onComplete() { subscriber.onComplete(); }
                        });
                    }
                }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }
    }

    static final class MonoCreate<T> extends Mono<T> {
        private final Consumer<MonoSink<T>> emitter;

        MonoCreate(Consumer<MonoSink<T>> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            MonoSink<T> sink = new MonoSink<T>() {
                private volatile boolean done;

                @Override
                public void success(T value) {
                    if (done) return;
                    done = true;
                    if (value != null) {
                        subscriber.onNext(value);
                    }
                    subscriber.onComplete();
                }

                @Override
                public void error(Throwable t) {
                    if (done) return;
                    done = true;
                    subscriber.onError(t);
                }
            };

            subscriber.onSubscribe(new Subscription() {
                private volatile boolean cancelled;

                @Override
                public void request(long n) {
                    if (cancelled || n <= 0) return;
                    try {
                        emitter.accept(sink);
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }
    }

    static final class BlockingSubscriber<T> implements Subscriber<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile T value;
        private volatile Throwable error;

        @Override
        public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }

        @Override
        public void onNext(T item) { value = item; }

        @Override
        public void onError(Throwable t) {
            error = t;
            latch.countDown();
        }

        @Override
        public void onComplete() { latch.countDown(); }

        T blockingGet() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (error != null) {
                throw new RuntimeException(error);
            }
            return value;
        }
    }
}
