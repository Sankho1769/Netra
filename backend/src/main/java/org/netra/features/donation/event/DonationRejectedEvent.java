package org.netra.features.donation.event;

import org.netra.features.donation.entity.DonationSourceType;

import java.util.UUID;

public class DonationRejectedEvent {

    private final UUID donationId;
    private final UUID donorUserId;
    private final UUID verifierUserId;
    private final DonationSourceType sourceType;
    private final String rejectionReason;

    public DonationRejectedEvent(UUID donationId, UUID donorUserId, UUID verifierUserId, DonationSourceType sourceType, String rejectionReason) {
        this.donationId = donationId;
        this.donorUserId = donorUserId;
        this.verifierUserId = verifierUserId;
        this.sourceType = sourceType;
        this.rejectionReason = rejectionReason;
    }

    public UUID getDonationId() {
        return donationId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public UUID getVerifierUserId() {
        return verifierUserId;
    }

    public DonationSourceType getSourceType() {
        return sourceType;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
