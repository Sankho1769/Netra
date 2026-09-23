package org.netra.features.fulfillment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateFulfillmentRequest {

    @NotNull(message = "bloodRequestId is required.")
    private UUID bloodRequestId;

    @NotNull(message = "donationId is required.")
    private UUID donationId;

    @NotNull(message = "units is required.")
    @Min(value = 1, message = "units must be at least 1.")
    @Max(value = 50, message = "units must not exceed 50.")
    private Integer units = 1;

    @Size(max = 1000, message = "notes must not exceed 1000 characters.")
    private String notes;

    public CreateFulfillmentRequest() {
    }

    public CreateFulfillmentRequest(UUID bloodRequestId, UUID donationId, Integer units, String notes) {
        this.bloodRequestId = bloodRequestId;
        this.donationId = donationId;
        this.units = units;
        this.notes = notes;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
