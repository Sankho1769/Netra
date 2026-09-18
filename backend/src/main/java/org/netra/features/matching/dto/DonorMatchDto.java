package org.netra.features.matching.dto;

import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.matching.rules.CompatibilityType;

import java.util.UUID;

/**
 * Privacy-preserving DTO representing a matched donor candidate.
 *
 * Privacy Guarantees:
 * - Exact home address, GPS coordinates, personal phone number, and email are strictly omitted.
 * - Donor name is masked to protect anonymity prior to formal consent.
 * - Health history and self-screening answers are never exposed.
 */
public class DonorMatchDto {

    private UUID matchId;
    private String donorDisplayName;
    private BloodGroup bloodGroup;
    private BloodGroupVerificationStatus bloodGroupVerificationStatus;
    private DonorAvailabilityStatus availabilityStatus;
    private Double distanceKm;
    private CompatibilityType compatibilityType;
    private MatchQuality matchQuality;

    public DonorMatchDto() {
    }

    public DonorMatchDto(
            UUID matchId,
            String donorDisplayName,
            BloodGroup bloodGroup,
            BloodGroupVerificationStatus bloodGroupVerificationStatus,
            DonorAvailabilityStatus availabilityStatus,
            Double distanceKm,
            CompatibilityType compatibilityType,
            MatchQuality matchQuality) {
        this.matchId = matchId;
        this.donorDisplayName = donorDisplayName;
        this.bloodGroup = bloodGroup;
        this.bloodGroupVerificationStatus = bloodGroupVerificationStatus;
        this.availabilityStatus = availabilityStatus;
        this.distanceKm = distanceKm;
        this.compatibilityType = compatibilityType;
        this.matchQuality = matchQuality;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
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

    public CompatibilityType getCompatibilityType() {
        return compatibilityType;
    }

    public void setCompatibilityType(CompatibilityType compatibilityType) {
        this.compatibilityType = compatibilityType;
    }

    public MatchQuality getMatchQuality() {
        return matchQuality;
    }

    public void setMatchQuality(MatchQuality matchQuality) {
        this.matchQuality = matchQuality;
    }
}
