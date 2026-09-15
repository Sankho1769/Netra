package org.netra.features.user.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class UserProfileDto {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private Set<String> roles;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public UserProfileDto() {
    }

    public UserProfileDto(UUID id, String fullName, String email, String phone, Set<String> roles, String status, Instant createdAt) {
        this(id, fullName, email, phone, roles, status, createdAt, createdAt);
    }

    public UserProfileDto(UUID id, String fullName, String email, String phone, Set<String> roles, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.roles = roles;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
