package org.netra.core.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private MutableClock testClock;
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        // Start at a fixed timestamp: 2026-09-18 10:09:45 UTC (15 seconds before the 10:10:00 boundary)
        testClock = new MutableClock(Instant.parse("2026-09-18T10:09:45Z"), ZoneOffset.UTC);
        rateLimitingService = new RateLimitingService(20, 5, testClock);
    }

    @Test
    @DisplayName("Rate-limit: increment belongs to one specific window")
    void testIncrementBelongsToSpecificWindow() {
        String userId = "user-100";
        long initialWindow = rateLimitingService.getEmergencyWindow();

        String windowKey = rateLimitingService.checkEmergencyRateLimit(userId);
        assertNotNull(windowKey);
        assertTrue(windowKey.contains("emergency:" + userId + ":" + initialWindow));

        // Window 1 counter should be 1
        assertEquals(1, rateLimitingService.getEmergencyCount(userId, initialWindow));
        // Next window counter should be 0
        assertEquals(0, rateLimitingService.getEmergencyCount(userId, initialWindow + 1));
    }

    @Test
    @DisplayName("Rate-limit: rollback decrements that same window even after clock crosses window boundary")
    void testRollbackDecrementsSameWindowAcrossClockBoundary() {
        String userId = "user-200";
        long window1 = rateLimitingService.getEmergencyWindow();

        // 1. Increment in Window 1 (10:09:45 UTC)
        String window1Key = rateLimitingService.checkEmergencyRateLimit(userId);
        assertEquals(1, rateLimitingService.getEmergencyCount(userId, window1));

        // 2. Advance clock past the 10-minute boundary into Window 2 (10:10:15 UTC)
        testClock.advance(Duration.ofSeconds(30));
        long window2 = rateLimitingService.getEmergencyWindow();
        assertNotEquals(window1, window2, "Clock should have transitioned to a new 10-minute window");
        assertEquals(window1 + 1, window2);

        // Verify current window count is 0
        assertEquals(0, rateLimitingService.getEmergencyCount(userId, window2));

        // 3. Perform rollback passing explicit window key
        rateLimitingService.decrementEmergencyRateLimit(userId, window1Key);

        // 4. Invariant assertions:
        // Window 1 should be decremented back to 0
        assertEquals(0, rateLimitingService.getEmergencyCount(userId, window1),
                "Rollback must decrement the original window where the increment occurred");

        // Window 2 must NOT be modified by the rollback
        assertEquals(0, rateLimitingService.getEmergencyCount(userId, window2),
                "Rollback must not decrement or modify the new window");
    }

    @Test
    @DisplayName("Rate-limit: explicit window key rollback operates on exact window key")
    void testExplicitWindowKeyRollback() {
        String userId = "user-300";
        long window1 = rateLimitingService.getEmergencyWindow();

        String key = rateLimitingService.checkEmergencyRateLimit(userId);
        assertEquals(1, rateLimitingService.getEmergencyCountForKey(key));

        // Advance clock by 1 hour (6 windows ahead)
        testClock.advance(Duration.ofHours(1));
        long laterWindow = rateLimitingService.getEmergencyWindow();
        assertTrue(laterWindow > window1);

        // Rollback using explicit key
        rateLimitingService.decrementEmergencyRateLimit(userId, key);

        assertEquals(0, rateLimitingService.getEmergencyCountForKey(key),
                "Explicit key rollback must decrement the exact window counter");
        assertEquals(0, rateLimitingService.getEmergencyCount(userId, laterWindow),
                "Current window must remain unaffected");
    }

    @Test
    @DisplayName("Rate-limit: enforce 5 requests per 10-minute window; 6th request rejected; resets in next window")
    void testEnforceMaxRequestsPerWindowAndResetOnNextWindow() {
        String userId = "user-400";
        long window1 = rateLimitingService.getEmergencyWindow();

        // 5 valid creations
        for (int i = 1; i <= 5; i++) {
            rateLimitingService.checkEmergencyRateLimit(userId);
            assertEquals(i, rateLimitingService.getEmergencyCount(userId, window1));
        }

        // 6th request must throw RateLimitExceededException
        assertThrows(RateLimitExceededException.class, () -> rateLimitingService.checkEmergencyRateLimit(userId));

        // Advance clock into next 10-minute window
        testClock.advance(Duration.ofMinutes(11));
        long window2 = rateLimitingService.getEmergencyWindow();
        assertNotEquals(window1, window2);

        // Fresh quota in new window: 1st request succeeds
        assertDoesNotThrow(() -> rateLimitingService.checkEmergencyRateLimit(userId));
        assertEquals(1, rateLimitingService.getEmergencyCount(userId, window2));
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        public MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        public void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
