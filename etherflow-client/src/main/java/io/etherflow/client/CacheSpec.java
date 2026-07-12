package io.etherflow.client;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class CacheEntry {
    final byte[] body;
    final long expiry;

    CacheEntry(byte[] body, long expiry) {
        this.body = body;
        this.expiry = expiry;
    }

    boolean isExpired() {
        return System.currentTimeMillis() > expiry;
    }
}

public class CacheSpec {
    private final Duration ttl;
    private final int maxSize;
    private final ConcurrentHashMap<String, CacheEntry> cache;

    private CacheSpec(Duration ttl, int maxSize) {
        this.ttl = ttl;
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>();
    }

    public static CacheSpec disabled() {
        return new CacheSpec(Duration.ZERO, 0);
    }

    public static CacheSpec of(Duration ttl, int maxSize) {
        return new CacheSpec(ttl, maxSize);
    }

    public boolean isEnabled() { return ttl.toMillis() > 0 && maxSize > 0; }
    public Duration ttl() { return ttl; }
    public int maxSize() { return maxSize; }

    byte[] get(String cacheKey) {
        CacheEntry entry = cache.get(cacheKey);
        if (entry == null) return null;
        if (entry.isExpired()) {
            cache.remove(cacheKey);
            return null;
        }
        return entry.body;
    }

    void put(String cacheKey, byte[] body) {
        if (!isEnabled()) return;
        if (cache.size() >= maxSize) {
            cache.clear();
        }
        cache.put(cacheKey, new CacheEntry(body, System.currentTimeMillis() + ttl.toMillis()));
    }

    void invalidate(String cacheKey) {
        cache.remove(cacheKey);
    }

    void clear() {
        cache.clear();
    }
}
