package io.ddys.halo.ddysopen.cache;

public record CacheStats(
    int size,
    long hits,
    long misses,
    long writes,
    long evictions
) {
}

