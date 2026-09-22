package org.netra.features.donation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.netra.features.donation.entity.DonationSourceType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for authorized staff directly recording and verifying a donation.
 */
public class RecordVerifiedDonationRequest {

    @NotNull(message = "donorUserId is required")
    private UUID donorUserId;

    @NotNull(message = "sourceType is required (BLOOD_REQUEST or DONATION_EVENT)")
    private DonationSourceType sourceType;

    private UUID bloodRequestId;

    private UUID donationEventId;

    @NotNull(message = "donationDate is required")
    @PastOrPresent(message = "donationDate cannot be in the future")
    private LocalDate donationDate;

    @Size(max = 500, message = "notes cannot exceed 500 characters")
    private String notes;

    public RecordVerifiedDonationRequest() {
    }

    public RecordVerifiedDonationRequest(UUID donorUserId, DonationSourceType sourceType, UUID bloodRequestId, UUID donationEventId, LocalDate donationDate, String notes) {
        this.donorUserId = donorUserId;
        this.sourceType = sourceType;
        this.bloodRequestId = bloodRequestId;
        this.donationEventId = donationEventId;
        this.donationDate = donationDate;
        this.notes = notes;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(UUID donorUserId) {
        this.donorUserId = donorUserId;
    }

    public DonationSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(DonationSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public void setBloodRequestId(UUID bloodRequestId) {
        this.bloodRequestId = bloodRequestId;
    }

    public UUID getDonationEventId() {
        return donationEventId;
    }

    public void setDonationEventId(UUID donationEventId) {
        this.donationEventId = donationEventId;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
