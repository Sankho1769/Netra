package org.netra.features.events.dto;

import org.netra.features.events.entity.DonationEventRegistrationStatus;

import java.time.Instant;
import java.util.UUID;

public class EventAttendeeDto {

    private UUID registrationId;
    private UUID donorUserId;
    private String donorName;
    private DonationEventRegistrationStatus status;
    private Instant registeredAt;
    private Instant checkedInAt;

    public EventAttendeeDto() {
    }

    public EventAttendeeDto(
            UUID registrationId,
            UUID donorUserId,
            String donorName,
            DonationEventRegistrationStatus status,
            Instant registeredAt,
            Instant checkedInAt) {
        this.registrationId = registrationId;
        this.donorUserId = donorUserId;
        this.donorName = donorName;
        this.status = status;
        this.registeredAt = registeredAt;
        this.checkedInAt = checkedInAt;
    }

    public UUID getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(UUID registrationId) {
        this.registrationId = registrationId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(UUID donorUserId) {
        this.donorUserId = donorUserId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public DonationEventRegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationEventRegistrationStatus status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
}
