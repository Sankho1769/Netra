package org.netra.features.fulfillment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.ValidationException;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fulfillment Lifecycle & State Machine Tests")
class FulfillmentLifecycleTest {

    private final UUID requestId = UUID.randomUUID();
    private final UUID donationId = UUID.randomUUID();
    private final UUID creatorId = UUID.randomUUID();
    private final UUID operatorId = UUID.randomUUID();

    @Test
    @DisplayName("Initial State: Newly created fulfillment starts in READY status")
    void initialStateIsReady() {
        Fulfillment fulfillment = new Fulfillment(requestId, donationId, 1, creatorId, "Initial notes");
        assertEquals(FulfillmentStatus.READY, fulfillment.getStatus());
        assertEquals(1, fulfillment.getUnits());
        assertEquals(requestId, fulfillment.getBloodRequestId());
        assertEquals(donationId, fulfillment.getDonationId());
        assertEquals(creatorId, fulfillment.getCreatedByUserId());
        assertNull(fulfillment.getStartedAt());
        assertNull(fulfillment.getCompletedAt());
        assertNull(fulfillment.getCancelledAt());
        assertNull(fulfillment.getFailedAt());
    }

    @Test
    @DisplayName("Happy Path: READY -> IN_PROGRESS -> FULFILLED")
    void happyPathReadyToInProgressToFulfilled() {
        Fulfillment fulfillment = new Fulfillment(requestId, donationId, 2, creatorId, "Notes");
        Instant now = Instant.now();

        // 1. Start: READY -> IN_PROGRESS
        fulfillment.start(operatorId, now);
        assertEquals(FulfillmentStatus.IN_PROGRESS, fulfillment.getStatus());
        assertEquals(operatorId, fulfillment.getStartedByUserId());
        assertEquals(now, fulfillment.getStartedAt());

        // 2. Complete: IN_PROGRESS -> FULFILLED
        Instant completeTime = now.plusSeconds(3600);
        fulfillment.complete(operatorId, completeTime);
        assertEquals(FulfillmentStatus.FULFILLED, fulfillment.getStatus());
        assertEquals(operatorId, fulfillment.getCompletedByUserId());
        assertEquals(completeTime, fulfillment.getCompletedAt());
    }

    @Test
    @DisplayName("Cancellation Path: READY -> CANCELLED")
    void readyToCancelled() {
        Fulfillment fulfillment = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        Instant now = Instant.now();

        fulfillment.cancel(creatorId, "No longer needed", now);
        assertEquals(FulfillmentStatus.CANCELLED, fulfillment.getStatus());
        assertEquals(creatorId, fulfillment.getCancelledByUserId());
        assertEquals("No longer needed", fulfillment.getCancellationReason());
        assertEquals(now, fulfillment.getCancelledAt());
    }

    @Test
    @DisplayName("Failure Path: READY -> IN_PROGRESS -> FAILED")
    void inProgressToFailed() {
        Fulfillment fulfillment = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        Instant now = Instant.now();

        fulfillment.start(operatorId, now);
        assertEquals(FulfillmentStatus.IN_PROGRESS, fulfillment.getStatus());

        Instant failTime = now.plusSeconds(1800);
        fulfillment.fail(operatorId, "Crossmatch mismatch during testing", failTime);
        assertEquals(FulfillmentStatus.FAILED, fulfillment.getStatus());
        assertEquals(operatorId, fulfillment.getFailedByUserId());
        assertEquals("Crossmatch mismatch during testing", fulfillment.getFailureReason());
        assertEquals(failTime, fulfillment.getFailedAt());
    }

    @Test
    @DisplayName("Invalid Transitions: Disallowed state changes throw ValidationException")
    void invalidTransitionsThrowValidationException() {
        Instant now = Instant.now();

        // Cannot skip IN_PROGRESS: READY -> FULFILLED is invalid
        Fulfillment f1 = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        assertThrows(ValidationException.class, () -> f1.complete(operatorId, now));

        // Cannot fail from READY: READY -> FAILED is invalid
        assertThrows(ValidationException.class, () -> f1.fail(operatorId, "Reason", now));

        // Cannot cancel from IN_PROGRESS: IN_PROGRESS -> CANCELLED is invalid
        Fulfillment f2 = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        f2.start(operatorId, now);
        assertThrows(ValidationException.class, () -> f2.cancel(creatorId, "Reason", now));

        // Terminal states cannot transition:
        // FULFILLED is terminal
        Fulfillment f3 = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        f3.start(operatorId, now);
        f3.complete(operatorId, now);
        assertThrows(ValidationException.class, () -> f3.start(operatorId, now));
        assertThrows(ValidationException.class, () -> f3.complete(operatorId, now));
        assertThrows(ValidationException.class, () -> f3.fail(operatorId, "Reason", now));
        assertThrows(ValidationException.class, () -> f3.cancel(creatorId, "Reason", now));

        // CANCELLED is terminal
        Fulfillment f4 = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        f4.cancel(creatorId, "Reason", now);
        assertThrows(ValidationException.class, () -> f4.start(operatorId, now));
        assertThrows(ValidationException.class, () -> f4.complete(operatorId, now));

        // FAILED is terminal
        Fulfillment f5 = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        f5.start(operatorId, now);
        f5.fail(operatorId, "Reason", now);
        assertThrows(ValidationException.class, () -> f5.complete(operatorId, now));
        assertThrows(ValidationException.class, () -> f5.start(operatorId, now));
    }

    @Test
    @DisplayName("Validation: Failure and Cancellation require non-empty reasons")
    void reasonsAreMandatory() {
        Fulfillment f1 = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        assertThrows(ValidationException.class, () -> f1.cancel(creatorId, null, Instant.now()));
        assertThrows(ValidationException.class, () -> f1.cancel(creatorId, "   ", Instant.now()));

        Fulfillment f2 = new Fulfillment(requestId, donationId, 1, creatorId, "Notes");
        f2.start(operatorId, Instant.now());
        assertThrows(ValidationException.class, () -> f2.fail(operatorId, null, Instant.now()));
        assertThrows(ValidationException.class, () -> f2.fail(operatorId, "   ", Instant.now()));
    }
}
