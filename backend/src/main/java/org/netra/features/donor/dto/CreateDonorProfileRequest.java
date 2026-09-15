package org.netra.features.donor.dto;

import jakarta.validation.constraints.NotNull;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorAvailabilityStatus;

public class CreateDonorProfileRequest {

    @NotNull(message = "Blood group is required.")
    private BloodGroup bloodGroup;

    private DonorAvailabilityStatus availabilityStatus;

    public CreateDonorProfileRequest() {
    }

    public CreateDonorProfileRequest(BloodGroup bloodGroup, DonorAvailabilityStatus availabilityStatus) {
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
