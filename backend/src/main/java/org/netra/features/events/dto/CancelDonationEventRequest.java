package org.netra.features.events.dto;

public class CancelDonationEventRequest {

    private String reason;

    public CancelDonationEventRequest() {
    }

    public CancelDonationEventRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
