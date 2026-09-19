package org.netra.features.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.ValidationException;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DonorMatchLifecycleTest {

    @Test
    @DisplayName("Unit: Initial match state is MATCHED with version 0")
    void testInitialMatchState() {
        UUID reqId = UUID.randomUUID();
        UUID donorId = UUID.randomUUID();
        Instant expiry = Instant.now().plus(24, ChronoUnit.HOURS);

        DonorMatch match = new DonorMatch(reqId, donorId, expiry);

        assertEquals(MatchStatus.MATCHED, match.getResponseStatus());
        assertEquals(0L, match.getVersion());
        assertFalse(match.getResponseStatus().isTerminal());
        assertFalse(match.isExpired(Instant.now()));
    }

    @Test
    @DisplayName("Unit: MatchStatus state machine allows only valid transitions from MATCHED")
    void testMatchStatusTransitions() {
        // From MATCHED:
        assertTrue(MatchStatus.MATCHED.canTransitionTo(MatchStatus.ACCEPTED));
        assertTrue(MatchStatus.MATCHED.canTransitionTo(MatchStatus.DECLINED));
        assertTrue(MatchStatus.MATCHED.canTransitionTo(MatchStatus.EXPIRED));
        assertTrue(MatchStatus.MATCHED.canTransitionTo(MatchStatus.CANCELLED));
        assertFalse(MatchStatus.MATCHED.canTransitionTo(MatchStatus.MATCHED));
        assertFalse(MatchStatus.MATCHED.canTransitionTo(null));

        // From Terminal States:
        for (MatchStatus terminal : new MatchStatus[]{MatchStatus.ACCEPTED, MatchStatus.DECLINED, MatchStatus.EXPIRED, MatchStatus.CANCELLED}) {
            assertTrue(terminal.isTerminal());
            for (MatchStatus target : MatchStatus.values()) {
                assertFalse(terminal.canTransitionTo(target), terminal + " must not transition to " + target);
            }
        }
    }

    @Test
    @DisplayName("Unit: Expiration condition is expiresAt <= now")
    void testExpirationCondition() {
        Instant exactExpiry = Instant.parse("2026-09-18T12:00:00Z");
        DonorMatch match = new DonorMatch(UUID.randomUUID(), UUID.randomUUID(), exactExpiry);

        // Before expiry:
        assertFalse(match.isExpired(exactExpiry.minusMillis(1)));

        // Exactly at expiry (expiresAt <= now):
        assertTrue(match.isExpired(exactExpiry));

        // After expiry:
        assertTrue(match.isExpired(exactExpiry.plusMillis(1)));
    }

    @Test
    @DisplayName("Unit: MATCHED -> ACCEPTED succeeds if active and not expired")
    void testAcceptSuccess() {
        Instant now = Instant.now();
        DonorMatch match = new DonorMatch(UUID.randomUUID(), UUID.randomUUID(), now.plus(1, ChronoUnit.HOURS));

        match.accept(now);

        assertEquals(MatchStatus.ACCEPTED, match.getResponseStatus());
        assertEquals(now, match.getRespondedAt());
        assertEquals(now, match.getUpdatedAt());
        assertTrue(match.getResponseStatus().isTerminal());
    }

    @Test
    @DisplayName("Unit: MATCHED -> DECLINED succeeds if active and not expired")
    void testDeclineSuccess() {
        Instant now = Instant.now();
        DonorMatch match = new DonorMatch(UUID.randomUUID(), UUID.randomUUID(), now.plus(1, ChronoUnit.HOURS));

        match.decline(now);

        assertEquals(MatchStatus.DECLINED, match.getResponseStatus());
        assertEquals(now, match.getRespondedAt());
        assertEquals(now, match.getUpdatedAt());
        assertTrue(match.getResponseStatus().isTerminal());
    }

    @Test
    @DisplayName("Unit: Expired match cannot be accepted or declined")
    void testExpiredMatchCannotAcceptOrDecline() {
        Instant expiry = Instant.parse("2026-09-18T10:00:00Z");
        Instant evaluateTime = Instant.parse("2026-09-18T10:00:01Z");
        DonorMatch match = new DonorMatch(UUID.randomUUID(), UUID.randomUUID(), expiry);

        assertThrows(ValidationException.class, () -> match.accept(evaluateTime));
        assertThrows(ValidationException.class, () -> match.decline(evaluateTime));
    }

    @Test
    @DisplayName("Unit: Terminal match cannot be accepted, declined, cancelled, or expired again")
    void testTerminalMatchesImmutable() {
        Instant now = Instant.now();
        DonorMatch match = new DonorMatch(UUID.randomUUID(), UUID.randomUUID(), now.plus(1, ChronoUnit.HOURS));
        match.accept(now);

        assertThrows(ValidationException.class, () -> match.accept(now));
        assertThrows(ValidationException.class, () -> match.decline(now));
        assertThrows(ValidationException.class, () -> match.cancel(now));
        assertThrows(ValidationException.class, () -> match.expire(now));
    }

    @Test
    @DisplayName("Unit: Creation timestamp and expiry calculations are deterministic with explicit clock timestamps")
    void testDeterministicTimestampsAndExpiry() {
        Instant fixedNow = Instant.parse("2026-09-19T10:00:00Z");
        Instant fixedExpiry = Instant.parse("2026-09-20T10:00:00Z");
        UUID reqId = UUID.randomUUID();
        UUID donorId = UUID.randomUUID();

        DonorMatch match = new DonorMatch(reqId, donorId, fixedNow, fixedExpiry);

        assertEquals(fixedNow, match.getCreatedAt(), "createdAt must strictly match the supplied clock instant");
        assertEquals(fixedNow, match.getUpdatedAt(), "updatedAt must initialize to the supplied clock instant");
        assertEquals(fixedExpiry, match.getExpiresAt(), "expiresAt must strictly match the supplied expiry instant");
        assertFalse(match.isExpired(fixedNow), "Match must not be expired at creation time");
        assertTrue(match.isExpired(fixedExpiry), "Match must be expired at exact expiry instant");
    }

    @Test
    @DisplayName("Unit: isUniqueMatchConstraintViolation detects explicit Hibernate ConstraintViolationException name")
    void testUniqueConstraintDetection_ExplicitConstraintName() {
        org.netra.features.matching.service.DonorResponseService service =
                new org.netra.features.matching.service.DonorResponseService(
                        null, null, null, null, null, null, null, null,
                        java.time.Clock.systemUTC(), java.time.Duration.ofHours(24), 100.0, 90, 10);

        org.hibernate.exception.ConstraintViolationException cve =
                new org.hibernate.exception.ConstraintViolationException(
                        "Duplicate key", new java.sql.SQLException("error"), "uq_donor_matches_request_donor");
        org.springframework.dao.DataIntegrityViolationException dive =
                new org.springframework.dao.DataIntegrityViolationException("Constraint violation", cve);

        assertTrue(service.isUniqueMatchConstraintViolation(dive),
                "Must detect uq_donor_matches_request_donor via ConstraintViolationException.getConstraintName()");
    }

    @Test
    @DisplayName("Unit: isUniqueMatchConstraintViolation detects fallback message pattern when explicit name unavailable")
    void testUniqueConstraintDetection_FallbackMessage() {
        org.netra.features.matching.service.DonorResponseService service =
                new org.netra.features.matching.service.DonorResponseService(
                        null, null, null, null, null, null, null, null,
                        java.time.Clock.systemUTC(), java.time.Duration.ofHours(24), 100.0, 90, 10);

        org.springframework.dao.DataIntegrityViolationException dive =
                new org.springframework.dao.DataIntegrityViolationException(
                        "Unique index or primary key violation: \"PUBLIC.UQ_DONOR_MATCHES_REQUEST_DONOR_INDEX_7 ON PUBLIC.DONOR_MATCHES(BLOOD_REQUEST_ID, DONOR_USER_ID)\"");

        assertTrue(service.isUniqueMatchConstraintViolation(dive),
                "Must detect unique constraint violation via message token fallback matching");
    }

    @Test
    @DisplayName("Unit: isUniqueMatchConstraintViolation rejects unrelated foreign key or check constraint violations")
    void testUniqueConstraintDetection_UnrelatedConstraintIgnored() {
        org.netra.features.matching.service.DonorResponseService service =
                new org.netra.features.matching.service.DonorResponseService(
                        null, null, null, null, null, null, null, null,
                        java.time.Clock.systemUTC(), java.time.Duration.ofHours(24), 100.0, 90, 10);

        // Unrelated Foreign Key
        org.hibernate.exception.ConstraintViolationException fkCve =
                new org.hibernate.exception.ConstraintViolationException(
                        "FK violation", new java.sql.SQLException("error"), "fk_donor_matches_request");
        org.springframework.dao.DataIntegrityViolationException fkDive =
                new org.springframework.dao.DataIntegrityViolationException("Foreign key error", fkCve);
        assertFalse(service.isUniqueMatchConstraintViolation(fkDive),
                "Unrelated foreign key constraint must not be treated as unique match violation");

        // Unrelated Check Constraint
        org.springframework.dao.DataIntegrityViolationException checkDive =
                new org.springframework.dao.DataIntegrityViolationException(
                        "Check constraint violation: \"CHK_DONOR_MATCHES_STATUS: (RESPONSE_STATUS IN ('MATCHED', 'ACCEPTED'))\"");
        assertFalse(service.isUniqueMatchConstraintViolation(checkDive),
                "Check constraint violation must not be treated as unique match violation");
    }

    @Test
    @DisplayName("Unit: Only DonorMatchLifecycleService owns a scheduled method for general match expiration")
    void testSingleScheduledOwnerForMatchExpiration() {
        java.util.List<java.lang.reflect.Method> scheduledMethods = new java.util.ArrayList<>();
        for (Class<?> clazz : java.util.List.of(
                org.netra.features.matching.service.DonorMatchLifecycleService.class,
                org.netra.features.matching.service.DonorResponseService.class,
                org.netra.features.matching.service.DonorMatchingService.class,
                org.netra.features.bloodrequest.service.BloodRequestExpirationService.class)) {
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(org.springframework.scheduling.annotation.Scheduled.class)) {
                    scheduledMethods.add(m);
                }
            }
        }

        // Exactly two scheduled methods across both domains:
        // 1. DonorMatchLifecycleService.scheduledExpiration() for general match expiration
        // 2. BloodRequestExpirationService.scheduledExpiration() for blood request expiration
        assertEquals(2, scheduledMethods.size());

        boolean matchLifecycleScheduled = scheduledMethods.stream().anyMatch(m ->
                m.getDeclaringClass().equals(org.netra.features.matching.service.DonorMatchLifecycleService.class)
                && m.getName().equals("scheduledExpiration"));
        assertTrue(matchLifecycleScheduled, "DonorMatchLifecycleService must own scheduledExpiration");

        boolean bloodRequestScheduled = scheduledMethods.stream().anyMatch(m ->
                m.getDeclaringClass().equals(org.netra.features.bloodrequest.service.BloodRequestExpirationService.class)
                && m.getName().equals("scheduledExpiration"));
        assertTrue(bloodRequestScheduled, "BloodRequestExpirationService must own scheduledExpiration for blood requests");
    }
}
