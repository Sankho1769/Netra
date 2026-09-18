package org.netra.features.matching.dto;

import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.donor.entity.BloodGroup;

import java.util.List;
import java.util.UUID;

/**
 * Enveloping response returned by the donor matching engine.
 * Includes conservative medical disclaimer.
 */
public class DonorMatchResponse {

    public static final String DEFAULT_DISCLAIMER =
            "Donor matching results represent potential, preliminary candidates based on registered availability " +
            "and standard ABO/Rh blood compatibility rules. A match does NOT constitute medical clearance or " +
            "guaranteed donation eligibility. Final medical evaluation and donor suitability clearance must be " +
            "performed by authorized clinical staff and qualified blood bank personnel prior to collection.";

    private UUID requestId;
    private BloodGroup bloodGroupRequired;
    private BloodRequestUrgency urgency;
    private Double searchRadiusKm;
    private int candidateCount;
    private List<DonorMatchDto> matches;
    private String disclaimer = DEFAULT_DISCLAIMER;

    public DonorMatchResponse() {
    }

    public DonorMatchResponse(
            UUID requestId,
            BloodGroup bloodGroupRequired,
            BloodRequestUrgency urgency,
            Double searchRadiusKm,
            int candidateCount,
            List<DonorMatchDto> matches) {
        this.requestId = requestId;
        this.bloodGroupRequired = bloodGroupRequired;
        this.urgency = urgency;
        this.searchRadiusKm = searchRadiusKm;
        this.candidateCount = candidateCount;
        this.matches = matches;
        this.disclaimer = DEFAULT_DISCLAIMER;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public BloodGroup getBloodGroupRequired() {
        return bloodGroupRequired;
    }

    public void setBloodGroupRequired(BloodGroup bloodGroupRequired) {
        this.bloodGroupRequired = bloodGroupRequired;
    }

    public BloodRequestUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(BloodRequestUrgency urgency) {
        this.urgency = urgency;
    }

    public Double getSearchRadiusKm() {
        return searchRadiusKm;
    }

    public void setSearchRadiusKm(Double searchRadiusKm) {
        this.searchRadiusKm = searchRadiusKm;
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    public List<DonorMatchDto> getMatches() {
        return matches;
    }

    public void setMatches(List<DonorMatchDto> matches) {
        this.matches = matches;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
