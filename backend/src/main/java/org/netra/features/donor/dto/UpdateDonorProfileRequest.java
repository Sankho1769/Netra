package org.netra.features.donor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorAvailabilityStatus;

public class UpdateDonorProfileRequest {

    private BloodGroup bloodGroup;
    private DonorAvailabilityStatus availabilityStatus;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0.")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0.")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0.")
    private Double longitude;

    @JsonIgnore
    @AssertTrue(message = "Latitude and longitude must either both be supplied or both be omitted.")
    public boolean isCoordinatesPairValid() {
        return (latitude == null && longitude == null) || (latitude != null && longitude != null);
    }

    public UpdateDonorProfileRequest() {
    }

    public UpdateDonorProfileRequest(BloodGroup bloodGroup, DonorAvailabilityStatus availabilityStatus) {
        this.bloodGroup = bloodGroup;
        this.availabilityStatus = availabilityStatus;
    }

    public UpdateDonorProfileRequest(BloodGroup bloodGroup, DonorAvailabilityStatus availabilityStatus, Double latitude, Double longitude) {
        this.bloodGroup = bloodGroup;
        this.availabilityStatus = availabilityStatus;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
