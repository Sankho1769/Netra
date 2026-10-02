package org.netra.features.events.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "donation_event_registrations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_event_donor", columnNames = {"event_id", "donor_user_id"})
    }
)
public class DonationEventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "donor_user_id", nullable = false)
    private UUID donorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DonationEventRegistrationStatus status = DonationEventRegistrationStatus.REGISTERED;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "participant_name", length = 128)
    private String participantName;

    @Column(name = "participant_dob")
    private java.time.LocalDate participantDob;

    @Column(name = "participant_phone", length = 32)
    private String participantPhone;

    @Column(name = "participant_email", length = 255)
    private String participantEmail;

    @Column(name = "participant_blood_group", length = 16)
    private String participantBloodGroup;

    @Column(name = "participant_address", length = 255)
    private String participantAddress;

    @Column(name = "participant_city", length = 100)
    private String participantCity;

    @Column(name = "emergency_contact_name", length = 128)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 32)
    private String emergencyContactPhone;

    @Column(name = "consent_confirmed", nullable = false)
    private boolean consentConfirmed = false;

    @Column(name = "consent_timestamp")
    private Instant consentTimestamp;

    public DonationEventRegistration() {
    }

    public DonationEventRegistration(UUID eventId, UUID donorUserId, DonationEventRegistrationStatus status) {
        this.eventId = eventId;
        this.donorUserId = donorUserId;
        this.status = status != null ? status : DonationEventRegistrationStatus.REGISTERED;
        this.registeredAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public java.time.LocalDate getParticipantDob() {
        return participantDob;
    }

    public void setParticipantDob(java.time.LocalDate participantDob) {
        this.participantDob = participantDob;
    }

    public String getParticipantPhone() {
        return participantPhone;
    }

    public void setParticipantPhone(String participantPhone) {
        this.participantPhone = participantPhone;
    }

    public String getParticipantEmail() {
        return participantEmail;
    }

    public void setParticipantEmail(String participantEmail) {
        this.participantEmail = participantEmail;
    }

    public String getParticipantBloodGroup() {
        return participantBloodGroup;
    }

    public void setParticipantBloodGroup(String participantBloodGroup) {
        this.participantBloodGroup = participantBloodGroup;
    }

    public String getParticipantAddress() {
        return participantAddress;
    }

    public void setParticipantAddress(String participantAddress) {
        this.participantAddress = participantAddress;
    }

    public String getParticipantCity() {
        return participantCity;
    }

    public void setParticipantCity(String participantCity) {
        this.participantCity = participantCity;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public boolean isConsentConfirmed() {
        return consentConfirmed;
    }

    public void setConsentConfirmed(boolean consentConfirmed) {
        this.consentConfirmed = consentConfirmed;
    }

    public Instant getConsentTimestamp() {
        return consentTimestamp;
    }

    public void setConsentTimestamp(Instant consentTimestamp) {
        this.consentTimestamp = consentTimestamp;
    }
}
