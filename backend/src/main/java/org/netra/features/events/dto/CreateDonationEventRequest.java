package org.netra.features.events.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public class CreateDonationEventRequest {

    @NotNull(message = "Blood bank ID is required")
    private UUID bloodBankId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotBlank(message = "Venue name is required")
    @Size(max = 255, message = "Venue name must not exceed 255 characters")
    private String venueName;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;

    @NotNull(message = "Start time is required")
    private Instant startAt;

    @NotNull(message = "End time is required")
    private Instant endAt;

    @NotNull(message = "Registration open time is required")
    private Instant registrationOpenAt;

    @NotNull(message = "Registration close time is required")
    private Instant registrationCloseAt;

    @NotNull(message = "Donor capacity is required")
    @Min(value = 1, message = "Donor capacity must be at least 1")
    private Integer donorCapacity;

    public CreateDonationEventRequest() {
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
