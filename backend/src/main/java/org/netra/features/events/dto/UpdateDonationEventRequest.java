package org.netra.features.events.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public class UpdateDonationEventRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @Size(max = 255, message = "Venue name must not exceed 255 characters")
    private String venueName;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;

    private Instant startAt;
    private Instant endAt;
    private Instant registrationOpenAt;
    private Instant registrationCloseAt;

    @Min(value = 1, message = "Donor capacity must be at least 1")
    private Integer donorCapacity;

    public UpdateDonationEventRequest() {
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
}
