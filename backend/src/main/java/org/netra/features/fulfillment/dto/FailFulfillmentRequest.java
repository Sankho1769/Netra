package org.netra.features.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FailFulfillmentRequest {

    @NotBlank(message = "failureReason is required.")
    @Size(max = 500, message = "failureReason cannot exceed 500 characters.")
    private String failureReason;

    @Size(max = 1000, message = "notes cannot exceed 1000 characters.")
    private String notes;

    public FailFulfillmentRequest() {
    }

    public FailFulfillmentRequest(String failureReason, String notes) {
        this.failureReason = failureReason;
        this.notes = notes;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
