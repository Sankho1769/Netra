package org.netra.features.fulfillment.dto;

import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;

import java.time.Instant;
import java.util.UUID;

public class FulfillmentDto {

    private UUID id;
    private UUID bloodRequestId;
    private UUID donationId;
    private Integer units;
    private FulfillmentStatus status;
    private UUID createdByUserId;
    private UUID startedByUserId;
    private UUID completedByUserId;
    private UUID failedByUserId;
    private UUID cancelledByUserId;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant cancelledAt;
    private String failureReason;
    private String cancellationReason;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public FulfillmentDto() {
    }

    public static FulfillmentDto fromEntity(Fulfillment fulfillment) {
        FulfillmentDto dto = new FulfillmentDto();
        dto.setId(fulfillment.getId());
        dto.setBloodRequestId(fulfillment.getBloodRequestId());
        dto.setDonationId(fulfillment.getDonationId());
        dto.setUnits(fulfillment.getUnits());
        dto.setStatus(fulfillment.getStatus());
        dto.setCreatedByUserId(fulfillment.getCreatedByUserId());
        dto.setStartedByUserId(fulfillment.getStartedByUserId());
        dto.setCompletedByUserId(fulfillment.getCompletedByUserId());
        dto.setFailedByUserId(fulfillment.getFailedByUserId());
        dto.setCancelledByUserId(fulfillment.getCancelledByUserId());
        dto.setStartedAt(fulfillment.getStartedAt());
        dto.setCompletedAt(fulfillment.getCompletedAt());
        dto.setFailedAt(fulfillment.getFailedAt());
        dto.setCancelledAt(fulfillment.getCancelledAt());
        dto.setFailureReason(fulfillment.getFailureReason());
        dto.setCancellationReason(fulfillment.getCancellationReason());
        dto.setNotes(fulfillment.getNotes());
        dto.setCreatedAt(fulfillment.getCreatedAt());
        dto.setUpdatedAt(fulfillment.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
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
}
