package org.netra.features.bloodbank.dto;

import jakarta.validation.constraints.NotNull;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;

public class UpdateBloodBankVerificationRequest {

    @NotNull(message = "Verification status is required")
    private BloodBankVerificationStatus status;

    public UpdateBloodBankVerificationRequest() {
    }

    public UpdateBloodBankVerificationRequest(BloodBankVerificationStatus status) {
        this.status = status;
    }

    public BloodBankVerificationStatus getStatus() {
        return status;
    }

    public void setStatus(BloodBankVerificationStatus status) {
        this.status = status;
    }
}
