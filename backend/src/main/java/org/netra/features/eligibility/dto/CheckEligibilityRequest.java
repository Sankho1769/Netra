package org.netra.features.eligibility.dto;

public class CheckEligibilityRequest {
    private boolean clientReviewConfirmed = true;

    public CheckEligibilityRequest() {
    }

    public CheckEligibilityRequest(boolean clientReviewConfirmed) {
        this.clientReviewConfirmed = clientReviewConfirmed;
    }

    public boolean isClientReviewConfirmed() {
        return clientReviewConfirmed;
    }

    public void setClientReviewConfirmed(boolean clientReviewConfirmed) {
        this.clientReviewConfirmed = clientReviewConfirmed;
    }
}
