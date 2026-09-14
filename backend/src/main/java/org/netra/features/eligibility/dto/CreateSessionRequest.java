package org.netra.features.eligibility.dto;

import java.time.Instant;

public class CreateSessionRequest {
    private Instant clientTimestamp;

    public CreateSessionRequest() {
    }

    public CreateSessionRequest(Instant clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }

    public Instant getClientTimestamp() {
        return clientTimestamp;
    }

    public void setClientTimestamp(Instant clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }
}
