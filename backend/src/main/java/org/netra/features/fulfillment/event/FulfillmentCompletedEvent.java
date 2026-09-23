package org.netra.features.fulfillment.event;

import java.util.UUID;

public class FulfillmentCompletedEvent {
    private final UUID fulfillmentId;
    private final UUID bloodRequestId;
    private final UUID donationId;
    private final UUID requesterUserId;
    private final UUID donorUserId;
    private final int unitsFulfilled;
    private final boolean requestCompleted;

    public FulfillmentCompletedEvent(UUID fulfillmentId, UUID bloodRequestId, UUID donationId, UUID requesterUserId, UUID donorUserId, int unitsFulfilled, boolean requestCompleted) {
        this.fulfillmentId = fulfillmentId;
        this.bloodRequestId = bloodRequestId;
        this.donationId = donationId;
        this.requesterUserId = requesterUserId;
        this.donorUserId = donorUserId;
        this.unitsFulfilled = unitsFulfilled;
        this.requestCompleted = requestCompleted;
    }

    public UUID getFulfillmentId() {
        return fulfillmentId;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public UUID getDonationId() {
        return donationId;
    }

    public UUID getRequesterUserId() {
        return requesterUserId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public int getUnitsFulfilled() {
        return unitsFulfilled;
    }

    public boolean isRequestCompleted() {
        return requestCompleted;
    }
}
