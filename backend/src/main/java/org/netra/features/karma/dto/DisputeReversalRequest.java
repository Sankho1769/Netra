package org.netra.features.karma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DisputeReversalRequest {

    @NotBlank(message = "Reversal reason is required.")
    @Size(max = 500, message = "Reason cannot exceed 500 characters.")
    private String reason;

    public DisputeReversalRequest() {
    }

    public DisputeReversalRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
