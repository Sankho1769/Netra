package org.netra.features.events.dto;

import org.netra.features.events.entity.DonationEventRegistrationStatus;

import java.time.Instant;
import java.util.UUID;

public class DonationEventRegistrationDto {

    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private UUID donorUserId;
    private DonationEventRegistrationStatus status;
    private Instant registeredAt;
    private Instant cancelledAt;
    private Instant checkedInAt;
    private Instant completedAt;

    public DonationEventRegistrationDto() {
    }

    public DonationEventRegistrationDto(
            UUID id,
            UUID eventId,
            String eventTitle,
            UUID donorUserId,
            DonationEventRegistrationStatus status,
            Instant registeredAt,
            Instant cancelledAt,
            Instant checkedInAt,
            Instant completedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.donorUserId = donorUserId;
        this.status = status;
        this.registeredAt = registeredAt;
        this.cancelledAt = cancelledAt;
        this.checkedInAt = checkedInAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(UUID donorUserId) {
        this.donorUserId = donorUserId;
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

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
