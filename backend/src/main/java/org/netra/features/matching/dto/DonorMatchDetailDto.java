package org.netra.features.matching.dto;

import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.matching.entity.MatchStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Privacy-preserving DTO for a donor viewing their assigned match.
 *
 * Exposes only operational details required for a decision:
 * blood group required, units, urgency, hospital, city, state, distance, deadlines, and disclaimer.
 * Strictly omits requester phone, email, private coordinates, and patient identifiers.
 */
public class DonorMatchDetailDto {

    public static final String REQUIRED_DISCLAIMER =
            "Accepting a match does not confirm medical eligibility or donation. Final screening is performed by qualified blood-bank staff.";

    private UUID matchId;
    private UUID bloodRequestId;
    private BloodGroup bloodGroupRequired;
    private Integer unitsRequired;
    private BloodRequestUrgency urgency;
    private String hospitalName;
    private String city;
    private String state;
    private Double distanceKm;
    private Instant requiredBy;
    private Instant expiresAt;
    private MatchStatus responseStatus;
    private Instant createdAt;
    private Instant respondedAt;
    private String disclaimer = REQUIRED_DISCLAIMER;

    public DonorMatchDetailDto() {
    }

    public DonorMatchDetailDto(
            UUID matchId,
            UUID bloodRequestId,
            BloodGroup bloodGroupRequired,
            Integer unitsRequired,
            BloodRequestUrgency urgency,
            String hospitalName,
            String city,
            String state,
            Double distanceKm,
            Instant requiredBy,
            Instant expiresAt,
            MatchStatus responseStatus,
            Instant createdAt,
            Instant respondedAt) {
        this.matchId = matchId;
        this.bloodRequestId = bloodRequestId;
        this.bloodGroupRequired = bloodGroupRequired;
        this.unitsRequired = unitsRequired;
        this.urgency = urgency;
        this.hospitalName = hospitalName;
        this.city = city;
        this.state = state;
        this.distanceKm = distanceKm;
        this.requiredBy = requiredBy;
        this.expiresAt = expiresAt;
        this.responseStatus = responseStatus;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
        this.disclaimer = REQUIRED_DISCLAIMER;
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

    public BloodGroup getBloodGroupRequired() {
        return bloodGroupRequired;
    }

    public void setBloodGroupRequired(BloodGroup bloodGroupRequired) {
        this.bloodGroupRequired = bloodGroupRequired;
    }

    public Integer getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(Integer unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public BloodRequestUrgency getUrgency() {
        return urgency;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Instant getRequiredBy() {
        return requiredBy;
    }

    public void setRequiredBy(Instant requiredBy) {
        this.requiredBy = requiredBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
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

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
