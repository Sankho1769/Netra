package org.netra.features.events.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class RegisterParticipantRequest {

    @NotBlank(message = "Participant full name is required")
    @Size(max = 128, message = "Name must not exceed 128 characters")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Participant phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,14}$", message = "Valid phone number is required")
    private String phone;

    @NotBlank(message = "Participant email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Blood group is required")
    private String bloodGroup;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "Emergency contact name is required")
    @Size(max = 128, message = "Emergency contact name must not exceed 128 characters")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,14}$", message = "Valid emergency contact phone number is required")
    private String emergencyContactPhone;

    @NotNull(message = "Consent confirmation is required")
    private Boolean consentConfirmed;

    public RegisterParticipantRequest() {
    }

    public RegisterParticipantRequest(
            String fullName,
            LocalDate dateOfBirth,
            String phone,
            String email,
            String bloodGroup,
            String address,
            String city,
            String emergencyContactName,
            String emergencyContactPhone,
            Boolean consentConfirmed) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.email = email;
        this.bloodGroup = bloodGroup;
        this.address = address;
        this.city = city;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.consentConfirmed = consentConfirmed;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
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

    public Boolean getConsentConfirmed() {
        return consentConfirmed;
    }

    public void setConsentConfirmed(Boolean consentConfirmed) {
        this.consentConfirmed = consentConfirmed;
    }
}
