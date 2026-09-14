package org.netra.features.eligibility.dto;

import org.netra.features.eligibility.entity.SessionStatus;
import java.time.Instant;
import java.util.UUID;

public class SessionResponse {
    private UUID sessionId;
    private String ruleVersion;
    private SessionStatus status;
    private Instant expiresAt;
    private String capabilityToken;

    public SessionResponse() {
    }

    public SessionResponse(UUID sessionId, String ruleVersion, SessionStatus status, Instant expiresAt) {
        this(sessionId, ruleVersion, status, expiresAt, null);
    }

    public SessionResponse(UUID sessionId, String ruleVersion, SessionStatus status, Instant expiresAt, String capabilityToken) {
        this.sessionId = sessionId;
        this.ruleVersion = ruleVersion;
        this.status = status;
        this.expiresAt = expiresAt;
        this.capabilityToken = capabilityToken;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCapabilityToken() {
        return capabilityToken;
    }

    public void setCapabilityToken(String capabilityToken) {
        this.capabilityToken = capabilityToken;
    }
}
