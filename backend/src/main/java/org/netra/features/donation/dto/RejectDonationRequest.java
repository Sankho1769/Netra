package org.netra.features.donation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for authorized staff rejecting a pending donation claim.
 */
public class RejectDonationRequest {

    @NotBlank(message = "rejectionReason is required")
    @Size(min = 3, max = 500, message = "rejectionReason must be between 3 and 500 characters")
    private String rejectionReason;

    public RejectDonationRequest() {
    }

    public RejectDonationRequest(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
