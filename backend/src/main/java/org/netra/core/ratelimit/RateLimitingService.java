package org.netra.core.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitingService {

    private final int maxRequestsPerMinute;
    private final int maxEmergencyRequests;
    private final Clock clock;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public RateLimitingService(
            @Value("${netra.eligibility.max-requests-per-minute:20}") int maxRequestsPerMinute,
            @Value("${netra.emergency.max-requests-per-10-minutes:5}") int maxEmergencyRequests) {
        this(maxRequestsPerMinute, maxEmergencyRequests, Clock.systemUTC());
    }

    public RateLimitingService(
            int maxRequestsPerMinute,
            int maxEmergencyRequests,
            Clock clock) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxEmergencyRequests = maxEmergencyRequests;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public void checkRateLimit(String clientIdentifier) {
        long currentMinute = clock.millis() / 60000;
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

    public String checkEmergencyRateLimit(String clientIdentifier) {
        long currentWindow = getEmergencyWindow();
        String windowKey = computeEmergencyWindowKey(clientIdentifier, currentWindow);

        if (requestCounts.size() > 10000) {
            long prevWindow = currentWindow - 1;
            requestCounts.keySet().removeIf(k -> {
                String[] parts = k.split(":");
                if (parts.length > 2 && parts[0].equals("emergency")) {
                    try {
                        return Long.parseLong(parts[2]) < prevWindow;
                    } catch (NumberFormatException e) {
                        return true;
                    }
                }
                return false;
            });
        }

        RequestCounter counter = requestCounts.computeIfAbsent(windowKey, k -> new RequestCounter());
        int count = counter.incrementAndGet();

        if (count > maxEmergencyRequests) {
            throw new RateLimitExceededException("Too many emergency requests. Please wait before creating another request.");
        }
        return windowKey;
    }

    public void decrementEmergencyRateLimit(String clientIdentifier, String windowKey) {
        String key = (windowKey != null && !windowKey.isBlank()) ? windowKey : clientIdentifier;
        if (key != null) {
            RequestCounter counter = requestCounts.get(key);
            if (counter != null) {
                counter.decrementAndGet();
            }
        }
    }

    public void decrementEmergencyRateLimit(String windowKey) {
        decrementEmergencyRateLimit(null, windowKey);
    }

    public long getEmergencyWindow() {
        return (clock.millis() / 60000) / 10;
    }

    public String computeEmergencyWindowKey(String clientIdentifier) {
        return "emergency:" + clientIdentifier + ":" + getEmergencyWindow();
    }

    public String computeEmergencyWindowKey(String clientIdentifier, long window) {
        return "emergency:" + clientIdentifier + ":" + window;
    }

    public int getEmergencyCount(String clientIdentifier) {
        return getEmergencyCount(clientIdentifier, getEmergencyWindow());
    }

    public int getEmergencyCount(String clientIdentifier, long window) {
        String key = "emergency:" + clientIdentifier + ":" + window;
        RequestCounter counter = requestCounts.get(key);
        return counter != null ? counter.getCount() : 0;
    }

    public int getEmergencyCountForKey(String windowKey) {
        RequestCounter counter = requestCounts.get(windowKey);
        return counter != null ? counter.getCount() : 0;
    }

    public void reset() {
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
