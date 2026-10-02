package org.netra.features.auth.service;

import org.netra.core.audit.AuditService;
import org.netra.core.ratelimit.RateLimitExceededException;
import org.netra.core.exception.VerificationExpiredException;
import org.netra.core.exception.VerificationFailedException;
import org.netra.core.exception.VerificationLockedException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.auth.entity.EmailVerificationChallenge;
import org.netra.features.auth.repository.EmailVerificationChallengeRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long EXPIRY_MINUTES = 15;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final VerificationProvider verificationProvider;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationChallengeRepository challengeRepository,
            UserRepository userRepository,
            VerificationProvider verificationProvider,
            AuditService auditService) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.verificationProvider = verificationProvider;
        this.auditService = auditService;
    }

    @Transactional
    public void sendVerificationChallenge(User user) {
        String normalizedEmail = user.getEmail().trim().toLowerCase();
        Instant now = Instant.now();

        // 1. Enforce 60-second cooldown protection
        Optional<EmailVerificationChallenge> latestOpt = challengeRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail);
        if (latestOpt.isPresent()) {
            EmailVerificationChallenge latest = latestOpt.get();
            long secondsSince = ChronoUnit.SECONDS.between(latest.getCreatedAt(), now);
            if (secondsSince < RESEND_COOLDOWN_SECONDS && latest.getUsedAt() == null) {
                long remaining = RESEND_COOLDOWN_SECONDS - secondsSince;
                throw new RateLimitExceededException("Please wait " + remaining + " seconds before requesting a new verification code.");
            }
        }

        // 2. Generate cryptographically secure 6-digit code
        int num = secureRandom.nextInt(900_000) + 100_000;
        String rawCode = String.valueOf(num);
        String codeHash = SecurityUtils.sha256Hex(rawCode);

        // 3. Persist challenge record
        EmailVerificationChallenge challenge = new EmailVerificationChallenge(
                user,
                normalizedEmail,
                codeHash,
                now.plus(EXPIRY_MINUTES, ChronoUnit.MINUTES)
        );
        challengeRepository.save(challenge);

        // 4. Dispatch via provider (safe sink / provider, zero exposure in HTTP response)
        verificationProvider.sendVerificationCode(normalizedEmail, rawCode);
        auditService.logAuthEvent("VERIFICATION_CHALLENGE_SENT", user.getId(), null, null, "Email=" + normalizedEmail);
    }

    @Transactional
    public User verifyCode(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();
        Instant now = Instant.now();

        EmailVerificationChallenge challenge = challengeRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new VerificationFailedException("No verification challenge found for this email."));

        if (challenge.getUsedAt() != null) {
            throw new VerificationFailedException("Verification code has already been used. Please request a new code.");
        }

        if (now.isAfter(challenge.getExpiresAt())) {
            throw new VerificationExpiredException("Verification code has expired. Please request a new code.");
        }

        if (challenge.getAttempts() >= MAX_ATTEMPTS) {
            throw new VerificationLockedException("Maximum verification attempts exceeded. Please request a new verification code.");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);

        String providedHash = SecurityUtils.sha256Hex(code.trim());
        if (!challenge.getCodeHash().equals(providedHash)) {
            challengeRepository.save(challenge);
            auditService.logAuthEvent("VERIFICATION_FAILED", challenge.getUser().getId(), null, null, "Attempts=" + challenge.getAttempts());
            throw new VerificationFailedException("Invalid verification code. Please check and try again.");
        }

        // Validated successfully
        challenge.setUsedAt(now);
        challengeRepository.save(challenge);

        User user = challenge.getUser();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);
        User savedUser = userRepository.save(user);

        auditService.logAuthEvent("VERIFICATION_SUCCESS", user.getId(), null, null, "EmailVerifiedAt=" + now);
        return savedUser;
    }
}
