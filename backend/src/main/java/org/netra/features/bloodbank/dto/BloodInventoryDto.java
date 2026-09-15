package org.netra.features.bloodbank.dto;

import org.netra.features.bloodbank.entity.InventoryFreshness;
import org.netra.features.donor.entity.BloodGroup;

import java.time.Instant;
import java.util.UUID;

public class BloodInventoryDto {

    private UUID id;
    private UUID bloodBankId;
    private BloodGroup bloodGroup;
    private Integer unitsAvailable;
    private InventoryFreshness freshness;
    private Instant lastUpdatedAt;

    public BloodInventoryDto() {
    }

    public BloodInventoryDto(
            UUID id,
            UUID bloodBankId,
            BloodGroup bloodGroup,
            Integer unitsAvailable,
            InventoryFreshness freshness,
            Instant lastUpdatedAt) {
        this.id = id;
        this.bloodBankId = bloodBankId;
        this.bloodGroup = bloodGroup;
        this.unitsAvailable = unitsAvailable;
        this.freshness = freshness;
        this.lastUpdatedAt = lastUpdatedAt;
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
        this.unitsAvailable = unitsAvailable;
    }

    public InventoryFreshness getFreshness() {
        return freshness;
    }

    public void setFreshness(InventoryFreshness freshness) {
        this.freshness = freshness;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
