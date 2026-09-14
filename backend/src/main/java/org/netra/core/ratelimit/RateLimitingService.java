package org.netra.core.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitingService {

    private final int maxRequestsPerMinute;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingService(@Value("${netra.eligibility.max-requests-per-minute:20}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public void checkRateLimit(String clientIdentifier) {
        long currentMinute = System.currentTimeMillis() / 60000;
        String key = clientIdentifier + ":" + currentMinute;

        if (requestCounts.size() > 10000) {
            long prevMinute = currentMinute - 1;
            requestCounts.keySet().removeIf(k -> {
                String[] parts = k.split(":");
                if (parts.length > 1) {
                    try {
                        return Long.parseLong(parts[1]) < prevMinute;
                    } catch (NumberFormatException e) {
                        return true;
                    }
                }
                return true;
            });
        }

        RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter());
        int count = counter.incrementAndGet();

        if (count > maxRequestsPerMinute) {
            throw new RateLimitExceededException("Too many eligibility requests. Please wait a moment before trying again.");
        }
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
