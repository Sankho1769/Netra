package org.netra.features.bloodrequest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus;

/**
 * DTO for clinical or administrative verification of a blood request.
 */
public class VerifyBloodRequestDto {

    @NotNull(message = "Verification decision is required (VERIFIED or REJECTED).")
    private BloodRequestVerificationStatus decision;

    @Size(max = 500, message = "Verification notes must not exceed 500 characters.")
    private String notes;

    public VerifyBloodRequestDto() {
    }

    public VerifyBloodRequestDto(BloodRequestVerificationStatus decision, String notes) {
        this.decision = decision;
        this.notes = notes;
    }

    public BloodRequestVerificationStatus getDecision() {
        return decision;
    }

    public void setDecision(BloodRequestVerificationStatus decision) {
        this.decision = decision;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReason() {
        return notes;
    }

    public void setReason(String reason) {
        this.notes = reason;
    }
}
