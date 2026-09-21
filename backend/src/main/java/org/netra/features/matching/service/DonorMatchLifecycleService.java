package org.netra.features.matching.service;

import org.netra.core.audit.AuditService;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.notification.event.BloodRequestCancelledEvent;
import org.netra.features.notification.event.MatchExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public DonorMatchLifecycleService(
            DonorMatchRepository donorMatchRepository,
            AuditService auditService,
            java.time.Clock clock,
            org.springframework.beans.factory.ObjectProvider<ApplicationEventPublisher> eventPublisherProvider) {
        this.donorMatchRepository = donorMatchRepository;
        this.auditService = auditService;
        this.clock = clock != null ? clock : java.time.Clock.systemUTC();
        this.eventPublisher = eventPublisherProvider != null ? eventPublisherProvider.getIfAvailable() : null;
    }

    public DonorMatchLifecycleService(
            DonorMatchRepository donorMatchRepository,
            AuditService auditService,
            java.time.Clock clock) {
        this(donorMatchRepository, auditService, clock, null);
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
        List<DonorMatch> overdue = donorMatchRepository.findByResponseStatusAndExpiresAtLessThanEqual(
                org.netra.features.matching.entity.MatchStatus.MATCHED, now);
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
            if (eventPublisher != null) {
                for (DonorMatch m : overdue) {
                    eventPublisher.publishEvent(new MatchExpiredEvent(
                            m.getId(), m.getBloodRequestId(), m.getDonorUserId(), null));
                }
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
        List<DonorMatch> activeMatches = donorMatchRepository.findByBloodRequestIdAndResponseStatus(
                requestId, org.netra.features.matching.entity.MatchStatus.MATCHED);
        List<UUID> affectedDonorUserIds = activeMatches.stream()
                .map(DonorMatch::getDonorUserId)
                .toList();

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
            if (eventPublisher != null && !affectedDonorUserIds.isEmpty()) {
                eventPublisher.publishEvent(new BloodRequestCancelledEvent(
                        requestId, actorId, affectedDonorUserIds));
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
