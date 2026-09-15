package org.netra.features.bloodbank.dto;

import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;

public class UpdateBloodBankStatusRequest {

    private BloodBankVerificationStatus verificationStatus;
    private BloodBankOperatingStatus operatingStatus;

    public UpdateBloodBankStatusRequest() {
    }

    public UpdateBloodBankStatusRequest(BloodBankVerificationStatus verificationStatus, BloodBankOperatingStatus operatingStatus) {
        this.verificationStatus = verificationStatus;
        this.operatingStatus = operatingStatus;
    }

    public BloodBankVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(BloodBankVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public BloodBankOperatingStatus getOperatingStatus() {
        return operatingStatus;
    }

    public void setOperatingStatus(BloodBankOperatingStatus operatingStatus) {
        this.operatingStatus = operatingStatus;
    }
}
