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

    @Test
    @DisplayName("Matching Rate-limit: current-minute counter works and enforces 30 requests per minute")
    void testMatchingRateLimit_CurrentMinuteCounterWorks() {
        String userId = "matching-user-1";
        long currentMinute = testClock.millis() / 60000;

        // 30 valid requests
        for (int i = 1; i <= 30; i++) {
            rateLimitingService.checkMatchingRateLimit(userId);
            assertEquals(i, rateLimitingService.getMatchingCount(userId, currentMinute));
        }

        // 31st request in same minute must throw RateLimitExceededException
        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitingService.checkMatchingRateLimit(userId)
        );
        assertTrue(ex.getMessage().contains("Too many matching requests"));
    }

    @Test
    @DisplayName("Matching Rate-limit: next-minute counter is independent")
    void testMatchingRateLimit_NextMinuteCounterIsIndependent() {
        String userId = "matching-user-2";
        long minute1 = testClock.millis() / 60000;

        // Exhaust quota in minute 1
        for (int i = 0; i < 30; i++) {
            rateLimitingService.checkMatchingRateLimit(userId);
        }
        assertEquals(30, rateLimitingService.getMatchingCount(userId, minute1));
        assertThrows(RateLimitExceededException.class, () -> rateLimitingService.checkMatchingRateLimit(userId));

        // Advance clock by 1 minute
        testClock.advance(Duration.ofMinutes(1));
        long minute2 = testClock.millis() / 60000;
        assertNotEquals(minute1, minute2);

        // Next minute has fresh quota
        assertEquals(0, rateLimitingService.getMatchingCount(userId, minute2));
        assertDoesNotThrow(() -> rateLimitingService.checkMatchingRateLimit(userId));
        assertEquals(1, rateLimitingService.getMatchingCount(userId, minute2));
    }

    @Test
    @DisplayName("Matching Rate-limit: old matching keys are cleaned when cleanup threshold is reached")
    void testMatchingRateLimit_OldKeysCleanedWhenCleanupThresholdReached() {
        // Create service with cleanup threshold of 5
        RateLimitingService customService = new RateLimitingService(20, 5, 30, testClock, 5);

        long minute1 = testClock.millis() / 60000;

        // Populate 4 keys in minute 1
        customService.checkMatchingRateLimit("user-A");
        customService.checkMatchingRateLimit("user-B");
        customService.checkMatchingRateLimit("user-C");
        customService.checkMatchingRateLimit("user-D");

        assertTrue(customService.containsKey("matching:user-A:" + minute1));
        assertTrue(customService.containsKey("matching:user-B:" + minute1));
        assertTrue(customService.containsKey("matching:user-C:" + minute1));
        assertTrue(customService.containsKey("matching:user-D:" + minute1));

        // Advance clock by 2 minutes to minute 3 (so minute 1 is < prevMinute)
        testClock.advance(Duration.ofMinutes(2));
        long minute3 = testClock.millis() / 60000;

        // Add 2 more keys in minute 3 to push total keys to 6 (exceeding threshold of 5)
        customService.checkMatchingRateLimit("user-E");
        customService.checkMatchingRateLimit("user-F");

        // The cleanup triggers when size > threshold, evicting minute 1 keys
        assertFalse(customService.containsKey("matching:user-A:" + minute1), "Old matching key from minute 1 must be cleaned");
        assertFalse(customService.containsKey("matching:user-B:" + minute1), "Old matching key from minute 1 must be cleaned");
        assertFalse(customService.containsKey("matching:user-C:" + minute1), "Old matching key from minute 1 must be cleaned");
        assertFalse(customService.containsKey("matching:user-D:" + minute1), "Old matching key from minute 1 must be cleaned");

        // New keys in minute 3 must remain intact
        assertTrue(customService.containsKey("matching:user-E:" + minute3), "Current minute matching key must be retained");
        assertTrue(customService.containsKey("matching:user-F:" + minute3), "Current minute matching key must be retained");
    }

    @Test
    @DisplayName("IPv6 Rate-limit: emergency rate limiting and rollback work seamlessly with IPv6 client addresses")
    void testIPv6ClientEmergencyRateLimitAndRollback() {
        String ipv6Client = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        long currentWindow = rateLimitingService.getEmergencyWindow();

        String windowKey = rateLimitingService.checkEmergencyRateLimit(ipv6Client);
        assertNotNull(windowKey);
        assertTrue(windowKey.startsWith("emergency:" + ipv6Client + ":"));

        assertEquals(1, rateLimitingService.getEmergencyCount(ipv6Client, currentWindow));

        // Decrement using explicit window key with multiple colons
        rateLimitingService.decrementEmergencyRateLimit(ipv6Client, windowKey);
        assertEquals(0, rateLimitingService.getEmergencyCount(ipv6Client, currentWindow));
    }

    @Test
    @DisplayName("IPv6 Rate-limit: cleanup correctly parses IPv6 keys using lastIndexOf colon without delimiter collision")
    void testIPv6ClientMatchingRateLimitAndCleanup() {
        RateLimitingService customService = new RateLimitingService(20, 5, 30, testClock, 3);
        long minute1 = testClock.millis() / 60000;

        String ipv6ClientA = "2001:db8::1";
        String ipv6ClientB = "fe80::1ff:fe23:4567:890a";

        customService.checkMatchingRateLimit(ipv6ClientA);
        customService.checkMatchingRateLimit(ipv6ClientB);

        assertTrue(customService.containsKey("matching:" + ipv6ClientA + ":" + minute1));
        assertTrue(customService.containsKey("matching:" + ipv6ClientB + ":" + minute1));

        // Advance clock by 2 minutes
        testClock.advance(Duration.ofMinutes(2));
        long minute3 = testClock.millis() / 60000;

        // Push 2 more keys in minute 3 to exceed cleanup threshold (3)
        customService.checkMatchingRateLimit("user-X");
        customService.checkMatchingRateLimit("user-Y");

        // Old IPv6 keys must be cleaned up properly without NumberFormatException
        assertFalse(customService.containsKey("matching:" + ipv6ClientA + ":" + minute1), "IPv6 key from minute 1 must be evicted");
        assertFalse(customService.containsKey("matching:" + ipv6ClientB + ":" + minute1), "IPv6 key from minute 1 must be evicted");
        assertTrue(customService.containsKey("matching:user-X:" + minute3));
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
