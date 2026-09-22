package org.netra.features.donation.entity;

import jakarta.persistence.*;
import org.netra.core.exception.ValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity representing a verified donation record.
 * Supports optimistic concurrency control via {@code @Version}.
 */
@Entity
@Table(
    name = "donations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_donations_donor_blood_request", columnNames = {"donor_user_id", "blood_request_id"}),
        @UniqueConstraint(name = "uq_donations_donor_event", columnNames = {"donor_user_id", "donation_event_id"})
    }
)
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "donor_user_id", nullable = false, updatable = false)
    private UUID donorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32, updatable = false)
    private DonationSourceType sourceType;

    @Column(name = "blood_request_id", updatable = false)
    private UUID bloodRequestId;

    @Column(name = "donation_event_id", updatable = false)
    private UUID donationEventId;

    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private DonationVerificationStatus verificationStatus = DonationVerificationStatus.PENDING_VERIFICATION;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public Donation() {
    }

    public Donation(UUID donorUserId,
                    DonationSourceType sourceType,
                    UUID bloodRequestId,
                    UUID donationEventId,
                    LocalDate donationDate,
                    String notes) {
        this.donorUserId = Objects.requireNonNull(donorUserId, "donorUserId must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.donationDate = Objects.requireNonNull(donationDate, "donationDate must not be null");
        this.bloodRequestId = bloodRequestId;
        this.donationEventId = donationEventId;
        this.notes = notes;
        this.verificationStatus = DonationVerificationStatus.PENDING_VERIFICATION;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.version = 0L;
        validateSourceReferences();
    }

    public void validateSourceReferences() {
        if (sourceType == DonationSourceType.BLOOD_REQUEST) {
            if (bloodRequestId == null || donationEventId != null) {
                throw new ValidationException("BLOOD_REQUEST donation must reference bloodRequestId and not donationEventId.");
            }
        } else if (sourceType == DonationSourceType.DONATION_EVENT) {
            if (donationEventId == null || bloodRequestId != null) {
                throw new ValidationException("DONATION_EVENT donation must reference donationEventId and not bloodRequestId.");
            }
        }
    }

    public void verify(UUID verifierUserId, Instant now, String verifierNotes) {
        if (!verificationStatus.canTransitionTo(DonationVerificationStatus.VERIFIED)) {
            throw new ValidationException("Cannot verify donation in status: " + verificationStatus);
        }
        this.verificationStatus = DonationVerificationStatus.VERIFIED;
        this.verifiedByUserId = Objects.requireNonNull(verifierUserId, "verifierUserId must not be null");
        this.verifiedAt = now != null ? now : Instant.now();
        this.updatedAt = this.verifiedAt;
        if (verifierNotes != null && !verifierNotes.isBlank()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + "[Verifier Notes]: " + verifierNotes.trim();
        }
    }

    public void reject(UUID verifierUserId, Instant now, String reason) {
        if (!verificationStatus.canTransitionTo(DonationVerificationStatus.REJECTED)) {
            throw new ValidationException("Cannot reject donation in status: " + verificationStatus);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Rejection reason is required.");
        }
        this.verificationStatus = DonationVerificationStatus.REJECTED;
        this.verifiedByUserId = Objects.requireNonNull(verifierUserId, "verifierUserId must not be null");
        this.verifiedAt = now != null ? now : Instant.now();
        this.rejectionReason = reason.trim();
        this.updatedAt = this.verifiedAt;
    }

    public void cancel(Instant now) {
        if (!verificationStatus.canTransitionTo(DonationVerificationStatus.CANCELLED)) {
            throw new ValidationException("Cannot cancel donation in status: " + verificationStatus);
        }
        this.verificationStatus = DonationVerificationStatus.CANCELLED;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void adminCorrection(DonationVerificationStatus targetStatus, String reason, UUID adminUserId, Instant now) {
        if (targetStatus == null) {
            throw new ValidationException("Target status is required for admin correction.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Correction reason is required.");
        }
        this.verificationStatus = targetStatus;
        this.verifiedByUserId = adminUserId;
        this.rejectionReason = targetStatus == DonationVerificationStatus.REJECTED ? reason.trim() : null;
        this.updatedAt = now != null ? now : Instant.now();
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "[Admin Correction by " + adminUserId + "]: " + reason.trim();
    }

    // Getters and setters
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
