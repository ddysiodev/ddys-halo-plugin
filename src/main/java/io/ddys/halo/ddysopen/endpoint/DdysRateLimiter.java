package io.ddys.halo.ddysopen.endpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DdysRateLimiter {

    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    public boolean allow(String key, int maxAttempts, Duration window) {
        if (key == null || key.isBlank()) {
            key = "anonymous";
        }
        String bucketKey = key;
        Instant now = Instant.now();
        Deque<Instant> bucket = buckets.computeIfAbsent(bucketKey, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            prune(bucket, now.minus(window));
            if (bucket.size() >= maxAttempts) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }

    public int bucketCount() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));
        for (Map.Entry<String, Deque<Instant>> entry : buckets.entrySet()) {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), cutoff);
                if (entry.getValue().isEmpty()) {
                    buckets.remove(entry.getKey(), entry.getValue());
                }
            }
        }
        return buckets.size();
    }

    private void prune(Deque<Instant> bucket, Instant cutoff) {
        while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
            bucket.removeFirst();
        }
    }
}
