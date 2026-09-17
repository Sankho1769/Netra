package org.netra.features.events.dto;

import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.entity.DonationEventType;

import java.time.Instant;
import java.util.UUID;

public class DonationEventDetailDto extends DonationEventSummaryDto {

    private Double latitude;
    private Double longitude;
    private Instant publishedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;

    public DonationEventDetailDto() {
    }

    public DonationEventDetailDto(
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
            Double latitude,
            Double longitude,
            Instant startAt,
            Instant endAt,
            Instant registrationOpenAt,
            Instant registrationCloseAt,
            Integer donorCapacity,
            Integer currentRegistrationCount,
            Double distanceKm,
            Instant publishedAt,
            Instant cancelledAt,
            Instant createdAt,
            Instant updatedAt) {
        super(
                id,
                bloodBankId,
                bloodBankName,
                title,
                description,
                eventType,
                status,
                venueName,
                address,
                city,
                state,
                postalCode,
                startAt,
                endAt,
                registrationOpenAt,
                registrationCloseAt,
                donorCapacity,
                currentRegistrationCount,
                distanceKm
        );
        this.latitude = latitude;
        this.longitude = longitude;
        this.publishedAt = publishedAt;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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
}
