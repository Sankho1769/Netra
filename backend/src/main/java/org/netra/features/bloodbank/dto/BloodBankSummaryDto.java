package org.netra.features.bloodbank.dto;

import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;

import java.util.UUID;

public class BloodBankSummaryDto {

    private UUID id;
    private String name;
    private String registrationNumber;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String phone;
    private String email;
    private BloodBankVerificationStatus verificationStatus;
    private BloodBankOperatingStatus operatingStatus;
    private Double distanceKm;
    private Double latitude;
    private Double longitude;

    public BloodBankSummaryDto() {
    }

    public BloodBankSummaryDto(
            UUID id,
            String name,
            String registrationNumber,
            String address,
            String city,
            String state,
            String postalCode,
            String phone,
            String email,
            BloodBankVerificationStatus verificationStatus,
            BloodBankOperatingStatus operatingStatus,
            Double distanceKm) {
        this(id, name, registrationNumber, address, city, state, postalCode, phone, email,
                verificationStatus, operatingStatus, distanceKm, null, null);
    }

    public BloodBankSummaryDto(
            UUID id,
            String name,
            String registrationNumber,
            String address,
            String city,
            String state,
            String postalCode,
            String phone,
            String email,
            BloodBankVerificationStatus verificationStatus,
            BloodBankOperatingStatus operatingStatus,
            Double distanceKm,
            Double latitude,
            Double longitude) {
        this.id = id;
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.address = address;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.phone = phone;
        this.email = email;
        this.verificationStatus = verificationStatus;
        this.operatingStatus = operatingStatus;
        this.distanceKm = distanceKm;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
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
