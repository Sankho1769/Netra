package org.netra.features.fulfillment.event;

import java.util.UUID;

public class FulfillmentCancelledEvent {
    private final UUID fulfillmentId;
    private final UUID bloodRequestId;
    private final UUID donationId;
    private final UUID requesterUserId;
    private final UUID donorUserId;
    private final String cancellationReason;

    public FulfillmentCancelledEvent(UUID fulfillmentId, UUID bloodRequestId, UUID donationId, UUID requesterUserId, UUID donorUserId, String cancellationReason) {
        this.fulfillmentId = fulfillmentId;
        this.bloodRequestId = bloodRequestId;
        this.donationId = donationId;
        this.requesterUserId = requesterUserId;
        this.donorUserId = donorUserId;
        this.cancellationReason = cancellationReason;
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

    public String getCancellationReason() {
        return cancellationReason;
    }
}
