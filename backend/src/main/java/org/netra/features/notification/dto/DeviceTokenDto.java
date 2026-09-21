package org.netra.features.notification.dto;

import org.netra.features.notification.entity.DevicePlatform;
import org.netra.features.notification.entity.UserDeviceToken;

import java.time.Instant;
import java.util.UUID;

public class DeviceTokenDto {

    private UUID id;
    private DevicePlatform platform;
    private String provider;
    private boolean active;
    private Instant createdAt;
    private Instant lastSeenAt;

    public DeviceTokenDto() {
    }

    public DeviceTokenDto(UUID id, DevicePlatform platform, String provider, boolean active, Instant createdAt, Instant lastSeenAt) {
        this.id = id;
        this.platform = platform;
        this.provider = provider;
        this.active = active;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
    }

    public static DeviceTokenDto fromEntity(UserDeviceToken entity) {
        if (entity == null) {
            return null;
        }
        return new DeviceTokenDto(
                entity.getId(),
                entity.getPlatform(),
                entity.getProvider(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getLastSeenAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
