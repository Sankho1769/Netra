package org.netra.features.donor.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.DuplicateResourceException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.donor.dto.CreateDonorProfileRequest;
import org.netra.features.donor.dto.DonorProfileDto;
import org.netra.features.donor.dto.UpdateDonorProfileRequest;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.entity.DonorStatus;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DonorService {

    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DonorService(
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public DonorProfileDto getDonorProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        validateUserActive(currentUserId);

        DonorProfile donorProfile = donorProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for authenticated user."));

        return mapToDto(donorProfile);
    }

    @Transactional
    public DonorProfileDto createDonorProfile(CreateDonorProfileRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        validateUserActive(currentUserId);
        validateCoordinates(request.getLatitude(), request.getLongitude());

        if (donorProfileRepository.existsByUserId(currentUserId)) {
            throw new DuplicateResourceException("Donor profile already exists for this user.");
        }

        // Enforce server-controlled defaults:
        // bloodGroupVerificationStatus is ALWAYS SELF_REPORTED on creation
        // donorStatus is ALWAYS ACTIVE on creation
        // lastDonationDate is ALWAYS null on creation
        DonorProfile profile = new DonorProfile();
        profile.setUserId(currentUserId);
        profile.setBloodGroup(request.getBloodGroup());
        profile.setAvailabilityStatus(request.getAvailabilityStatus() != null
                ? request.getAvailabilityStatus()
                : DonorAvailabilityStatus.AVAILABLE);
        profile.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.SELF_REPORTED);
        profile.setDonorStatus(DonorStatus.ACTIVE);
        profile.setLastDonationDate(null);
        if (request.getLatitude() != null && request.getLongitude() != null) {
            profile.setLatitude(request.getLatitude());
            profile.setLongitude(request.getLongitude());
        }
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        DonorProfile savedProfile = donorProfileRepository.save(profile);

        auditService.logAuthEvent(
                "DONOR_PROFILE_CREATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"action\":\"DONOR_PROFILE_CREATED\"}"
        );

        return mapToDto(savedProfile);
    }

    @Transactional
    public DonorProfileDto updateDonorProfile(UpdateDonorProfileRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        validateUserActive(currentUserId);
        validateCoordinates(request.getLatitude(), request.getLongitude());

        DonorProfile profile = donorProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for authenticated user."));

        boolean modified = false;

        // 1. Blood group change
        if (request.getBloodGroup() != null && !request.getBloodGroup().equals(profile.getBloodGroup())) {
            profile.setBloodGroup(request.getBloodGroup());
            // CRITICAL: Changing blood group resets verification status to SELF_REPORTED
            profile.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.SELF_REPORTED);
            auditService.logAuthEvent(
                    "BLOOD_GROUP_CHANGED",
                    currentUserId,
                    clientIp,
                    userAgent,
                    "{\"action\":\"BLOOD_GROUP_CHANGED\"}"
            );
            modified = true;
        }

        // 2. Availability status change
        if (request.getAvailabilityStatus() != null && !request.getAvailabilityStatus().equals(profile.getAvailabilityStatus())) {
            profile.setAvailabilityStatus(request.getAvailabilityStatus());
            auditService.logAuthEvent(
                    "DONOR_AVAILABILITY_CHANGED",
                    currentUserId,
                    clientIp,
                    userAgent,
                    "{\"action\":\"DONOR_AVAILABILITY_CHANGED\"}"
            );
            modified = true;
        }

        // 3. Location coordinates change
        if (request.getLatitude() != null && request.getLongitude() != null) {
            if (!request.getLatitude().equals(profile.getLatitude())) {
                profile.setLatitude(request.getLatitude());
                modified = true;
            }
            if (!request.getLongitude().equals(profile.getLongitude())) {
                profile.setLongitude(request.getLongitude());
                modified = true;
            }
        }

        if (modified) {
            profile.setUpdatedAt(Instant.now());
            profile = donorProfileRepository.save(profile);
            auditService.logAuthEvent(
                    "DONOR_PROFILE_UPDATED",
                    currentUserId,
                    clientIp,
                    userAgent,
                    "{\"action\":\"DONOR_PROFILE_UPDATED\"}"
            );
        }

        return mapToDto(profile);
    }

    private void validateUserActive(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!user.isActive()) {
            throw new AccountStatusException("Account is " + user.getStatus() + ". Please contact NETRA support.");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null && longitude != null) || (latitude != null && longitude == null)) {
            throw new ValidationException("Latitude and longitude must either both be supplied or both be omitted.");
        }
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw new ValidationException("Latitude must be between -90.0 and 90.0.");
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw new ValidationException("Longitude must be between -180.0 and 180.0.");
        }
    }

    private DonorProfileDto mapToDto(DonorProfile profile) {
        return new DonorProfileDto(
                profile.getId(),
                profile.getBloodGroup(),
                profile.getBloodGroupVerificationStatus(),
                profile.getAvailabilityStatus(),
                profile.getDonorStatus(),
                profile.getLastDonationDate(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
