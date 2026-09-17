package org.netra.features.events.dto;

import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.entity.DonationEventType;

import java.time.Instant;
import java.util.UUID;

public class DonationEventSummaryDto {

    private UUID id;
    private UUID bloodBankId;
    private String bloodBankName;
    private String title;
    private String description;
    private DonationEventType eventType;
    private DonationEventStatus status;
    private String venueName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private Instant startAt;
    private Instant endAt;
    private Instant registrationOpenAt;
    private Instant registrationCloseAt;
    private Integer donorCapacity;
    private Integer currentRegistrationCount;
    private Integer remainingCapacity;
    private Double distanceKm;
    private Boolean isRegistrationOpen;

    public DonationEventSummaryDto() {
    }

    public DonationEventSummaryDto(
            UUID id,
            UUID bloodBankId,
            String bloodBankName,
            String title,
            String description,
            DonationEventType eventType,
            DonationEventStatus status,
            String venueName,
            String address,
            String city,
            String state,
            String postalCode,
            Instant startAt,
            Instant endAt,
            Instant registrationOpenAt,
            Instant registrationCloseAt,
            Integer donorCapacity,
            Integer currentRegistrationCount,
            Double distanceKm) {
        this.id = id;
        this.bloodBankId = bloodBankId;
        this.bloodBankName = bloodBankName;
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.status = status;
        this.venueName = venueName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.startAt = startAt;
        this.endAt = endAt;
        this.registrationOpenAt = registrationOpenAt;
        this.registrationCloseAt = registrationCloseAt;
        this.donorCapacity = donorCapacity;
        this.currentRegistrationCount = currentRegistrationCount;
        this.remainingCapacity = (donorCapacity != null && currentRegistrationCount != null)
                ? Math.max(0, donorCapacity - currentRegistrationCount)
                : 0;
        this.distanceKm = distanceKm;

        Instant now = Instant.now();
        this.isRegistrationOpen = (status == DonationEventStatus.PUBLISHED)
                && (registrationOpenAt != null && !now.isBefore(registrationOpenAt))
                && (registrationCloseAt != null && !now.isAfter(registrationCloseAt))
                && (donorCapacity != null && currentRegistrationCount != null && currentRegistrationCount < donorCapacity);
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

    public String getBloodBankName() {
        return bloodBankName;
    }

    public void setBloodBankName(String bloodBankName) {
        this.bloodBankName = bloodBankName;
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

    public Integer getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(Integer remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Boolean getIsRegistrationOpen() {
        return isRegistrationOpen;
    }

    public void setIsRegistrationOpen(Boolean isRegistrationOpen) {
        this.isRegistrationOpen = isRegistrationOpen;
    }
}
