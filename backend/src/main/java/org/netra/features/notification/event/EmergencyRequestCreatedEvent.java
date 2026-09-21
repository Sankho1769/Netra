package org.netra.features.notification.event;

import org.netra.features.donor.entity.BloodGroup;

import java.time.Instant;
import java.util.UUID;

public class EmergencyRequestCreatedEvent {

    private final UUID bloodRequestId;
    private final UUID requesterUserId;
    private final BloodGroup bloodGroup;
    private final Double latitude;
    private final Double longitude;
    private final Instant requiredBy;

    public EmergencyRequestCreatedEvent(
            UUID bloodRequestId,
            UUID requesterUserId,
            BloodGroup bloodGroup,
            Double latitude,
            Double longitude,
            Instant requiredBy) {
        this.bloodRequestId = bloodRequestId;
        this.requesterUserId = requesterUserId;
        this.bloodGroup = bloodGroup;
        this.latitude = latitude;
        this.longitude = longitude;
        this.requiredBy = requiredBy;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public UUID getRequesterUserId() {
        return requesterUserId;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Instant getRequiredBy() {
        return requiredBy;
    }
}
