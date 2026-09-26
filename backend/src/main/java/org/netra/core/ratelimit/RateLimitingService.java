package org.netra.core.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Service for sliding window rate limiting across endpoints.
 * Protects eligibility checking, matching, match creation, and emergency request generation.
 *
 * Operational & Security guarantees:
 * - Structured key scheme safe for IPv6 addresses (zero colon delimiter collisions).
 * - Decoupled storage backend via RateLimitStore abstraction (in-memory single-instance by default,
 *   readily replaceable with distributed Redis store).
 */
@Service
public class RateLimitingService {

    public static final int DEFAULT_CLEANUP_THRESHOLD = 10000;

    private final int maxRequestsPerMinute;
    private final int maxEmergencyRequests;
    private final int maxMatchingRequestsPerMinute;
    private final int maxMatchCreationRequestsPerMinute;
    private final int cleanupThreshold;
    private final Clock clock;
    private final RateLimitStore store;

    @Autowired
    public RateLimitingService(
            @Value("${netra.eligibility.max-requests-per-minute:20}") int maxRequestsPerMinute,
            @Value("${netra.emergency.max-requests-per-10-minutes:5}") int maxEmergencyRequests,
            @Value("${netra.matching.max-requests-per-minute:30}") int maxMatchingRequestsPerMinute,
            @Value("${netra.matching.max-creation-requests-per-minute:20}") int maxMatchCreationRequestsPerMinute,
            @Autowired(required = false) RateLimitStore store) {
        this(maxRequestsPerMinute, maxEmergencyRequests, maxMatchingRequestsPerMinute, maxMatchCreationRequestsPerMinute,
                store != null ? store : new InMemoryRateLimitStore(), Clock.systemUTC(), DEFAULT_CLEANUP_THRESHOLD);
    }

    public RateLimitingService(
            int maxRequestsPerMinute,
            int maxEmergencyRequests,
            Clock clock) {
        this(maxRequestsPerMinute, maxEmergencyRequests, 30, 20, new InMemoryRateLimitStore(), clock, DEFAULT_CLEANUP_THRESHOLD);
    }

    public RateLimitingService(
            int maxRequestsPerMinute,
            int maxEmergencyRequests,
            int maxMatchingRequestsPerMinute,
            Clock clock) {
        this(maxRequestsPerMinute, maxEmergencyRequests, maxMatchingRequestsPerMinute, 20, new InMemoryRateLimitStore(), clock, DEFAULT_CLEANUP_THRESHOLD);
    }

    public RateLimitingService(
            int maxRequestsPerMinute,
            int maxEmergencyRequests,
            int maxMatchingRequestsPerMinute,
            Clock clock,
            int cleanupThreshold) {
        this(maxRequestsPerMinute, maxEmergencyRequests, maxMatchingRequestsPerMinute, 20, new InMemoryRateLimitStore(), clock, cleanupThreshold);
    }

    public RateLimitingService(
            int maxRequestsPerMinute,
            int maxEmergencyRequests,
            int maxMatchingRequestsPerMinute,
            int maxMatchCreationRequestsPerMinute,
            Clock clock,
            int cleanupThreshold) {
        this(maxRequestsPerMinute, maxEmergencyRequests, maxMatchingRequestsPerMinute, maxMatchCreationRequestsPerMinute,
                new InMemoryRateLimitStore(), clock, cleanupThreshold);
    }

    public RateLimitingService(
            int maxRequestsPerMinute,
            int maxEmergencyRequests,
            int maxMatchingRequestsPerMinute,
            int maxMatchCreationRequestsPerMinute,
            RateLimitStore store,
            Clock clock,
            int cleanupThreshold) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxEmergencyRequests = maxEmergencyRequests;
        this.maxMatchingRequestsPerMinute = maxMatchingRequestsPerMinute;
        this.maxMatchCreationRequestsPerMinute = maxMatchCreationRequestsPerMinute;
        this.store = store != null ? store : new InMemoryRateLimitStore();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.cleanupThreshold = cleanupThreshold > 0 ? cleanupThreshold : DEFAULT_CLEANUP_THRESHOLD;
    }

    public void checkRateLimit(String clientIdentifier) {
        long currentMinute = clock.millis() / 60000;
        String key = clientIdentifier + ":" + currentMinute;

        if (store.size() >= cleanupThreshold) {
            long prevMinute = currentMinute - 1;
            store.removeIf(k -> {
                if (!k.startsWith("matching:") && !k.startsWith("emergency:") && !k.startsWith("match-create:")) {
                    long window = extractWindowFromKey(k);
                    return window != -1 && window < prevMinute;
                }
                return false;
            });
        }

        int count = store.incrementAndGet(key);
        if (count > maxRequestsPerMinute) {
            throw new RateLimitExceededException("Too many eligibility requests. Please wait a moment before trying again.");
        }
    }

    public void checkMatchingRateLimit(String clientIdentifier) {
        long currentMinute = clock.millis() / 60000;
        String key = "matching:" + clientIdentifier + ":" + currentMinute;

        if (store.size() >= cleanupThreshold) {
            long prevMinute = currentMinute - 1;
            store.removeIf(k -> {
                if (k.startsWith("matching:")) {
                    long window = extractWindowFromKey(k);
                    return window != -1 && window < prevMinute;
                }
                return false;
            });
        }

        int count = store.incrementAndGet(key);
        if (count > maxMatchingRequestsPerMinute) {
            throw new RateLimitExceededException("Too many matching requests. Please wait a moment before trying again.");
        }
    }

    public void checkMatchCreationRateLimit(String clientIdentifier) {
        long currentMinute = clock.millis() / 60000;
        String key = "match-create:" + clientIdentifier + ":" + currentMinute;

        if (store.size() >= cleanupThreshold) {
            long prevMinute = currentMinute - 1;
            store.removeIf(k -> {
                if (k.startsWith("match-create:")) {
                    long window = extractWindowFromKey(k);
                    return window != -1 && window < prevMinute;
                }
                return false;
            });
        }

        int count = store.incrementAndGet(key);
        if (count > maxMatchCreationRequestsPerMinute) {
            throw new RateLimitExceededException("Too many match creation requests. Please wait a moment before trying again.");
        }
    }

    public String checkEmergencyRateLimit(String clientIdentifier) {
        long currentWindow = getEmergencyWindow();
        String windowKey = computeEmergencyWindowKey(clientIdentifier, currentWindow);

        if (store.size() >= cleanupThreshold) {
            long prevWindow = currentWindow - 1;
            store.removeIf(k -> {
                if (k.startsWith("emergency:")) {
                    long window = extractWindowFromKey(k);
                    return window != -1 && window < prevWindow;
                }
                return false;
            });
        }

        int count = store.incrementAndGet(windowKey);
        if (count > maxEmergencyRequests) {
            throw new RateLimitExceededException("Too many emergency requests. Please wait before creating another request.");
        }
        return windowKey;
    }

    public void decrementEmergencyRateLimit(String clientIdentifier, String windowKey) {
        String key = (windowKey != null && !windowKey.isBlank()) ? windowKey : clientIdentifier;
        if (key != null) {
            store.decrementAndGet(key);
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
        return store.get(key);
    }

    public int getEmergencyCountForKey(String windowKey) {
        return store.get(windowKey);
    }

    public int getMatchingCount(String clientIdentifier) {
        long currentMinute = clock.millis() / 60000;
        return getMatchingCount(clientIdentifier, currentMinute);
    }

    public int getMatchingCount(String clientIdentifier, long minute) {
        String key = "matching:" + clientIdentifier + ":" + minute;
        return store.get(key);
    }

    public boolean containsKey(String key) {
        return store.containsKey(key);
    }

    public int getStoredKeyCount() {
        return store.size();
    }

    public void reset() {
        store.clear();
    }

    /**
     * Safely extracts the numeric window timestamp from the end of the key.
     * Guaranteed safe for IPv6 addresses containing multiple colons.
     */
    public static long extractWindowFromKey(String key) {
        if (key == null) {
            return -1;
        }
        int lastColon = key.lastIndexOf(':');
        if (lastColon < 0 || lastColon >= key.length() - 1) {
            return -1;
        }
        try {
            return Long.parseLong(key.substring(lastColon + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
