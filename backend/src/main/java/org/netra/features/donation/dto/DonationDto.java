package org.netra.features.donation.dto;

import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Summary DTO representing a donation in list queries.
 */
public class DonationDto {

    private UUID id;
    private UUID donorUserId;
    private DonationSourceType sourceType;
    private UUID bloodRequestId;
    private UUID donationEventId;
    private LocalDate donationDate;
    private DonationVerificationStatus verificationStatus;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public DonationDto() {
    }

    public DonationDto(UUID id,
                       UUID donorUserId,
                       DonationSourceType sourceType,
                       UUID bloodRequestId,
                       UUID donationEventId,
                       LocalDate donationDate,
                       DonationVerificationStatus verificationStatus,
                       Instant verifiedAt,
                       Instant createdAt,
                       Instant updatedAt) {
        this.id = id;
        this.donorUserId = donorUserId;
        this.sourceType = sourceType;
        this.bloodRequestId = bloodRequestId;
        this.donationEventId = donationEventId;
        this.donationDate = donationDate;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DonationDto fromEntity(Donation d) {
        if (d == null) return null;
        return new DonationDto(
                d.getId(),
                d.getDonorUserId(),
                d.getSourceType(),
                d.getBloodRequestId(),
                d.getDonationEventId(),
                d.getDonationDate(),
                d.getVerificationStatus(),
                d.getVerifiedAt(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public DonationVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(DonationVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
