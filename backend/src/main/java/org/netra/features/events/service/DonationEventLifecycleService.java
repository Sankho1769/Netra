package org.netra.features.events.service;

import org.netra.features.events.repository.DonationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DonationEventLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(DonationEventLifecycleService.class);

    private final DonationEventRepository donationEventRepository;
    private final org.netra.core.observability.NetraMetrics netraMetrics;

    public DonationEventLifecycleService(DonationEventRepository donationEventRepository) {
        this(donationEventRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DonationEventLifecycleService(
            DonationEventRepository donationEventRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            org.netra.core.observability.NetraMetrics netraMetrics) {
        this.donationEventRepository = donationEventRepository;
        this.netraMetrics = netraMetrics;
    }

    /**
     * Executes scheduled background lifecycle transitions at the configured interval.
     */
    @Scheduled(fixedDelayString = "${netra.events.lifecycle-interval-ms:60000}")
    @Transactional
    public void scheduledLifecycleTransitions() {
        long start = System.currentTimeMillis();
        String outcome = "SUCCESS";
        int count = 0;
        try {
            LifecycleTransitionResult result = processLifecycleTransitions(Instant.now());
            count = result.totalTransitions();
        } catch (Exception e) {
            outcome = "FAILURE";
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (netraMetrics != null) {
                netraMetrics.recordScheduledJobExecution("donation_event_lifecycle", outcome, duration, count);
            }
        }
    }

    /**
     * Performs atomic conditional lifecycle transitions in strict chronological order:
     * 1. close registrations: PUBLISHED -> REGISTRATION_CLOSED when registrationCloseAt <= now
     * 2. start events: REGISTRATION_CLOSED -> ONGOING when startAt <= now
     * 3. complete events: ONGOING -> COMPLETED when endAt <= now
     *
     * Terminal states (CANCELLED, REJECTED) and unapproved states (DRAFT, PENDING_APPROVAL)
     * are never modified.
     *
     * @param referenceTime Timestamp to evaluate against, or now if null.
     * @return Affected row counts per transition stage.
     */
    @Transactional
    public LifecycleTransitionResult processLifecycleTransitions(Instant referenceTime) {
        Instant now = referenceTime != null ? referenceTime : Instant.now();

        // 1. close registrations: PUBLISHED -> REGISTRATION_CLOSED
        int closedCount = donationEventRepository.closeDueRegistrations(now);

        // 2. start events: REGISTRATION_CLOSED -> ONGOING
        int startedCount = donationEventRepository.startDueEvents(now);

        // 3. complete events: ONGOING -> COMPLETED
        int completedCount = donationEventRepository.completeDueEvents(now);

        if (closedCount > 0 || startedCount > 0 || completedCount > 0) {
            log.info("Donation event lifecycle transition executed at {}: closed={}, started={}, completed={}",
                    now, closedCount, startedCount, completedCount);
        }

        return new LifecycleTransitionResult(closedCount, startedCount, completedCount);
    }

    public record LifecycleTransitionResult(int closedCount, int startedCount, int completedCount) {
        public int totalTransitions() {
            return closedCount + startedCount + completedCount;
        }
    }
}
