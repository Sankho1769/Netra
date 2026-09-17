package org.netra.features.events.dto;

import jakarta.validation.constraints.NotNull;
import org.netra.features.events.entity.DonationEventStatus;

public class ApproveDonationEventRequest {

    @NotNull(message = "Decision status is required (PUBLISHED or REJECTED)")
    private DonationEventStatus status;

    private String rejectionReason;

    public ApproveDonationEventRequest() {
    }

    public ApproveDonationEventRequest(DonationEventStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public DonationEventStatus getStatus() {
        return status;
    }

    public void setStatus(DonationEventStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
