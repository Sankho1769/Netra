package org.netra.core.ratelimit;

import java.util.function.Predicate;

/**
 * Storage abstraction for rate limiting counters.
 * Decouples rate limiting policy from in-memory or distributed (e.g. Redis) counter persistence.
 */
public interface RateLimitStore {

    int incrementAndGet(String key);

    int decrementAndGet(String key);

    int get(String key);

    boolean containsKey(String key);

    int size();

    void removeIf(Predicate<String> keyFilter);

    void clear();
}
