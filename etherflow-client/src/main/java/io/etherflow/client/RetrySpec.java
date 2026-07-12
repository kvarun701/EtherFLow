package io.etherflow.client;

import java.time.Duration;

public class RetrySpec {
    private final int maxRetries;
    private final Duration initialBackoff;
    private final double backoffMultiplier;
    private final Duration maxBackoff;

    RetrySpec(int maxRetries, Duration initialBackoff, double backoffMultiplier, Duration maxBackoff) {
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
        this.backoffMultiplier = backoffMultiplier;
        this.maxBackoff = maxBackoff;
    }

    public static RetrySpec defaults() {
        return new RetrySpec(3, Duration.ofMillis(100), 2.0, Duration.ofSeconds(5));
    }

    public static RetrySpec none() {
        return new RetrySpec(0, Duration.ZERO, 1.0, Duration.ZERO);
    }

    public int maxRetries() { return maxRetries; }
    public Duration initialBackoff() { return initialBackoff; }
    public double backoffMultiplier() { return backoffMultiplier; }
    public Duration maxBackoff() { return maxBackoff; }

    public long computeDelay(int attempt) {
        if (attempt <= 0) return 0;
        long delay = (long) (initialBackoff.toMillis() * Math.pow(backoffMultiplier, attempt - 1));
        return Math.min(delay, maxBackoff.toMillis());
    }
}
