package org.netra.features.hospital.dto;

import java.util.UUID;

public class HospitalVerificationResultDto {

    private String verificationStatus; // VERIFIED, UNVERIFIED, REJECTED
    private UUID verifiedHospitalId;
    private String hospitalName;
    private String hospitalAddress;
    private String city;
    private String state;
    private String postalCode;
    private String placeId;
    private Boolean hasBloodBank;
    private Double latitude; // Internal/Service resolution
    private Double longitude; // Internal/Service resolution
    private String notes;

    public HospitalVerificationResultDto() {
    }

    public static HospitalVerificationResultDto verified(
            UUID verifiedHospitalId,
            String hospitalName,
            String hospitalAddress,
            String city,
            String state,
            String postalCode,
            String placeId,
            Boolean hasBloodBank,
            Double latitude,
            Double longitude,
            String notes) {
        HospitalVerificationResultDto dto = new HospitalVerificationResultDto();
        dto.setVerificationStatus("VERIFIED");
        dto.setVerifiedHospitalId(verifiedHospitalId);
        dto.setHospitalName(hospitalName);
        dto.setHospitalAddress(hospitalAddress);
        dto.setCity(city);
        dto.setState(state);
        dto.setPostalCode(postalCode);
        dto.setPlaceId(placeId);
        dto.setHasBloodBank(hasBloodBank);
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        dto.setNotes(notes != null ? notes : "Verified against authoritative healthcare registry.");
        return dto;
    }

    public static HospitalVerificationResultDto unverified(String hospitalName, String hospitalAddress, String city, String state, String notes) {
        HospitalVerificationResultDto dto = new HospitalVerificationResultDto();
        dto.setVerificationStatus("UNVERIFIED");
        dto.setHospitalName(hospitalName);
        dto.setHospitalAddress(hospitalAddress);
        dto.setCity(city);
        dto.setState(state);
        dto.setHasBloodBank(false);
        dto.setNotes(notes != null ? notes : "Hospital could not be verified in authoritative registry. Requires manual clinical verification.");
        return dto;
    }

    public static HospitalVerificationResultDto rejected(String hospitalName, String reason) {
        HospitalVerificationResultDto dto = new HospitalVerificationResultDto();
        dto.setVerificationStatus("REJECTED");
        dto.setHospitalName(hospitalName);
        dto.setNotes(reason != null ? reason : "Place rejected: known fraudulent or invalid clinical institution.");
        return dto;
    }

    public boolean isVerified() {
        return "VERIFIED".equalsIgnoreCase(verificationStatus);
    }

    public String getMessage() {
        return notes;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public UUID getVerifiedHospitalId() {
        return verifiedHospitalId;
    }

    public void setVerifiedHospitalId(UUID verifiedHospitalId) {
        this.verifiedHospitalId = verifiedHospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
