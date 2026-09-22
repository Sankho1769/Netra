package org.netra.features.donation.dto;

import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Detailed DTO with context for donation views.
 */
public class DonationDetailDto {

    private UUID id;
    private UUID donorUserId;
    private String donorName;
    private DonationSourceType sourceType;
    private UUID bloodRequestId;
    private UUID donationEventId;
    private String referenceTitle;
    private String referenceLocation;
    private LocalDate donationDate;
    private DonationVerificationStatus verificationStatus;
    private Instant verifiedAt;
    private UUID verifiedByUserId;
    private String rejectionReason;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public DonationDetailDto() {
    }

    public DonationDetailDto(UUID id,
                             UUID donorUserId,
                             String donorName,
                             DonationSourceType sourceType,
                             UUID bloodRequestId,
                             UUID donationEventId,
                             String referenceTitle,
                             String referenceLocation,
                             LocalDate donationDate,
                             DonationVerificationStatus verificationStatus,
                             Instant verifiedAt,
                             UUID verifiedByUserId,
                             String rejectionReason,
                             String notes,
                             Instant createdAt,
                             Instant updatedAt) {
        this.id = id;
        this.donorUserId = donorUserId;
        this.donorName = donorName;
        this.sourceType = sourceType;
        this.bloodRequestId = bloodRequestId;
        this.donationEventId = donationEventId;
        this.referenceTitle = referenceTitle;
        this.referenceLocation = referenceLocation;
        this.donationDate = donationDate;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
        this.verifiedByUserId = verifiedByUserId;
        this.rejectionReason = rejectionReason;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DonationDetailDto fromEntity(Donation d, String donorName, String refTitle, String refLocation) {
        if (d == null) return null;
        return new DonationDetailDto(
                d.getId(),
                d.getDonorUserId(),
                donorName,
                d.getSourceType(),
                d.getBloodRequestId(),
                d.getDonationEventId(),
                refTitle,
                refLocation,
                d.getDonationDate(),
                d.getVerificationStatus(),
                d.getVerifiedAt(),
                d.getVerifiedByUserId(),
                d.getRejectionReason(),
                d.getNotes(),
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

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
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

    public String getReferenceTitle() {
        return referenceTitle;
    }

    public void setReferenceTitle(String referenceTitle) {
        this.referenceTitle = referenceTitle;
    }

    public String getReferenceLocation() {
        return referenceLocation;
    }

    public void setReferenceLocation(String referenceLocation) {
        this.referenceLocation = referenceLocation;
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

    public UUID getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(UUID verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
