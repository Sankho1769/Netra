package org.netra.features.bloodrequest.service;

import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class BloodRequestExpirationService {

    private static final Logger log = LoggerFactory.getLogger(BloodRequestExpirationService.class);

    private final BloodRequestRepository bloodRequestRepository;
    private final org.netra.features.matching.service.DonorMatchLifecycleService donorMatchLifecycleService;
    private final Clock clock;
    private final org.netra.core.observability.NetraMetrics netraMetrics;

    @Autowired
    public BloodRequestExpirationService(
            BloodRequestRepository bloodRequestRepository,
            @Autowired(required = false)
            org.netra.features.matching.service.DonorMatchLifecycleService donorMatchLifecycleService,
            Clock clock,
            @Autowired(required = false)
            org.netra.core.observability.NetraMetrics netraMetrics) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.donorMatchLifecycleService = donorMatchLifecycleService;
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.netraMetrics = netraMetrics;
    }

    public BloodRequestExpirationService(
            BloodRequestRepository bloodRequestRepository,
            Clock clock) {
        this(bloodRequestRepository, null, clock, null);
    }

    public BloodRequestExpirationService(
            BloodRequestRepository bloodRequestRepository,
            org.netra.features.matching.service.DonorMatchLifecycleService donorMatchLifecycleService,
            Clock clock) {
        this(bloodRequestRepository, donorMatchLifecycleService, clock, null);
    }

    /**
     * Periodically runs background expiration of overdue open blood requests.
     */
    @Scheduled(fixedDelayString = "${netra.blood-requests.expiration-interval-ms:60000}")
    @Transactional
    public void scheduledExpiration() {
        long start = System.currentTimeMillis();
        String outcome = "SUCCESS";
        int count = 0;
        try {
            count = processExpirations(Instant.now(clock));
        } catch (Exception e) {
            outcome = "FAILURE";
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (netraMetrics != null) {
                netraMetrics.recordScheduledJobExecution("blood_request_expiration", outcome, duration, count);
            }
        }
    }

    /**
     * Atomically transitions overdue OPEN requests to EXPIRED status.
     * Idempotent: re-running with the same reference time updates 0 records.
     * Increments optimistic lock version to maintain relational consistency.
     *
     * @param referenceTime Timestamp evaluated against required_by (or now from clock if null)
     * @return Number of expired requests
     */
    @Transactional
    public int processExpirations(Instant referenceTime) {
        Instant now = referenceTime != null ? referenceTime : Instant.now(clock);
        java.util.List<java.util.UUID> overdueRequestIds = bloodRequestRepository.findOverdueRequestIds(now);
        if (overdueRequestIds.isEmpty()) {
            return 0;
        }

        int expiredCount = bloodRequestRepository.expireDueRequests(now);
        if (expiredCount > 0) {
            log.info("Expired {} overdue blood request(s) at {}", expiredCount, now);
            if (donorMatchLifecycleService != null) {
                // Cascading request-level expiration for active matches attached to newly expired requests
                for (java.util.UUID reqId : overdueRequestIds) {
                    donorMatchLifecycleService.expireActiveMatchesForRequest(reqId);
                }
            }
        }
        return expiredCount;
    }
}
