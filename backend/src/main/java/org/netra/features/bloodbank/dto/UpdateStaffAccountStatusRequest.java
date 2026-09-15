package org.netra.features.bloodbank.dto;

import jakarta.validation.constraints.NotNull;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;

public class UpdateStaffAccountStatusRequest {

    @NotNull(message = "Account status is required")
    private BloodBankAccountStatus status;

    public UpdateStaffAccountStatusRequest() {
    }

    public UpdateStaffAccountStatusRequest(BloodBankAccountStatus status) {
        this.status = status;
    }

    public BloodBankAccountStatus getStatus() {
        return status;
    }

    public void setStatus(BloodBankAccountStatus status) {
        this.status = status;
    }
}
