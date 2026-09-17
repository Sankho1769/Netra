package org.netra.features.bloodrequest.dto;

import jakarta.validation.constraints.*;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.donor.entity.BloodGroup;

import java.time.Instant;

public class CreateBloodRequestRequest {

    @NotNull(message = "Blood group is required.")
    private BloodGroup bloodGroup;

    @NotNull(message = "Units required is required.")
    @Min(value = 1, message = "Units required must be at least 1.")
    @Max(value = 50, message = "Units required cannot exceed 50.")
    private Integer unitsRequired;

    private BloodRequestUrgency urgency = BloodRequestUrgency.NORMAL;

    @NotBlank(message = "Hospital name is required.")
    @Size(max = 255, message = "Hospital name must not exceed 255 characters.")
    private String hospitalName;

    @NotBlank(message = "Hospital address is required.")
    @Size(max = 255, message = "Hospital address must not exceed 255 characters.")
    private String hospitalAddress;

    @NotBlank(message = "City is required.")
    @Size(max = 100, message = "City must not exceed 100 characters.")
    private String city;

    @NotBlank(message = "State is required.")
    @Size(max = 100, message = "State must not exceed 100 characters.")
    private String state;

    @NotBlank(message = "Postal code is required.")
    @Size(max = 20, message = "Postal code must not exceed 20 characters.")
    private String postalCode;

    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0.")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0.")
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0.")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0.")
    private Double longitude;

    @NotNull(message = "Required by deadline is required.")
    private Instant requiredBy;

    @Size(max = 1000, message = "Description must not exceed 1000 characters.")
    private String description;

    public CreateBloodRequestRequest() {
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(Integer unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public BloodRequestUrgency getUrgency() {
        return urgency != null ? urgency : BloodRequestUrgency.NORMAL;
    }

    public void setUrgency(BloodRequestUrgency urgency) {
        this.urgency = urgency;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
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

    public Instant getRequiredBy() {
        return requiredBy;
    }

    public void setRequiredBy(Instant requiredBy) {
        this.requiredBy = requiredBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
