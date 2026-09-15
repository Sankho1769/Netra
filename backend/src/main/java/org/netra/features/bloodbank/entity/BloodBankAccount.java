package org.netra.features.bloodbank.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "blood_bank_accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_blood_bank_account_user_bank", columnNames = {"user_id", "blood_bank_id"})
    }
)
public class BloodBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "blood_bank_id", nullable = false)
    private UUID bloodBankId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BloodBankAccountStatus status = BloodBankAccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BloodBankAccount() {
    }

    public BloodBankAccount(UUID userId, UUID bloodBankId, BloodBankAccountStatus status) {
        this.userId = userId;
        this.bloodBankId = bloodBankId;
        this.status = status != null ? status : BloodBankAccountStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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
