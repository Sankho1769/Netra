package org.netra.features.bloodrequest.service;

import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BloodRequestAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(BloodRequestAuthorizationService.class);

    private final UserRepository userRepository;

    public BloodRequestAuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User verifyActiveUser(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedSessionAccessException("Authentication is required.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found: " + userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("User account is not active. Current status: " + user.getStatus());
        }
        return user;
    }

    public void verifyCanManageRequest(UUID currentUserId, BloodRequest request) {
        User user = verifyActiveUser(currentUserId);
        if (request == null) {
            throw new ResourceNotFoundException("Blood request not found.");
        }

        boolean isOwner = request.getRequesterUserId().equals(currentUserId);
        boolean isAdmin = user.getRoles().contains(UserRole.ROLE_ADMIN);

        if (!isOwner && !isAdmin) {
            log.warn("User {} unauthorized to manage blood request {} owned by {}",
                    currentUserId, request.getId(), request.getRequesterUserId());
            throw new UnauthorizedSessionAccessException("You do not have permission to manage this blood request.");
        }
    }

    public boolean isOwner(UUID currentUserId, BloodRequest request) {
        if (currentUserId == null || request == null) {
            return false;
        }
        return request.getRequesterUserId().equals(currentUserId);
    }

    public void verifyCanVerifyRequest(UUID currentUserId, BloodRequest request) {
        User user = verifyActiveUser(currentUserId);
        if (request == null) {
            throw new ResourceNotFoundException("Blood request not found.");
        }

        // Anti-fraud: Requesters cannot verify their own blood requests
        if (request.getRequesterUserId().equals(currentUserId)) {
            log.warn("User {} attempted to self-verify own blood request {}", currentUserId, request.getId());
            throw new ValidationException("Requesters are not permitted to verify their own blood requests.");
        }

        boolean isAuthorized = user.getRoles().contains(UserRole.ROLE_BLOODBANK)
                || user.getRoles().contains(UserRole.ROLE_ADMIN);

        if (!isAuthorized) {
            log.warn("User {} unauthorized to verify blood request {}", currentUserId, request.getId());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only blood bank personnel and administrators are authorized to verify blood requests.");
        }
    }
}
