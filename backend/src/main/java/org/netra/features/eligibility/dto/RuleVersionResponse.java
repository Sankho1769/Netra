package org.netra.features.eligibility.dto;

import java.time.Instant;

public class RuleVersionResponse {
    private String activeVersion;
    private Instant effectiveDate;
    private String policySource;
    private String authority;

    public RuleVersionResponse() {
    }

    public RuleVersionResponse(String activeVersion, Instant effectiveDate, String policySource, String authority) {
        this.activeVersion = activeVersion;
        this.effectiveDate = effectiveDate;
        this.policySource = policySource;
        this.authority = authority;
    }

    public String getActiveVersion() {
        return activeVersion;
    }

    public void setActiveVersion(String activeVersion) {
        this.activeVersion = activeVersion;
    }

    public Instant getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Instant effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getPolicySource() {
        return policySource;
    }

    public void setPolicySource(String policySource) {
        this.policySource = policySource;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}
