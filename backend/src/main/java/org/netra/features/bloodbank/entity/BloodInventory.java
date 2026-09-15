package org.netra.features.bloodbank.entity;

import jakarta.persistence.*;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupConverter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "blood_inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_blood_inventory_bank_group", columnNames = {"blood_bank_id", "blood_group"})
    }
)
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blood_bank_id", nullable = false)
    private UUID bloodBankId;

    @Convert(converter = BloodGroupConverter.class)
    @Column(name = "blood_group", nullable = false, length = 8)
    private BloodGroup bloodGroup;

    @Column(name = "units_available", nullable = false)
    private Integer unitsAvailable = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt = Instant.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BloodInventory() {
    }

    public BloodInventory(UUID bloodBankId, BloodGroup bloodGroup, Integer unitsAvailable) {
        if (unitsAvailable < 0) {
            throw new IllegalArgumentException("Blood inventory units cannot be negative.");
        }
        this.bloodBankId = bloodBankId;
        this.bloodGroup = bloodGroup;
        this.unitsAvailable = unitsAvailable;
        this.lastUpdatedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(UUID bloodBankId) {
        this.bloodBankId = bloodBankId;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(Integer unitsAvailable) {
        if (unitsAvailable != null && unitsAvailable < 0) {
            throw new IllegalArgumentException("Blood inventory units cannot be negative.");
        }
        this.unitsAvailable = unitsAvailable;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
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
