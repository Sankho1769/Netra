package org.netra.features.events.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donation_events")
public class DonationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blood_bank_id", nullable = false)
    private UUID bloodBankId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private DonationEventType eventType = DonationEventType.BLOOD_DONATION_CAMP;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DonationEventStatus status = DonationEventStatus.DRAFT;

    @Column(name = "venue_name", nullable = false, length = 255)
    private String venueName;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "registration_open_at", nullable = false)
    private Instant registrationOpenAt;

    @Column(name = "registration_close_at", nullable = false)
    private Instant registrationCloseAt;

    @Column(name = "donor_capacity", nullable = false)
    private Integer donorCapacity;

    @Column(name = "current_registration_count", nullable = false)
    private Integer currentRegistrationCount = 0;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public DonationEvent() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(UUID bloodBankId) {
        this.bloodBankId = bloodBankId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DonationEventType getEventType() {
        return eventType;
    }

    public void setEventType(DonationEventType eventType) {
        this.eventType = eventType;
    }

    public DonationEventStatus getStatus() {
        return status;
    }

    public void setStatus(DonationEventStatus status) {
        this.status = status;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public Instant getRegistrationOpenAt() {
        return registrationOpenAt;
    }

    public void setRegistrationOpenAt(Instant registrationOpenAt) {
        this.registrationOpenAt = registrationOpenAt;
    }

    public Instant getRegistrationCloseAt() {
        return registrationCloseAt;
    }

    public void setRegistrationCloseAt(Instant registrationCloseAt) {
        this.registrationCloseAt = registrationCloseAt;
    }

    public Integer getDonorCapacity() {
        return donorCapacity;
    }

    public void setDonorCapacity(Integer donorCapacity) {
        this.donorCapacity = donorCapacity;
    }

    public Integer getCurrentRegistrationCount() {
        return currentRegistrationCount;
    }

    public void setCurrentRegistrationCount(Integer currentRegistrationCount) {
        this.currentRegistrationCount = currentRegistrationCount;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(UUID cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
