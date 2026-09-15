package org.netra.features.user.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.user.dto.UpdateUserProfileRequest;
import org.netra.features.user.dto.UserProfileDto;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        if (!user.isActive()) {
            throw new AccountStatusException("Account is " + user.getStatus() + ". Please contact NETRA support.");
        }

        return mapToDto(user);
    }

    @Transactional
    public UserProfileDto updateCurrentUserProfile(UpdateUserProfileRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        if (!user.isActive()) {
            throw new AccountStatusException("Account is " + user.getStatus() + ". Please contact NETRA support.");
        }

        // Mass assignment prevention: only fullName and phone are modifiable by normal user profile API
        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) {
            String trimmedPhone = request.getPhone().trim();
            user.setPhone(trimmedPhone.isEmpty() ? null : trimmedPhone);
        }
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        auditService.logAuthEvent(
                "PROFILE_UPDATED",
                savedUser.getId(),
                clientIp,
                userAgent,
                "{\"action\":\"PROFILE_UPDATED\"}"
        );

        return mapToDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        Optional<String> currentUserRole = SecurityUtils.getCurrentUserRole();
        boolean isAdmin = currentUserRole.isPresent() && UserRole.ROLE_ADMIN.name().equalsIgnoreCase(currentUserRole.get());

        // IDOR / BOLA Protection:
        // A user can ONLY access their own profile unless they have ROLE_ADMIN
        if (!currentUserId.equals(targetUserId) && !isAdmin) {
            throw new UnauthorizedSessionAccessException("Access denied. You do not have permission to access another user's profile.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        return mapToDto(user);
    }

    private UserProfileDto mapToDto(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roleNames,
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
