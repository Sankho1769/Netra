package org.netra.features.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "rotated_from")
    private UUID rotatedFrom;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "created_ip_hash", length = 64)
    private String createdIpHash;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt = Instant.now();

    public RefreshSession() {
    }

    public RefreshSession(User user, String tokenHash, UUID familyId, UUID rotatedFrom, Instant expiresAt, String createdIpHash, String userAgent, String deviceId) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.rotatedFrom = rotatedFrom;
        this.expiresAt = expiresAt;
        this.createdIpHash = createdIpHash;
        this.userAgent = userAgent;
        this.deviceId = deviceId;
        this.issuedAt = Instant.now();
        this.lastUsedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public UUID getRotatedFrom() {
        return rotatedFrom;
    }

    public void setRotatedFrom(UUID rotatedFrom) {
        this.rotatedFrom = rotatedFrom;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCreatedIpHash() {
        return createdIpHash;
    }

    public void setCreatedIpHash(String createdIpHash) {
        this.createdIpHash = createdIpHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
