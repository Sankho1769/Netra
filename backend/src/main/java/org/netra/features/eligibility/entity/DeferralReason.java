package org.netra.features.eligibility.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deferral_reasons")
public class DeferralReason {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private EligibilityRule rule;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "display_text", nullable = false, columnDefinition = "TEXT")
    private String displayText;

    @Column(name = "recommended_action", nullable = false, columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public DeferralReason() {
    }

    public DeferralReason(EligibilityRule rule, String reasonCode, String displayText, String recommendedAction) {
        this.rule = rule;
        this.reasonCode = reasonCode;
        this.displayText = displayText;
        this.recommendedAction = recommendedAction;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EligibilityRule getRule() {
        return rule;
    }

    public void setRule(EligibilityRule rule) {
        this.rule = rule;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
