package org.netra.features.eligibility.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "eligibility_sessions")
public class EligibilitySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "capability_token_hash", length = 64)
    private String capabilityTokenHash;

    @Column(name = "rule_version", nullable = false, length = 32)
    private String ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ResultType result;

    @Column(name = "estimated_eligible_date")
    private LocalDate estimatedEligibleDate;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EligibilityAnswer> answers = new ArrayList<>();

    public EligibilitySession() {
    }

    public EligibilitySession(UUID userId, String ruleVersion, Instant expiresAt) {
        this.userId = userId;
        this.ruleVersion = ruleVersion;
        this.expiresAt = expiresAt;
        this.startedAt = Instant.now();
        this.createdAt = Instant.now();
        this.status = SessionStatus.IN_PROGRESS;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getCapabilityTokenHash() {
        return capabilityTokenHash;
    }

    public void setCapabilityTokenHash(String capabilityTokenHash) {
        this.capabilityTokenHash = capabilityTokenHash;
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

    public ResultType getResult() {
        return result;
    }

    public void setResult(ResultType result) {
        this.result = result;
    }

    public LocalDate getEstimatedEligibleDate() {
        return estimatedEligibleDate;
    }

    public void setEstimatedEligibleDate(LocalDate estimatedEligibleDate) {
        this.estimatedEligibleDate = estimatedEligibleDate;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<EligibilityAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<EligibilityAnswer> answers) {
        this.answers = answers;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
