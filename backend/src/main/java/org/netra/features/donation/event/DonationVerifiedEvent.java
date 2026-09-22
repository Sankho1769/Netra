package org.netra.features.donation.event;

import org.netra.features.donation.entity.DonationSourceType;

import java.time.LocalDate;
import java.util.UUID;

public class DonationVerifiedEvent {

    private final UUID donationId;
    private final UUID donorUserId;
    private final UUID verifierUserId;
    private final DonationSourceType sourceType;
    private final LocalDate donationDate;

    public DonationVerifiedEvent(UUID donationId, UUID donorUserId, UUID verifierUserId, DonationSourceType sourceType, LocalDate donationDate) {
        this.donationId = donationId;
        this.donorUserId = donorUserId;
        this.verifierUserId = verifierUserId;
        this.sourceType = sourceType;
        this.donationDate = donationDate;
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

    public LocalDate getDonationDate() {
        return donationDate;
    }
}
