package org.netra.core.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Thread-safe in-memory rate limit counter store using ConcurrentHashMap.
 */
@Component
public class InMemoryRateLimitStore implements RateLimitStore {

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    public int incrementAndGet(String key) {
        RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter());
        return counter.incrementAndGet();
    }

    @Override
    public int decrementAndGet(String key) {
        RequestCounter counter = requestCounts.get(key);
        return counter != null ? counter.decrementAndGet() : 0;
    }

    @Override
    public int get(String key) {
        RequestCounter counter = requestCounts.get(key);
        return counter != null ? counter.getCount() : 0;
    }

    @Override
    public boolean containsKey(String key) {
        return requestCounts.containsKey(key);
    }

    @Override
    public int size() {
        return requestCounts.size();
    }

    @Override
    public void removeIf(Predicate<String> keyFilter) {
        requestCounts.keySet().removeIf(keyFilter);
    }

    @Override
    public void clear() {
        requestCounts.clear();
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public int incrementAndGet() {
            return count.incrementAndGet();
        }

        public int decrementAndGet() {
            return count.decrementAndGet();
        }

        public int getCount() {
            return count.get();
        }
    }
}
