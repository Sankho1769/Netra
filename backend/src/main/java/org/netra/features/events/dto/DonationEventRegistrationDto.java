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

    private String participantName;
    private String participantBloodGroup;
    private String participantCity;
    private Boolean consentConfirmed;
    private Instant consentTimestamp;

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
        this(id, eventId, eventTitle, donorUserId, status, registeredAt, cancelledAt, checkedInAt, completedAt,
                null, null, null, null, null);
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
            Instant completedAt,
            String participantName,
            String participantBloodGroup,
            String participantCity,
            Boolean consentConfirmed,
            Instant consentTimestamp) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.donorUserId = donorUserId;
        this.status = status;
        this.registeredAt = registeredAt;
        this.cancelledAt = cancelledAt;
        this.checkedInAt = checkedInAt;
        this.completedAt = completedAt;
        this.participantName = participantName;
        this.participantBloodGroup = participantBloodGroup;
        this.participantCity = participantCity;
        this.consentConfirmed = consentConfirmed;
        this.consentTimestamp = consentTimestamp;
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

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantBloodGroup() {
        return participantBloodGroup;
    }

    public void setParticipantBloodGroup(String participantBloodGroup) {
        this.participantBloodGroup = participantBloodGroup;
    }

    public String getParticipantCity() {
        return participantCity;
    }

    public void setParticipantCity(String participantCity) {
        this.participantCity = participantCity;
    }

    public Boolean getConsentConfirmed() {
        return consentConfirmed;
    }

    public void setConsentConfirmed(Boolean consentConfirmed) {
        this.consentConfirmed = consentConfirmed;
    }

    public Instant getConsentTimestamp() {
        return consentTimestamp;
    }

    public void setConsentTimestamp(Instant consentTimestamp) {
        this.consentTimestamp = consentTimestamp;
    }
}
