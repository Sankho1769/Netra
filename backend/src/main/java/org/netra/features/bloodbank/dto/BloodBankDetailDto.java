package org.netra.features.bloodbank.dto;

import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class BloodBankDetailDto {

    private UUID id;
    private String name;
    private String registrationNumber;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private BloodBankVerificationStatus verificationStatus;
    private BloodBankOperatingStatus operatingStatus;
    private Double distanceKm;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BloodInventoryDto> inventory;

    public BloodBankDetailDto() {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BloodInventoryDto> getInventory() {
        return inventory;
    }

    public void setInventory(List<BloodInventoryDto> inventory) {
        this.inventory = inventory;
    }
}
