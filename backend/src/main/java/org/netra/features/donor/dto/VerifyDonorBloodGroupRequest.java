package org.netra.features.donor.dto;

import jakarta.validation.constraints.Size;

public class VerifyDonorBloodGroupRequest {

    @Size(max = 500, message = "Verification notes must not exceed 500 characters.")
    private String notes;

    public VerifyDonorBloodGroupRequest() {
    }

    public VerifyDonorBloodGroupRequest(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
