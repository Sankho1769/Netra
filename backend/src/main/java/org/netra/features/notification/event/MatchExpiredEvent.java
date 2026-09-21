package org.netra.features.notification.event;

import java.util.UUID;

public class MatchExpiredEvent {

    private final UUID matchId;
    private final UUID bloodRequestId;
    private final UUID donorUserId;
    private final UUID requesterUserId;

    public MatchExpiredEvent(UUID matchId, UUID bloodRequestId, UUID donorUserId, UUID requesterUserId) {
        this.matchId = matchId;
        this.bloodRequestId = bloodRequestId;
        this.donorUserId = donorUserId;
        this.requesterUserId = requesterUserId;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public UUID getRequesterUserId() {
        return requesterUserId;
    }
}
