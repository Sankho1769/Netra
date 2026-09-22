package org.netra.features.donation.event;

import org.netra.features.donation.entity.DonationSourceType;

import java.time.LocalDate;
import java.util.UUID;

public class DonationSubmittedEvent {

    private final UUID donationId;
    private final UUID donorUserId;
    private final DonationSourceType sourceType;
    private final LocalDate donationDate;

    public DonationSubmittedEvent(UUID donationId, UUID donorUserId, DonationSourceType sourceType, LocalDate donationDate) {
        this.donationId = donationId;
        this.donorUserId = donorUserId;
        this.sourceType = sourceType;
        this.donationDate = donationDate;
    }

    public UUID getDonationId() {
        return donationId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public DonationSourceType getSourceType() {
        return sourceType;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }
}
