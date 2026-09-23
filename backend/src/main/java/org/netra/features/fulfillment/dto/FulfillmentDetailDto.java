package org.netra.features.fulfillment.dto;

import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class FulfillmentDetailDto {

    private UUID id;
    private UUID bloodRequestId;
    private UUID donationId;
    private Integer units;
    private FulfillmentStatus status;
    private UUID createdByUserId;
    private UUID startedByUserId;
    private UUID completedByUserId;
    private UUID failedByUserId;
    private UUID cancelledByUserId;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant cancelledAt;
    private String failureReason;
    private String cancellationReason;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // Blood Request details
    private String hospitalName;
    private String hospitalAddress;
    private String city;
    private String state;
    private BloodGroup requestedBloodGroup;
    private Integer unitsRequired;
    private Integer unitsFulfilled;
    private Integer remainingUnits;

    // Donation details
    private UUID donorUserId;
    private BloodGroup donorBloodGroup;
    private LocalDate donationDate;

    // Ownership / permissions
    private boolean isRequester;
    private boolean isDonor;
    private boolean canManage;

    public FulfillmentDetailDto() {
    }

    public static FulfillmentDetailDto from(
            Fulfillment fulfillment,
            BloodRequest bloodRequest,
            Donation donation,
            BloodGroup donorBloodGroup,
            boolean isRequester,
            boolean isDonor,
            boolean canManage) {
        FulfillmentDetailDto dto = new FulfillmentDetailDto();
        dto.setId(fulfillment.getId());
        dto.setBloodRequestId(fulfillment.getBloodRequestId());
        dto.setDonationId(fulfillment.getDonationId());
        dto.setUnits(fulfillment.getUnits());
        dto.setStatus(fulfillment.getStatus());
        dto.setCreatedByUserId(fulfillment.getCreatedByUserId());
        dto.setStartedByUserId(fulfillment.getStartedByUserId());
        dto.setCompletedByUserId(fulfillment.getCompletedByUserId());
        dto.setFailedByUserId(fulfillment.getFailedByUserId());
        dto.setCancelledByUserId(fulfillment.getCancelledByUserId());
        dto.setStartedAt(fulfillment.getStartedAt());
        dto.setCompletedAt(fulfillment.getCompletedAt());
        dto.setFailedAt(fulfillment.getFailedAt());
        dto.setCancelledAt(fulfillment.getCancelledAt());
        dto.setFailureReason(fulfillment.getFailureReason());
        dto.setCancellationReason(fulfillment.getCancellationReason());
        dto.setNotes(fulfillment.getNotes());
        dto.setCreatedAt(fulfillment.getCreatedAt());
        dto.setUpdatedAt(fulfillment.getUpdatedAt());

        if (bloodRequest != null) {
            dto.setHospitalName(bloodRequest.getHospitalName());
            dto.setHospitalAddress(bloodRequest.getHospitalAddress());
            dto.setCity(bloodRequest.getCity());
            dto.setState(bloodRequest.getState());
            dto.setRequestedBloodGroup(bloodRequest.getBloodGroup());
            dto.setUnitsRequired(bloodRequest.getUnitsRequired());
            dto.setUnitsFulfilled(bloodRequest.getUnitsFulfilled());
            dto.setRemainingUnits(bloodRequest.getRemainingUnits());
        }

        if (donation != null) {
            dto.setDonorUserId(donation.getDonorUserId());
            dto.setDonationDate(donation.getDonationDate());
        }
        dto.setDonorBloodGroup(donorBloodGroup);

        dto.setRequester(isRequester);
        dto.setDonor(isDonor);
        dto.setCanManage(canManage);

        return dto;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public void setBloodRequestId(UUID bloodRequestId) {
        this.bloodRequestId = bloodRequestId;
    }

    public UUID getDonationId() {
        return donationId;
    }

    public void setDonationId(UUID donationId) {
        this.donationId = donationId;
    }

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public void setStatus(FulfillmentStatus status) {
        this.status = status;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public UUID getStartedByUserId() {
        return startedByUserId;
    }

    public void setStartedByUserId(UUID startedByUserId) {
        this.startedByUserId = startedByUserId;
    }

    public UUID getCompletedByUserId() {
        return completedByUserId;
    }

    public void setCompletedByUserId(UUID completedByUserId) {
        this.completedByUserId = completedByUserId;
    }

    public UUID getFailedByUserId() {
        return failedByUserId;
    }

    public void setFailedByUserId(UUID failedByUserId) {
        this.failedByUserId = failedByUserId;
    }

    public UUID getCancelledByUserId() {
        return cancelledByUserId;
    }

    public void setCancelledByUserId(UUID cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public BloodGroup getRequestedBloodGroup() {
        return requestedBloodGroup;
    }

    public void setRequestedBloodGroup(BloodGroup requestedBloodGroup) {
        this.requestedBloodGroup = requestedBloodGroup;
    }

    public Integer getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(Integer unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public Integer getUnitsFulfilled() {
        return unitsFulfilled;
    }

    public void setUnitsFulfilled(Integer unitsFulfilled) {
        this.unitsFulfilled = unitsFulfilled;
    }

    public Integer getRemainingUnits() {
        return remainingUnits;
    }

    public void setRemainingUnits(Integer remainingUnits) {
        this.remainingUnits = remainingUnits;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(UUID donorUserId) {
        this.donorUserId = donorUserId;
    }

    public BloodGroup getDonorBloodGroup() {
        return donorBloodGroup;
    }

    public void setDonorBloodGroup(BloodGroup donorBloodGroup) {
        this.donorBloodGroup = donorBloodGroup;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public boolean isRequester() {
        return isRequester;
    }

    public void setRequester(boolean requester) {
        isRequester = requester;
    }

    public boolean isDonor() {
        return isDonor;
    }

    public void setDonor(boolean donor) {
        isDonor = donor;
    }

    public boolean isCanManage() {
        return canManage;
    }

    public void setCanManage(boolean canManage) {
        this.canManage = canManage;
    }
}
