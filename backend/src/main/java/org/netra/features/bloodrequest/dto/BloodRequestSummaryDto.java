package org.netra.features.bloodrequest.dto;

import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.donor.entity.BloodGroup;

import java.time.Instant;
import java.util.UUID;

public class BloodRequestSummaryDto {

    private UUID id;
    private BloodGroup bloodGroup;
    private Integer unitsRequired;
    private BloodRequestUrgency urgency;
    private BloodRequestStatus status;
    private String hospitalName;
    private String city;
    private String state;
    private Instant requiredBy;
    private Double distanceKm;
    private Instant createdAt;
    private org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus verificationStatus =
            org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus.UNVERIFIED;

    public BloodRequestSummaryDto() {
    }

    public BloodRequestSummaryDto(UUID id, BloodGroup bloodGroup, Integer unitsRequired,
                                 BloodRequestUrgency urgency, BloodRequestStatus status,
                                 String hospitalName, String city, String state,
                                 Instant requiredBy, Double distanceKm, Instant createdAt) {
        this(id, bloodGroup, unitsRequired, urgency, status, hospitalName, city, state, requiredBy, distanceKm, createdAt,
                org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus.UNVERIFIED);
    }

    public BloodRequestSummaryDto(UUID id, BloodGroup bloodGroup, Integer unitsRequired,
                                 BloodRequestUrgency urgency, BloodRequestStatus status,
                                 String hospitalName, String city, String state,
                                 Instant requiredBy, Double distanceKm, Instant createdAt,
                                 org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus verificationStatus) {
        this.id = id;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.urgency = urgency;
        this.status = status;
        this.hospitalName = hospitalName;
        this.city = city;
        this.state = state;
        this.requiredBy = requiredBy;
        this.distanceKm = distanceKm;
        this.createdAt = createdAt;
        this.verificationStatus = verificationStatus != null ? verificationStatus : org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus.UNVERIFIED;
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

    public BloodRequestStatus getStatus() {
        return status;
    }

    public void setStatus(BloodRequestStatus status) {
        this.status = status;
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

    public Instant getRequiredBy() {
        return requiredBy;
    }

    public void setRequiredBy(Instant requiredBy) {
        this.requiredBy = requiredBy;
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

    public org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus != null ? verificationStatus : org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus.UNVERIFIED;
    }
}
