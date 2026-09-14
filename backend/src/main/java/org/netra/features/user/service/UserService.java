package org.netra.features.user.service;

import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.user.dto.UserProfileDto;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());

        return new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roleNames,
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
