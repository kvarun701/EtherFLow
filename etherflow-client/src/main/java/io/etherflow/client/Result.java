package io.etherflow.client;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

public final class Result<T> {
    private final T value;
    private final Throwable error;

    private Result(T value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> success(T value) {
        Objects.requireNonNull(value);
        return new Result<>(value, null);
    }

    public static <T> Result<T> error(Throwable error) {
        Objects.requireNonNull(error);
        return new Result<>(null, error);
    }

    public boolean isSuccess() { return error == null; }
    public boolean isError() { return error != null; }

    public T get() {
        if (error != null) throw new RuntimeException(error);
        return value;
    }

    public T orElse(T fallback) {
        return error != null ? fallback : value;
    }

    public Throwable getError() { return error; }

    public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        if (error != null) return Result.error(error);
        try {
            return Result.success(mapper.apply(value));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @Override
    public String toString() {
        return error != null ? "Result{error=" + error + '}' : "Result{value=" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Result)) return false;
        Result<?> other = (Result<?>) o;
        return Objects.equals(value, other.value) && Objects.equals(error, other.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, error);
    }
}
