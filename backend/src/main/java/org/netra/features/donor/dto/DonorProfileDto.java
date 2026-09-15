package org.netra.features.donor.dto;

import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class DonorProfileDto {

    private UUID id;
    private BloodGroup bloodGroup;
    private BloodGroupVerificationStatus bloodGroupVerificationStatus;
    private DonorAvailabilityStatus availabilityStatus;
    private DonorStatus donorStatus;
    private LocalDate lastDonationDate;
    private Instant createdAt;
    private Instant updatedAt;

    public DonorProfileDto() {
    }

    public DonorProfileDto(
            UUID id,
            BloodGroup bloodGroup,
            BloodGroupVerificationStatus bloodGroupVerificationStatus,
            DonorAvailabilityStatus availabilityStatus,
            DonorStatus donorStatus,
            LocalDate lastDonationDate,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.bloodGroup = bloodGroup;
        this.bloodGroupVerificationStatus = bloodGroupVerificationStatus;
        this.availabilityStatus = availabilityStatus;
        this.donorStatus = donorStatus;
        this.lastDonationDate = lastDonationDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public BloodGroupVerificationStatus getBloodGroupVerificationStatus() {
        return bloodGroupVerificationStatus;
    }

    public void setBloodGroupVerificationStatus(BloodGroupVerificationStatus bloodGroupVerificationStatus) {
        this.bloodGroupVerificationStatus = bloodGroupVerificationStatus;
    }

    public DonorAvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(DonorAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public DonorStatus getDonorStatus() {
        return donorStatus;
    }

    public void setDonorStatus(DonorStatus donorStatus) {
        this.donorStatus = donorStatus;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
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
}
