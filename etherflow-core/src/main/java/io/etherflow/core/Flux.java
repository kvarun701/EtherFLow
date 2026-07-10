package io.etherflow.core;

import io.etherflow.streams.*;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.*;

public abstract class Flux<T> implements Publisher<T> {

    @SafeVarargs
    public static <T> Flux<T> just(T... values) {
        Objects.requireNonNull(values);
        return new FluxArray<>(values);
    }

    public static <T> Flux<T> fromIterable(Iterable<? extends T> iterable) {
        Objects.requireNonNull(iterable);
        return new FluxIterable<>(iterable);
    }

    public static <T> Flux<T> empty() {
        return new FluxEmpty<>();
    }

    public static <T> Flux<T> error(Throwable error) {
        Objects.requireNonNull(error);
        return new FluxError<>(error);
    }

    public static Flux<Integer> range(int start, int count) {
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");
        return new FluxRange(start, count);
    }

    public final <R> Flux<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return new FluxMap<>(this, mapper);
    }

    public final <R> Flux<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        return new FluxFlatMap<>(this, mapper);
    }

    public final Flux<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return new FluxFilter<>(this, predicate);
    }

    public final Flux<T> subscribeOn(Executor executor) {
        Objects.requireNonNull(executor);
        return new FluxSubscribeOn<>(this, executor);
    }

    public final Flux<T> publishOn(Executor executor) {
        Objects.requireNonNull(executor);
        return new FluxPublishOn<>(this, executor);
    }

    public final Mono<Void> then() {
        return new MonoFromFluxVoid(this);
    }

    @Override
    public abstract void subscribe(Subscriber<? super T> subscriber);

    public final void subscribe(Consumer<? super T> onNext) {
        subscribe(new FluxLambdaSubscriber<>(onNext, null, null));
    }

    public final void subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        subscribe(new FluxLambdaSubscriber<>(onNext, onError, null));
    }

    public final void subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
                                Runnable onComplete) {
        subscribe(new FluxLambdaSubscriber<>(onNext, onError, onComplete));
    }

    static final class FluxArray<T> extends Flux<T> {
        private final T[] array;

        @SafeVarargs
        FluxArray(T... array) {
            this.array = array;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean cancelled;
                private int index;

                @Override
                public void request(long n) {
                    if (cancelled || n <= 0) return;
                    long remaining = n;
                    while (remaining > 0 && index < array.length && !cancelled) {
                        subscriber.onNext(array[index++]);
                        remaining--;
                    }
                    if (index >= array.length && !cancelled) {
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() { cancelled = true; }
            });
        }
    }

    static final class FluxIterable<T> extends Flux<T> {
        private final Iterable<? extends T> iterable;

        FluxIterable(Iterable<? extends T> iterable) {
            this.iterable = iterable;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            try {
                java.util.Iterator<? extends T> it = iterable.iterator();
                subscriber.onSubscribe(new Subscription() {
                    private boolean cancelled;
                    private boolean done;

                    @Override
                    public void request(long n) {
                        if (cancelled || done || n <= 0) return;
                        long remaining = n;
                        while (remaining > 0 && it.hasNext() && !cancelled) {
                            subscriber.onNext(it.next());
                            remaining--;
                        }
                        if (!it.hasNext() && !cancelled) {
                            done = true;
                            subscriber.onComplete();
                        }
                    }

                    @Override
                    public void cancel() { cancelled = true; }
                });
            } catch (Exception e) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) { subscriber.onError(e); }
                    @Override
                    public void cancel() {}
                });
            }
        }
    }

    static final class FluxEmpty<T> extends Flux<T> {
        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) { subscriber.onComplete(); }
                @Override
                public void cancel() {}
            });
        }
    }

    static final class FluxError<T> extends Flux<T> {
        private final Throwable error;

        FluxError(Throwable error) {
            this.error = error;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) { subscriber.onError(error); }
                @Override
                public void cancel() {}
            });
        }
    }

    static final class FluxRange extends Flux<Integer> {
        private final int start;
        private final int count;

        FluxRange(int start, int count) {
            this.start = start;
            this.count = count;
        }

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private int index;
                private boolean cancelled;

                @Override
                public void request(long n) {
                    if (cancelled || n <= 0) return;
                    long remaining = n;
                    while (remaining > 0 && index < count && !cancelled) {
                        subscriber.onNext(start + index++);
                        remaining--;
                    }
                    if (index >= count && !cancelled) {
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() { cancelled = true; }
            });
        }
    }

    static final class FluxMap<T, R> extends Flux<R> {
        private final Flux<T> source;
        private final Function<? super T, ? extends R> mapper;

        FluxMap(Flux<T> source, Function<? super T, ? extends R> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override
        public void subscribe(Subscriber<? super R> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) {
                    try {
                        subscriber.onNext(mapper.apply(item));
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }
    }

    static final class FluxFlatMap<T, R> extends Flux<R> {
        private final Flux<T> source;
        private final Function<? super T, ? extends Publisher<? extends R>> mapper;

        FluxFlatMap(Flux<T> source, Function<? super T, ? extends Publisher<? extends R>> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override
        public void subscribe(Subscriber<? super R> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) {
                    try {
                        Publisher<? extends R> inner = mapper.apply(item);
                        if (inner != null) {
                            subscribeInner(inner, subscriber);
                        }
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }

        private static <T> void subscribeInner(Publisher<? extends T> publisher, Subscriber<? super T> downstream) {
            publisher.subscribe(new Subscriber<T>() {
                @Override
                public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }

                @Override
                public void onNext(T item) { downstream.onNext(item); }

                @Override
                public void onError(Throwable t) { downstream.onError(t); }

                @Override
                public void onComplete() {}
            });
        }
    }

    static final class FluxFilter<T> extends Flux<T> {
        private final Flux<T> source;
        private final Predicate<? super T> predicate;

        FluxFilter(Flux<T> source, Predicate<? super T> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(T item) {
                    try {
                        if (predicate.test(item)) {
                            subscriber.onNext(item);
                        }
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }
    }

    static final class FluxSubscribeOn<T> extends Flux<T> {
        private final Flux<T> source;
        private final Executor executor;

        FluxSubscribeOn(Flux<T> source, Executor executor) {
            this.source = source;
            this.executor = executor;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            executor.execute(() -> source.subscribe(subscriber));
        }
    }

    static final class FluxPublishOn<T> extends Flux<T> {
        private final Flux<T> source;
        private final Executor executor;

        FluxPublishOn(Flux<T> source, Executor executor) {
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

    static final class FluxLambdaSubscriber<T> implements Subscriber<T> {
        private final Consumer<? super T> onNext;
        private final Consumer<? super Throwable> onError;
        private final Runnable onComplete;

        FluxLambdaSubscriber(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
                             Runnable onComplete) {
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
        }

        @Override
        public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }

        @Override
        public void onNext(T item) { if (onNext != null) onNext.accept(item); }

        @Override
        public void onError(Throwable t) { if (onError != null) onError.accept(t); }

        @Override
        public void onComplete() { if (onComplete != null) onComplete.run(); }
    }

    static final class MonoFromFluxVoid extends Mono<Void> {
        private final Flux<?> source;

        MonoFromFluxVoid(Flux<?> source) {
            this.source = source;
        }

        @Override
        public void subscribe(Subscriber<? super Void> subscriber) {
            source.subscribe(new Subscriber<Object>() {
                @Override
                public void onSubscribe(Subscription s) { subscriber.onSubscribe(s); }

                @Override
                public void onNext(Object item) {}

                @Override
                public void onError(Throwable t) { subscriber.onError(t); }

                @Override
                public void onComplete() { subscriber.onComplete(); }
            });
        }
    }
}
