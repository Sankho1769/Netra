package org.netra.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserProfileRequest {

    @NotBlank(message = "Full name is required.")
    @Size(min = 2, max = 128, message = "Full name must be between 2 and 128 characters.")
    private String fullName;

    @Pattern(regexp = "^$|^\\+?[0-9\\s\\-]{7,16}$", message = "Phone number must be a valid format (7-16 digits/characters).")
    private String phone;

    public UpdateUserProfileRequest() {
    }

    public UpdateUserProfileRequest(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
