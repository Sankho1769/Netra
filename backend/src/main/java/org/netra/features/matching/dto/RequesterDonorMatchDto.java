package org.netra.features.matching.dto;

import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.matching.entity.MatchStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Privacy-preserving DTO for a requester or admin viewing persistent donor matches.
 *
 * Safe fields exposed:
 * match ID, masked donor display name, blood group, verification status, availability,
 * distance in km, match response status, and relevant timestamps.
 *
 * Strictly Omitted:
 * - Donor phone number
 * - Donor email address
 * - Exact home GPS coordinates
 * - Home street address
 * - Health / medical screening history
 * - Self-screening questionnaire answers
 * - Security audit metadata
 */
public class RequesterDonorMatchDto {

    private UUID matchId;
    private UUID bloodRequestId;
    private String donorDisplayName;
    private BloodGroup bloodGroup;
    private BloodGroupVerificationStatus bloodGroupVerificationStatus;
    private DonorAvailabilityStatus availabilityStatus;
    private Double distanceKm;
    private MatchStatus responseStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant respondedAt;
    private Instant expiresAt;

    public RequesterDonorMatchDto() {
    }

    public RequesterDonorMatchDto(
            UUID matchId,
            UUID bloodRequestId,
            String donorDisplayName,
            BloodGroup bloodGroup,
            BloodGroupVerificationStatus bloodGroupVerificationStatus,
            DonorAvailabilityStatus availabilityStatus,
            Double distanceKm,
            MatchStatus responseStatus,
            Instant createdAt,
            Instant updatedAt,
            Instant respondedAt,
            Instant expiresAt) {
        this.matchId = matchId;
        this.bloodRequestId = bloodRequestId;
        this.donorDisplayName = donorDisplayName;
        this.bloodGroup = bloodGroup;
        this.bloodGroupVerificationStatus = bloodGroupVerificationStatus;
        this.availabilityStatus = availabilityStatus;
        this.distanceKm = distanceKm;
        this.responseStatus = responseStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.respondedAt = respondedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public void setBloodRequestId(UUID bloodRequestId) {
        this.bloodRequestId = bloodRequestId;
    }

    public String getDonorDisplayName() {
        return donorDisplayName;
    }

    public void setDonorDisplayName(String donorDisplayName) {
        this.donorDisplayName = donorDisplayName;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public MatchStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(MatchStatus responseStatus) {
        this.responseStatus = responseStatus;
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

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
