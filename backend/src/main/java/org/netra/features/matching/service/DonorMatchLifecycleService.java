package org.netra.features.matching.service;

import org.netra.core.audit.AuditService;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing background and decoupled lifecycle transitions for Donor Matches.
 *
 * Responsibilities:
 * - Cancelling active MATCHED matches when a blood request is cancelled.
 * - Expiring active MATCHED matches when a blood request expires.
 * - Periodic background batch expiration of overdue MATCHED records (expiresAt <= now).
 *
 * Decouples BloodRequestService from DonorResponseService to prevent circular dependencies.
 */
@Service
public class DonorMatchLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(DonorMatchLifecycleService.class);

    private final DonorMatchRepository donorMatchRepository;
    private final AuditService auditService;
    private final java.time.Clock clock;

    public DonorMatchLifecycleService(
            DonorMatchRepository donorMatchRepository,
            AuditService auditService,
            java.time.Clock clock) {
        this.donorMatchRepository = donorMatchRepository;
        this.auditService = auditService;
        this.clock = clock != null ? clock : java.time.Clock.systemUTC();
    }

    /**
     * Idempotent scheduled background task expiring matches that reached expiresAt <= now.
     * Sole scheduled owner of general donor match expiration.
     */
    @Scheduled(fixedDelayString = "${netra.donor-matching.match-expiration-interval-ms:60000}")
    @Transactional
    public void scheduledExpiration() {
        expireOverdueMatches(Instant.now(clock));
    }

    /**
     * Atomically transitions all MATCHED records with expiresAt <= now (or where parent request is EXPIRED) to EXPIRED.
     *
     * @param referenceTime Evaluation timestamp
     * @return Number of expired matches
     */
    @Transactional
    public int expireOverdueMatches(Instant referenceTime) {
        Instant now = referenceTime != null ? referenceTime : Instant.now(clock);
        int count = donorMatchRepository.expireOverdueMatches(now);
        if (count > 0) {
            log.info("Batch expired {} overdue donor match(es) at {}", count, now);
            if (auditService != null) {
                auditService.logAuthEvent(
                        "DONOR_MATCH_EXPIRED",
                        null,
                        "SYSTEM",
                        "NetraMatchLifecycleService",
                        String.format("{\"expiredCount\":%d,\"timestamp\":\"%s\"}", count, now)
                );
            }
        }
        return count;
    }

    /**
     * Atomically transitions active MATCHED records associated with a cancelled request to CANCELLED.
     */
    @Transactional
    public int cancelActiveMatchesForRequest(UUID requestId, UUID actorId, String clientIp, String userAgent) {
        Instant now = Instant.now(clock);
        int count = donorMatchRepository.cancelActiveMatchesForRequest(requestId, now);
        if (count > 0) {
            log.info("Cancelled {} active donor match(es) for blood request {}", count, requestId);
            if (auditService != null) {
                auditService.logAuthEvent(
                        "DONOR_MATCH_CANCELLED",
                        actorId,
                        clientIp,
                        userAgent,
                        String.format("{\"requestId\":\"%s\",\"cancelledCount\":%d}", requestId, count)
                );
            }
        }
        return count;
    }

    /**
     * Atomically transitions active MATCHED records associated with an expired request to EXPIRED.
     */
    @Transactional
    public int expireActiveMatchesForRequest(UUID requestId) {
        Instant now = Instant.now(clock);
        int count = donorMatchRepository.expireActiveMatchesForRequest(requestId, now);
        if (count > 0) {
            log.info("Expired {} active donor match(es) for expired blood request {}", count, requestId);
            if (auditService != null) {
                auditService.logAuthEvent(
                        "DONOR_MATCH_EXPIRED",
                        null,
                        "SYSTEM",
                        "NetraMatchLifecycleService",
                        String.format("{\"requestId\":\"%s\",\"expiredCount\":%d}", requestId, count)
                );
            }
        }
        return count;
    }
}
