package org.netra.features.donor.dto;

import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorAvailabilityStatus;

public class UpdateDonorProfileRequest {

    private BloodGroup bloodGroup;
    private DonorAvailabilityStatus availabilityStatus;

    public UpdateDonorProfileRequest() {
    }

    public UpdateDonorProfileRequest(BloodGroup bloodGroup, DonorAvailabilityStatus availabilityStatus) {
        this.bloodGroup = bloodGroup;
        this.availabilityStatus = availabilityStatus;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public DonorAvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(DonorAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
