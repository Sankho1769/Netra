package org.netra.features.fulfillment.entity;

import jakarta.persistence.*;
import org.netra.core.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity representing a Fulfillment aggregate.
 * Operational fulfillment of a Blood Request using a VERIFIED Donation.
 * Concurrency protected via {@code @Version}.
 */
@Entity
@Table(
    name = "fulfillments",
    indexes = {
        @Index(name = "idx_fulfillments_blood_request", columnList = "blood_request_id"),
        @Index(name = "idx_fulfillments_donation", columnList = "donation_id"),
        @Index(name = "idx_fulfillments_status", columnList = "status"),
        @Index(name = "idx_fulfillments_created_by", columnList = "created_by_user_id"),
        @Index(name = "idx_fulfillments_created_at", columnList = "created_at DESC")
    }
)
public class Fulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "blood_request_id", nullable = false, updatable = false)
    private UUID bloodRequestId;

    @Column(name = "donation_id", nullable = false, updatable = false)
    private UUID donationId;

    @Column(name = "units", nullable = false, updatable = false)
    private Integer units = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FulfillmentStatus status = FulfillmentStatus.READY;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "started_by_user_id")
    private UUID startedByUserId;

    @Column(name = "completed_by_user_id")
    private UUID completedByUserId;

    @Column(name = "failed_by_user_id")
    private UUID failedByUserId;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public Fulfillment() {
    }

    public Fulfillment(UUID bloodRequestId,
                       UUID donationId,
                       Integer units,
                       UUID createdByUserId,
                       String notes) {
        this.bloodRequestId = Objects.requireNonNull(bloodRequestId, "bloodRequestId must not be null");
        this.donationId = Objects.requireNonNull(donationId, "donationId must not be null");
        this.units = (units != null && units > 0) ? units : 1;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
        this.notes = notes;
        this.status = FulfillmentStatus.READY;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }

    public void start(UUID operatorUserId, Instant now) {
        if (!status.canTransitionTo(FulfillmentStatus.IN_PROGRESS)) {
            throw new ValidationException("Cannot transition fulfillment from " + status + " to IN_PROGRESS.");
        }
        this.status = FulfillmentStatus.IN_PROGRESS;
        this.startedByUserId = Objects.requireNonNull(operatorUserId, "operatorUserId must not be null");
        this.startedAt = now != null ? now : Instant.now();
        this.updatedAt = this.startedAt;
    }

    public void complete(UUID operatorUserId, Instant now) {
        if (!status.canTransitionTo(FulfillmentStatus.FULFILLED)) {
            throw new ValidationException("Cannot transition fulfillment from " + status + " to FULFILLED.");
        }
        this.status = FulfillmentStatus.FULFILLED;
        this.completedByUserId = Objects.requireNonNull(operatorUserId, "operatorUserId must not be null");
        this.completedAt = now != null ? now : Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void fail(UUID operatorUserId, String reason, Instant now) {
        if (!status.canTransitionTo(FulfillmentStatus.FAILED)) {
            throw new ValidationException("Cannot transition fulfillment from " + status + " to FAILED.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Failure reason is required.");
        }
        this.status = FulfillmentStatus.FAILED;
        this.failedByUserId = Objects.requireNonNull(operatorUserId, "operatorUserId must not be null");
        this.failureReason = reason.trim();
        this.failedAt = now != null ? now : Instant.now();
        this.updatedAt = this.failedAt;
    }

    public void cancel(UUID operatorUserId, String reason, Instant now) {
        if (!status.canTransitionTo(FulfillmentStatus.CANCELLED)) {
            throw new ValidationException("Cannot transition fulfillment from " + status + " to CANCELLED.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Cancellation reason is required.");
        }
        this.status = FulfillmentStatus.CANCELLED;
        this.cancelledByUserId = Objects.requireNonNull(operatorUserId, "operatorUserId must not be null");
        this.cancellationReason = reason.trim();
        this.cancelledAt = now != null ? now : Instant.now();
        this.updatedAt = this.cancelledAt;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public void setBloodRequestId(UUID bloodRequestId) {
        this.bloodRequestId = bloodRequestId;
    }

    public UUID getDonationId() {
        return donationId;
    }

    public void setDonationId(UUID donationId) {
        this.donationId = donationId;
    }

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public void setStatus(FulfillmentStatus status) {
        this.status = status;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public UUID getStartedByUserId() {
        return startedByUserId;
    }

    public void setStartedByUserId(UUID startedByUserId) {
        this.startedByUserId = startedByUserId;
    }

    public UUID getCompletedByUserId() {
        return completedByUserId;
    }

    public void setCompletedByUserId(UUID completedByUserId) {
        this.completedByUserId = completedByUserId;
    }

    public UUID getFailedByUserId() {
        return failedByUserId;
    }

    public void setFailedByUserId(UUID failedByUserId) {
        this.failedByUserId = failedByUserId;
    }

    public UUID getCancelledByUserId() {
        return cancelledByUserId;
    }

    public void setCancelledByUserId(UUID cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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
