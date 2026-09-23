package org.netra.features.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelFulfillmentRequest {

    @NotBlank(message = "cancellationReason is required.")
    @Size(max = 500, message = "cancellationReason cannot exceed 500 characters.")
    private String cancellationReason;

    @Size(max = 1000, message = "notes cannot exceed 1000 characters.")
    private String notes;

    public CancelFulfillmentRequest() {
    }

    public CancelFulfillmentRequest(String cancellationReason, String notes) {
        this.cancellationReason = cancellationReason;
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
