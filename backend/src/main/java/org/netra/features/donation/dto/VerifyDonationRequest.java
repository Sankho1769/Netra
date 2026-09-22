package org.netra.features.donation.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for authorized staff verifying a pending donation claim.
 */
public class VerifyDonationRequest {

    @Size(max = 500, message = "notes cannot exceed 500 characters")
    private String notes;

    public VerifyDonationRequest() {
    }

    public VerifyDonationRequest(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
