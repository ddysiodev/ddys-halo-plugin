package io.ddys.halo.ddysopen.cache;

import java.time.Instant;

record CacheEntry<T>(T value, Instant expiresAt, Instant createdAt) {
    boolean expired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}

