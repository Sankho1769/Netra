package org.netra.features.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyHospitalRequest {

    @NotBlank(message = "Hospital name is required.")
    @Size(max = 255, message = "Hospital name must not exceed 255 characters.")
    private String hospitalName;

    @Size(max = 255, message = "Hospital address must not exceed 255 characters.")
    private String hospitalAddress;

    @Size(max = 100, message = "City must not exceed 100 characters.")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters.")
    private String state;

    @Size(max = 128, message = "Place ID must not exceed 128 characters.")
    private String placeId;

    public VerifyHospitalRequest() {
    }

    public VerifyHospitalRequest(String hospitalName, String hospitalAddress, String city, String state, String placeId) {
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.city = city;
        this.state = state;
        this.placeId = placeId;
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

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
}
