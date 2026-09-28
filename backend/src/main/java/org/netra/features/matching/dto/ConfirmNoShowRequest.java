package org.netra.features.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfirmNoShowRequest {

    @NotBlank(message = "Confirmation reason is required.")
    @Size(max = 500, message = "Reason cannot exceed 500 characters.")
    private String reason;

    public ConfirmNoShowRequest() {
    }

    public ConfirmNoShowRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
