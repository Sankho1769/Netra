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
    private Double latitude;
    private Double longitude;
    private String biologicalSex;
    private UUID verifiedBy;
    private Instant verifiedAt;
    private String verificationNotes;
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
        this(id, bloodGroup, bloodGroupVerificationStatus, availabilityStatus, donorStatus, lastDonationDate, null, null, createdAt, updatedAt);
    }

    public DonorProfileDto(
            UUID id,
            BloodGroup bloodGroup,
            BloodGroupVerificationStatus bloodGroupVerificationStatus,
            DonorAvailabilityStatus availabilityStatus,
            DonorStatus donorStatus,
            LocalDate lastDonationDate,
            Double latitude,
            Double longitude,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.bloodGroup = bloodGroup;
        this.bloodGroupVerificationStatus = bloodGroupVerificationStatus;
        this.availabilityStatus = availabilityStatus;
        this.donorStatus = donorStatus;
        this.lastDonationDate = lastDonationDate;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getBiologicalSex() {
        return biologicalSex;
    }

    public void setBiologicalSex(String biologicalSex) {
        this.biologicalSex = biologicalSex;
    }

    public UUID getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(UUID verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }
}
