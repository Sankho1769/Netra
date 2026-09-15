package org.netra.features.bloodbank.dto;

import jakarta.validation.constraints.NotNull;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;

public class UpdateOperatingStatusRequest {

    @NotNull(message = "Operating status is required")
    private BloodBankOperatingStatus operatingStatus;

    public UpdateOperatingStatusRequest() {
    }

    public UpdateOperatingStatusRequest(BloodBankOperatingStatus operatingStatus) {
        this.operatingStatus = operatingStatus;
    }

    public BloodBankOperatingStatus getOperatingStatus() {
        return operatingStatus;
    }

    public void setOperatingStatus(BloodBankOperatingStatus operatingStatus) {
        this.operatingStatus = operatingStatus;
    }
}
