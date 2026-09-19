package org.netra.features.matching.dto;

import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.matching.rules.CompatibilityType;

import java.util.UUID;

/**
 * Privacy-preserving DTO representing a matched donor candidate.
 *
 * Candidate Identity Design:
 * - Donor candidate matches computed by Donor Matching are transient decision-support results.
 * - They do NOT represent persistent database records and therefore do NOT have a {@code matchId}.
 * - {@code candidateReference} is currently the DonorProfile UUID: an authorized internal donor-selection reference. Not a secret.
 * - It allows the authorized requester to select a candidate and create a persistent match without leaking the donor's internal
 *   user account identifier ({@code users.id}).
 * - The persistent {@code DonorMatch.id} remains a distinct, server-generated random UUID upon match creation.
 *
 * Privacy Guarantees:
 * - Exact home address, GPS coordinates, personal phone number, and email are strictly omitted.
 * - Donor name is masked to protect anonymity prior to formal consent.
 * - Health history and self-screening answers are never exposed.
 */
public class DonorMatchDto {

    private UUID candidateReference;
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
            UUID candidateReference,
            String donorDisplayName,
            BloodGroup bloodGroup,
            BloodGroupVerificationStatus bloodGroupVerificationStatus,
            DonorAvailabilityStatus availabilityStatus,
            Double distanceKm,
            CompatibilityType compatibilityType,
            MatchQuality matchQuality) {
        this.candidateReference = candidateReference;
        this.donorDisplayName = donorDisplayName;
        this.bloodGroup = bloodGroup;
        this.bloodGroupVerificationStatus = bloodGroupVerificationStatus;
        this.availabilityStatus = availabilityStatus;
        this.distanceKm = distanceKm;
        this.compatibilityType = compatibilityType;
        this.matchQuality = matchQuality;
    }

    public UUID getCandidateReference() {
        return candidateReference;
    }

    public void setCandidateReference(UUID candidateReference) {
        this.candidateReference = candidateReference;
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
