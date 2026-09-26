package org.netra.features.bloodrequest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.donor.entity.BloodGroup;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BloodRequestPublicDetailDto {

    private UUID id;
    private BloodGroup bloodGroup;
    private Integer unitsRequired;
    private BloodRequestUrgency urgency;
    private BloodRequestStatus status;
    private String hospitalName;
    private String hospitalAddress;
    private String city;
    private String state;
    private String postalCode;
    private Instant requiredBy;
    private Double distanceKm;
    private Instant createdAt;
    private Boolean isOwner = false;
    private Boolean canManage = false;
    private org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus verificationStatus =
            org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus.UNVERIFIED;

    public BloodRequestPublicDetailDto() {
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

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
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

    public Boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(Boolean isOwner) {
        this.isOwner = isOwner;
    }

    public Boolean getCanManage() {
        return canManage;
    }

    public void setCanManage(Boolean canManage) {
        this.canManage = canManage;
    }

    public org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus != null ? verificationStatus : org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus.UNVERIFIED;
    }
}
