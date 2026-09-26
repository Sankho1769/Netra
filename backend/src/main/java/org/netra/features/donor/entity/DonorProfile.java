package org.netra.features.donor.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "donor_profiles")
public class DonorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Convert(converter = BloodGroupConverter.class)
    @Column(name = "blood_group", nullable = false, length = 8)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group_verification_status", nullable = false, length = 32)
    private BloodGroupVerificationStatus bloodGroupVerificationStatus = BloodGroupVerificationStatus.SELF_REPORTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 32)
    private DonorAvailabilityStatus availabilityStatus = DonorAvailabilityStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "donor_status", nullable = false, length = 32)
    private DonorStatus donorStatus = DonorStatus.ACTIVE;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "biological_sex", length = 16)
    private String biologicalSex;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_notes", length = 500)
    private String verificationNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DonorProfile() {
    }

    public DonorProfile(UUID userId, BloodGroup bloodGroup, DonorAvailabilityStatus availabilityStatus) {
        this.userId = userId;
        this.bloodGroup = bloodGroup;
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus : DonorAvailabilityStatus.AVAILABLE;
        this.bloodGroupVerificationStatus = BloodGroupVerificationStatus.SELF_REPORTED;
        this.donorStatus = DonorStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getBiologicalSex() {
        return biologicalSex;
    }

    public void setBiologicalSex(String biologicalSex) {
        this.biologicalSex = biologicalSex;
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
