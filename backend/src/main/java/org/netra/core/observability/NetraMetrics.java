package org.netra.core.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Centralized registry and helper for NETRA operational and business metrics.
 * Uses Micrometer for clean aggregation without recording PII or raw medical data.
 */
@Component
public class NetraMetrics {

    private final MeterRegistry registry;

    public NetraMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public MeterRegistry getRegistry() {
        return registry;
    }

    // =========================================================================
    // 1. BUSINESS METRICS
    // =========================================================================

    public void incrementBloodRequestsCreated(String urgency, String bloodGroup) {
        Counter.builder("netra.blood_requests.created.total")
                .description("Total number of blood requests created")
                .tag("urgency", urgency != null ? urgency : "UNKNOWN")
                .tag("blood_group", bloodGroup != null ? bloodGroup : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void incrementEmergencyRequestsCreated() {
        Counter.builder("netra.emergency_requests.created.total")
                .description("Total number of emergency blood requests created")
                .register(registry)
                .increment();
    }

    public void incrementDonorMatchesCreated(int count) {
        Counter.builder("netra.donor_matches.created.total")
                .description("Total number of donor matches generated")
                .register(registry)
                .increment(count);
    }

    public void incrementDonorMatchResponse(String outcome) {
        Counter.builder("netra.donor_matches.response.total")
                .description("Total number of donor match responses")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    public void incrementDonationsClaimed(String sourceType) {
        Counter.builder("netra.donations.claimed.total")
                .description("Total number of donations claimed")
                .tag("source_type", sourceType != null ? sourceType : "DIRECT")
                .register(registry)
                .increment();
    }

    public void incrementDonationsVerified() {
        Counter.builder("netra.donations.verified.total")
                .description("Total number of donations verified by blood banks")
                .register(registry)
                .increment();
    }

    public void incrementFulfillmentsCreated() {
        Counter.builder("netra.fulfillments.created.total")
                .description("Total number of fulfillments created")
                .register(registry)
                .increment();
    }

    public void incrementFulfillmentsCompleted() {
        Counter.builder("netra.fulfillments.completed.total")
                .description("Total number of fulfillments completed")
                .register(registry)
                .increment();
    }

    public void incrementBloodRequestsCancelled() {
        Counter.builder("netra.blood_requests.cancelled.total")
                .description("Total number of blood requests cancelled")
                .register(registry)
                .increment();
    }

    public void incrementNotificationsCreated(String type) {
        Counter.builder("netra.notifications.created.total")
                .description("Total number of in-app notifications generated")
                .tag("type", type != null ? type : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void incrementNotificationDelivery(String outcome) {
        Counter.builder("netra.notifications.delivery.total")
                .description("Total number of push notification delivery outcomes")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    // =========================================================================
    // 2. CONCURRENCY & CONTENTION METRICS
    // =========================================================================

    public void incrementOptimisticLockConflict(String aggregate) {
        Counter.builder("netra.concurrency.optimistic_lock_conflicts.total")
                .description("Total number of optimistic locking (@Version) conflicts caught")
                .tag("aggregate", aggregate != null ? aggregate : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void incrementFulfillmentContention() {
        Counter.builder("netra.concurrency.fulfillment_contention.total")
                .description("Total number of rejected concurrent fulfillment allocations exceeding required units")
                .register(registry)
                .increment();
    }

    public void incrementRateLimitRejected(String endpointGroup) {
        Counter.builder("netra.ratelimit.rejected.total")
                .description("Total number of requests rejected by rate limiting (HTTP 429)")
                .tag("endpoint_group", endpointGroup != null ? endpointGroup : "DEFAULT")
                .register(registry)
                .increment();
    }

    // =========================================================================
    // 3. BACKGROUND SCHEDULER METRICS
    // =========================================================================

    public void recordScheduledJobExecution(String jobName, String outcome, long durationMs, int processedItems) {
        Counter.builder("netra.scheduled_jobs.executions.total")
                .description("Total scheduled background job executions")
                .tag("job_name", jobName)
                .tag("outcome", outcome)
                .register(registry)
                .increment();

        if (processedItems > 0) {
            Counter.builder("netra.scheduled_jobs.processed_items.total")
                    .description("Total items processed across scheduled background jobs")
                    .tag("job_name", jobName)
                    .register(registry)
                    .increment(processedItems);
        }

        Timer.builder("netra.scheduled_jobs.duration")
                .description("Execution duration of scheduled background jobs")
                .tag("job_name", jobName)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // =========================================================================
    // 4. TIMERS
    // =========================================================================

    public void recordMatchingDuration(long durationMs) {
        Timer.builder("netra.matching.duration")
                .description("Execution duration for donor matching algorithm")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
