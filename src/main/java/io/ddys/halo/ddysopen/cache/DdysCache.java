package io.ddys.halo.ddysopen.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DdysCache {

    private final Map<String, CacheEntry<Object>> entries = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong writes = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    @SuppressWarnings("unchecked")
    public <T> Mono<T> getOrCompute(String key, int ttlSeconds, int maxEntries, Supplier<Mono<T>> supplier) {
        if (ttlSeconds <= 0) {
            misses.incrementAndGet();
            return supplier.get();
        }
        Instant now = Instant.now();
        CacheEntry<Object> entry = entries.get(key);
        if (entry != null && !entry.expired(now)) {
            hits.incrementAndGet();
            return Mono.just((T) entry.value());
        }
        misses.incrementAndGet();
        return supplier.get()
            .doOnNext(value -> put(key, value, ttlSeconds, maxEntries));
    }

    public void put(String key, Object value, int ttlSeconds, int maxEntries) {
        if (value == null || ttlSeconds <= 0) {
            return;
        }
        Instant now = Instant.now();
        entries.put(key, new CacheEntry<>(value, now.plus(Duration.ofSeconds(ttlSeconds)), now));
        writes.incrementAndGet();
        enforceLimit(maxEntries);
    }

    public int clear() {
        int size = entries.size();
        entries.clear();
        evictions.addAndGet(size);
        return size;
    }

    public int pruneExpired() {
        Instant now = Instant.now();
        int before = entries.size();
        entries.entrySet().removeIf(item -> item.getValue().expired(now));
        int removed = before - entries.size();
        evictions.addAndGet(removed);
        return removed;
    }

    public CacheStats stats() {
        pruneExpired();
        return new CacheStats(
            entries.size(),
            hits.get(),
            misses.get(),
            writes.get(),
            evictions.get()
        );
    }

    private void enforceLimit(int maxEntries) {
        if (maxEntries <= 0) {
            return;
        }
        while (entries.size() > maxEntries) {
            entries.entrySet().stream()
                .min(Comparator.comparing(item -> item.getValue().createdAt()))
                .ifPresent(item -> {
                    entries.remove(item.getKey());
                    evictions.incrementAndGet();
                });
        }
    }
}

