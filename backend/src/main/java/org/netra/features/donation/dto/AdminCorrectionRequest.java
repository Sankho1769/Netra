package org.netra.features.donation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.netra.features.donation.entity.DonationVerificationStatus;

/**
 * Request payload for administrator correcting a donation record.
 */
public class AdminCorrectionRequest {

    @NotNull(message = "targetStatus is required")
    private DonationVerificationStatus targetStatus;

    @NotBlank(message = "correctionReason is required")
    @Size(min = 3, max = 500, message = "correctionReason must be between 3 and 500 characters")
    private String correctionReason;

    public AdminCorrectionRequest() {
    }

    public AdminCorrectionRequest(DonationVerificationStatus targetStatus, String correctionReason) {
        this.targetStatus = targetStatus;
        this.correctionReason = correctionReason;
    }

    public DonationVerificationStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(DonationVerificationStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getCorrectionReason() {
        return correctionReason;
    }

    public void setCorrectionReason(String correctionReason) {
        this.correctionReason = correctionReason;
    }
}
