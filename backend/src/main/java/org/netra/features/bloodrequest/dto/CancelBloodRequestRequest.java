package org.netra.features.bloodrequest.dto;

import jakarta.validation.constraints.Size;

public class CancelBloodRequestRequest {

    @Size(max = 255, message = "Cancellation reason must not exceed 255 characters.")
    private String reason;

    public CancelBloodRequestRequest() {
    }

    public CancelBloodRequestRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
