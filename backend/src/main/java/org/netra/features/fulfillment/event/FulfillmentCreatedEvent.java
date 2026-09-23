package org.netra.features.fulfillment.event;

import java.util.UUID;

public class FulfillmentCreatedEvent {
    private final UUID fulfillmentId;
    private final UUID bloodRequestId;
    private final UUID donationId;
    private final UUID requesterUserId;
    private final UUID donorUserId;
    private final int units;

    public FulfillmentCreatedEvent(UUID fulfillmentId, UUID bloodRequestId, UUID donationId, UUID requesterUserId, UUID donorUserId, int units) {
        this.fulfillmentId = fulfillmentId;
        this.bloodRequestId = bloodRequestId;
        this.donationId = donationId;
        this.requesterUserId = requesterUserId;
        this.donorUserId = donorUserId;
        this.units = units;
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

    public int getUnits() {
        return units;
    }
}
