package org.netra.features.auth.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.*;
import org.netra.core.security.JwtTokenProvider;
import org.netra.core.security.SecurityUtils;
import org.netra.features.auth.dto.*;
import org.netra.features.user.entity.RefreshSession;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.RefreshSessionRepository;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Password policy: At least 8 chars, 1 uppercase, 1 lowercase, 1 digit
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("[0-9]");

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;
    private final long refreshTokenValidityDays;
    private final long accessTokenValiditySeconds;

    public AuthService(
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuditService auditService,
            @Value("${netra.security.jwt.refresh-token-validity-days:7}") long refreshTokenValidityDays,
            @Value("${netra.security.jwt.access-token-validity-seconds:900}") long accessTokenValiditySeconds) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
        this.refreshTokenValidityDays = refreshTokenValidityDays;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp, String userAgent) {
        validatePasswordPolicy(request.getPassword());

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            // Generic failure to prevent account enumeration
            throw new DuplicateEmailException("An account with this email address already exists.");
        }

        String rawPhone = request.getPhone() != null ? request.getPhone().trim().replaceAll("[\\s\\-\\(\\)]", "") : "";
        String normalizedPhone;
        if (rawPhone.startsWith("+91")) {
            normalizedPhone = rawPhone;
        } else if (rawPhone.startsWith("91") && rawPhone.length() == 12) {
            normalizedPhone = "+" + rawPhone;
        } else {
            normalizedPhone = "+91" + rawPhone;
        }

        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new DuplicatePhoneException("An account with this mobile number already exists.");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Clinical Safety & Zero-Trust: Registration role is strictly server-assigned.
        // Client cannot self-grant ROLE_ADMIN or other privileged roles.
        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                normalizedPhone,
                passwordHash,
                Set.of(UserRole.ROLE_DONOR)
        );

        user = userRepository.save(user);

        // Create initial refresh session family
        String rawRefreshToken = SecurityUtils.generateSecureToken();
        String tokenHash = SecurityUtils.sha256Hex(rawRefreshToken);
        UUID familyId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(refreshTokenValidityDays, ChronoUnit.DAYS);

        RefreshSession session = new RefreshSession(
                user,
                tokenHash,
                familyId,
                null,
                expiresAt,
                auditService.hashIp(clientIp),
                userAgent,
                null
        );
        refreshSessionRepository.save(session);

        auditService.logAuthEvent("REGISTER", user.getId(), clientIp, userAgent, "Status=ACTIVE");

        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), roleNames);

        UserSummaryDto summary = new UserSummaryDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roleNames,
                user.getStatus().name()
        );

        return new AuthResponse(accessToken, rawRefreshToken, accessTokenValiditySeconds, summary);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    auditService.logAuthEvent("LOGIN_FAILURE", null, clientIp, userAgent, "UnknownEmail");
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logAuthEvent("LOGIN_FAILURE", user.getId(), clientIp, userAgent, "BadPassword");
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            auditService.logAuthEvent("LOGIN_BLOCKED", user.getId(), clientIp, userAgent, "Status=SUSPENDED");
            throw new AccountStatusException("Your account has been suspended. Please contact support.");
        }

        if (user.getStatus() == UserStatus.DEACTIVATED) {
            auditService.logAuthEvent("LOGIN_BLOCKED", user.getId(), clientIp, userAgent, "Status=DEACTIVATED");
            throw new AccountStatusException("Your account has been deactivated.");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate refresh session with new family
        String rawRefreshToken = SecurityUtils.generateSecureToken();
        String tokenHash = SecurityUtils.sha256Hex(rawRefreshToken);
        UUID familyId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(refreshTokenValidityDays, ChronoUnit.DAYS);

        RefreshSession session = new RefreshSession(
                user,
                tokenHash,
                familyId,
                null,
                expiresAt,
                auditService.hashIp(clientIp),
                userAgent,
                request.getDeviceId()
        );
        refreshSessionRepository.save(session);

        auditService.logAuthEvent("LOGIN_SUCCESS", user.getId(), clientIp, userAgent, "FamilyId=" + familyId);

        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), roleNames);

        UserSummaryDto summary = new UserSummaryDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roleNames,
                user.getStatus().name()
        );

        return new AuthResponse(accessToken, rawRefreshToken, accessTokenValiditySeconds, summary);
    }

    @Transactional(noRollbackFor = {TokenReuseException.class})
    public AuthResponse refresh(TokenRefreshRequest request, String clientIp, String userAgent) {
        String rawRefreshToken = request.getRefreshToken();
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException("Refresh token is required.");
        }

        String tokenHash = SecurityUtils.sha256Hex(rawRefreshToken.trim());
        RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token."));

        // Critical Security Check: Token Reuse Detection
        if (session.isRevoked()) {
            auditService.logAuthEvent(
                    "REFRESH_REUSE_DETECTED",
                    session.getUser().getId(),
                    clientIp,
                    userAgent,
                    "RevokedFamily=" + session.getFamilyId()
            );
            // Immediately revoke the entire token family
            refreshSessionRepository.revokeFamily(session.getFamilyId(), Instant.now());
            throw new TokenReuseException("Revoked refresh token reuse detected. All sessions terminated. Please sign in again.");
        }

        if (session.isExpired()) {
            throw new InvalidCredentialsException("Refresh token has expired. Please sign in again.");
        }

        User user = session.getUser();
        if (!user.isActive()) {
            throw new AccountStatusException("Account is not active.");
        }

        // Atomic CAS revocation to prevent concurrent refresh race conditions
        int updated = refreshSessionRepository.atomicRevokeSession(session.getId(), Instant.now());
        if (updated == 0) {
            auditService.logAuthEvent(
                    "REFRESH_REUSE_DETECTED",
                    session.getUser().getId(),
                    clientIp,
                    userAgent,
                    "ConcurrentReuseFamily=" + session.getFamilyId()
            );
            refreshSessionRepository.revokeFamily(session.getFamilyId(), Instant.now());
            throw new TokenReuseException("Revoked refresh token reuse detected. All sessions terminated. Please sign in again.");
        }

        // Issue new rotated token in the SAME family
        String newRawRefreshToken = SecurityUtils.generateSecureToken();
        String newTokenHash = SecurityUtils.sha256Hex(newRawRefreshToken);
        Instant newExpiresAt = Instant.now().plus(refreshTokenValidityDays, ChronoUnit.DAYS);

        RefreshSession childSession = new RefreshSession(
                user,
                newTokenHash,
                session.getFamilyId(),
                session.getId(),
                newExpiresAt,
                auditService.hashIp(clientIp),
                userAgent,
                session.getDeviceId()
        );
        refreshSessionRepository.save(childSession);

        auditService.logAuthEvent("REFRESH", user.getId(), clientIp, userAgent, "FamilyId=" + session.getFamilyId());

        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), roleNames);

        UserSummaryDto summary = new UserSummaryDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roleNames,
                user.getStatus().name()
        );

        return new AuthResponse(newAccessToken, newRawRefreshToken, accessTokenValiditySeconds, summary);
    }

    @Transactional
    public void logout(LogoutRequest request, UUID currentUserId, String clientIp, String userAgent) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String tokenHash = SecurityUtils.sha256Hex(request.getRefreshToken().trim());
            refreshSessionRepository.findByTokenHash(tokenHash).ifPresent(session -> {
                if (currentUserId == null || session.getUser().getId().equals(currentUserId)) {
                    session.revoke();
                    refreshSessionRepository.save(session);
                } else {
                    log.warn("IDOR attempt: User {} attempted to logout session belonging to user {}",
                            currentUserId, session.getUser().getId());
                }
            });
        }
        auditService.logAuthEvent("LOGOUT", currentUserId, clientIp, userAgent, null);
    }

    @Transactional
    public void logoutAll(UUID currentUserId, String clientIp, String userAgent) {
        refreshSessionRepository.revokeAllForUser(currentUserId, Instant.now());
        auditService.logAuthEvent("LOGOUT_ALL", currentUserId, clientIp, userAgent, null);
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUserSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new UserSummaryDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roleNames,
                user.getStatus().name()
        );
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new PasswordPolicyException("Password must be at least 8 characters long.");
        }
        if (!PASSWORD_UPPERCASE.matcher(password).find()) {
            throw new PasswordPolicyException("Password must contain at least one uppercase letter.");
        }
        if (!PASSWORD_LOWERCASE.matcher(password).find()) {
            throw new PasswordPolicyException("Password must contain at least one lowercase letter.");
        }
        if (!PASSWORD_DIGIT.matcher(password).find()) {
            throw new PasswordPolicyException("Password must contain at least one digit.");
        }
    }
}
