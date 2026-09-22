package org.netra.features.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.netra.features.notification.entity.DevicePlatform;

public class RegisterDeviceTokenRequest {

    @NotBlank(message = "Device token must not be blank")
    @Size(max = 512, message = "Device token must not exceed 512 characters")
    private String token;

    private DevicePlatform platform = DevicePlatform.ANDROID;

    @Pattern(regexp = "^(?i)FCM$", message = "Only FCM provider is supported")
    @Size(max = 32, message = "Provider must not exceed 32 characters")
    private String provider = "FCM";

    public RegisterDeviceTokenRequest() {
    }

    public RegisterDeviceTokenRequest(String token, DevicePlatform platform, String provider) {
        this.token = token;
        this.platform = platform != null ? platform : DevicePlatform.ANDROID;
        this.provider = provider != null ? provider : "FCM";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(DevicePlatform platform) {
        this.platform = platform;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
