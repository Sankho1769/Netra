package org.netra.features.bloodbank.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class LinkStaffAccountRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    public LinkStaffAccountRequest() {
    }

    public LinkStaffAccountRequest(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
