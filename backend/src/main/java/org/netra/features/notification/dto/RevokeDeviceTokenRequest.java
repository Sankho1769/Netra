package org.netra.features.notification.dto;

import jakarta.validation.constraints.NotBlank;

public class RevokeDeviceTokenRequest {

    @NotBlank(message = "Device token is required.")
    private String token;

    public RevokeDeviceTokenRequest() {
    }

    public RevokeDeviceTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
