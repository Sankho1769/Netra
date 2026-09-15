package org.netra.features.bloodbank.dto;

import org.netra.features.bloodbank.entity.BloodBankAccountStatus;

import java.time.Instant;
import java.util.UUID;

public class BloodBankAccountDto {

    private UUID id;
    private UUID userId;
    private UUID bloodBankId;
    private BloodBankAccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public BloodBankAccountDto() {
    }

    public BloodBankAccountDto(UUID id, UUID userId, UUID bloodBankId, BloodBankAccountStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.bloodBankId = bloodBankId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(UUID bloodBankId) {
        this.bloodBankId = bloodBankId;
    }

    public BloodBankAccountStatus getStatus() {
        return status;
    }

    public void setStatus(BloodBankAccountStatus status) {
        this.status = status;
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
