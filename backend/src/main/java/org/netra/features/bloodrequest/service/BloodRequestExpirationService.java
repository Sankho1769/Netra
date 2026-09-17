package org.netra.features.bloodrequest.service;

import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BloodRequestExpirationService {

    private static final Logger log = LoggerFactory.getLogger(BloodRequestExpirationService.class);

    private final BloodRequestRepository bloodRequestRepository;

    public BloodRequestExpirationService(BloodRequestRepository bloodRequestRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
    }

    /**
     * Periodically runs background expiration of overdue open blood requests.
     */
    @Scheduled(fixedDelayString = "${netra.blood-requests.expiration-interval-ms:60000}")
    @Transactional
    public void scheduledExpiration() {
        processExpirations(Instant.now());
    }

    /**
     * Atomically transitions overdue OPEN requests to EXPIRED status.
     * Idempotent: re-running with the same reference time updates 0 records.
     * Increments optimistic lock version to maintain relational consistency.
     *
     * @param referenceTime Timestamp evaluated against required_by (or now if null)
     * @return Number of expired requests
     */
    @Transactional
    public int processExpirations(Instant referenceTime) {
        Instant now = referenceTime != null ? referenceTime : Instant.now();
        int expiredCount = bloodRequestRepository.expireDueRequests(now);
        if (expiredCount > 0) {
            log.info("Expired {} overdue blood request(s) at {}", expiredCount, now);
        }
        return expiredCount;
    }
}
