package org.netra.features.hospital.dto;

import org.netra.features.hospital.entity.VerifiedHospital;
import java.util.UUID;

public class VerifiedHospitalDto {

    private UUID id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String placeId;
    private Boolean hasBloodBank;
    private String verificationStatus;
    private String phone;
    private Double latitude;
    private Double longitude;
    private String source;
    private String placeType;

    public VerifiedHospitalDto() {
    }

    public VerifiedHospitalDto(
            UUID id,
            String name,
            String address,
            String city,
            String state,
            String postalCode,
            String placeId,
            Boolean hasBloodBank,
            String verificationStatus,
            String phone) {
        this(id, name, address, city, state, postalCode, placeId, hasBloodBank, verificationStatus, phone, null, null, "INTERNAL_REGISTRY", "HOSPITAL");
    }

    public VerifiedHospitalDto(
            UUID id,
            String name,
            String address,
            String city,
            String state,
            String postalCode,
            String placeId,
            Boolean hasBloodBank,
            String verificationStatus,
            String phone,
            Double latitude,
            Double longitude,
            String source,
            String placeType) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.placeId = placeId;
        this.hasBloodBank = hasBloodBank;
        this.verificationStatus = verificationStatus;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.source = source != null ? source : "INTERNAL_REGISTRY";
        this.placeType = placeType != null ? placeType : "HOSPITAL";
    }

    public static VerifiedHospitalDto fromEntity(VerifiedHospital entity) {
        if (entity == null) return null;
        return new VerifiedHospitalDto(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getCity(),
                entity.getState(),
                entity.getPostalCode(),
                entity.getPlaceId(),
                entity.getHasBloodBank(),
                entity.getVerificationStatus(),
                entity.getPhone(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getSource(),
                entity.getPlaceType()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public Boolean getHasBloodBank() {
        return hasBloodBank;
    }

    public void setHasBloodBank(Boolean hasBloodBank) {
        this.hasBloodBank = hasBloodBank;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPlaceType() {
        return placeType;
    }

    public void setPlaceType(String placeType) {
        this.placeType = placeType;
    }
}
