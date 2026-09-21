package org.netra.features.notification.event;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BloodRequestCancelledEvent {

    private final UUID bloodRequestId;
    private final UUID requesterUserId;
    private final List<UUID> affectedDonorUserIds;

    public BloodRequestCancelledEvent(UUID bloodRequestId, UUID requesterUserId, List<UUID> affectedDonorUserIds) {
        this.bloodRequestId = bloodRequestId;
        this.requesterUserId = requesterUserId;
        this.affectedDonorUserIds = affectedDonorUserIds != null ? List.copyOf(affectedDonorUserIds) : Collections.emptyList();
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public UUID getRequesterUserId() {
        return requesterUserId;
    }

    public List<UUID> getAffectedDonorUserIds() {
        return affectedDonorUserIds;
    }
}
