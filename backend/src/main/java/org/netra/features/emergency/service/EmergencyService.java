package org.netra.features.emergency.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.IdempotencyKeyReuseException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.SecurityUtils;
import org.netra.features.bloodrequest.dto.BloodRequestDetailDto;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.emergency.dto.EmergencyCreationResult;
import org.netra.features.emergency.entity.IdempotencyRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmergencyService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyService.class);
    private static final Duration MAX_EMERGENCY_WINDOW = Duration.ofHours(72);

    private final BloodRequestService bloodRequestService;
    private final EmergencyIdempotencyService idempotencyService;
    private final EmergencyTransactionalService transactionalService;
    private final RateLimitingService rateLimitingService;
    private final AuditService auditService;

    public EmergencyService(
            BloodRequestService bloodRequestService,
            EmergencyIdempotencyService idempotencyService,
            EmergencyTransactionalService transactionalService,
            RateLimitingService rateLimitingService,
            AuditService auditService) {
        this.bloodRequestService = bloodRequestService;
        this.idempotencyService = idempotencyService;
        this.transactionalService = transactionalService;
        this.rateLimitingService = rateLimitingService;
        this.auditService = auditService;
    }

    public EmergencyCreationResult createEmergencyRequest(
            EmergencyBloodRequestRequest request,
            String idempotencyKey,
            String clientIp,
            String userAgent) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to create an emergency request."));

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new ValidationException("Idempotency-Key header is required.");
        }
        String key = idempotencyKey.trim();
        if (key.length() > 255) {
            throw new ValidationException("Idempotency-Key header must not exceed 255 characters.");
        }

        String fingerprint = idempotencyService.computeFingerprint(request);

        // Pre-check idempotency record FIRST before deadline validation
        Optional<IdempotencyRecord> recordOpt = idempotencyService.findRecord(
                currentUserId, key, EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST);

        if (recordOpt.isPresent()) {
            IdempotencyRecord record = recordOpt.get();
            if (record.getRequestFingerprint().equals(fingerprint)) {
                log.info("Returning idempotent replay for user {}", currentUserId);
                BloodRequestDetailDto detail = (BloodRequestDetailDto) bloodRequestService.getRequestById(record.getResourceId());
                return new EmergencyCreationResult(detail, true);
            } else {
                log.warn("Idempotency key reuse detected for user {}", currentUserId);
                throw new IdempotencyKeyReuseException("Idempotency key has already been used with a different request payload.");
            }
        }

        // Validate emergency deadline (now < requiredBy <= now + 72 hours) only for new submissions
        validateEmergencyDeadline(request.getRequiredBy());

        // Safely remove any expired record so that key reuse requires an INSERT that enforces the unique constraint
        idempotencyService.deleteExpiredRecord(currentUserId, key, EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST);

        // Explicitly acquire rate-limit window key for this new submission
        String windowKey = rateLimitingService.checkEmergencyRateLimit(currentUserId.toString());

        try {
            return transactionalService.executeCreation(currentUserId, request, key, fingerprint, windowKey, clientIp, userAgent);
        } catch (DataIntegrityViolationException ex) {
            if (!isIdempotencyConstraintViolation(ex)) {
                throw ex;
            }
            rateLimitingService.decrementEmergencyRateLimit(currentUserId.toString(), windowKey);
            log.warn("Concurrent duplicate submission detected for user {}. Resolving replay.", currentUserId);
            Optional<IdempotencyRecord> concurrentRecord = Optional.empty();
            for (int retry = 0; retry < 5; retry++) {
                concurrentRecord = idempotencyService.findRecord(
                        currentUserId, key, EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST);
                if (concurrentRecord.isPresent()) {
                    break;
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (concurrentRecord.isPresent()) {
                IdempotencyRecord record = concurrentRecord.get();
                if (record.getRequestFingerprint().equals(fingerprint)) {
                    BloodRequestDetailDto detail = (BloodRequestDetailDto) bloodRequestService.getRequestById(record.getResourceId());
                    return new EmergencyCreationResult(detail, true);
                } else {
                    throw new IdempotencyKeyReuseException("Idempotency key has already been used with a different request payload.");
                }
            }
            throw ex;
        }
    }

    public BloodRequestDetailDto cancelEmergencyRequest(
            UUID id,
            CancelBloodRequestRequest request,
            String clientIp,
            String userAgent) {

        // Delegate cancellation to BloodRequestService (validates CRITICAL urgency, ownership/admin, validates status)
        return bloodRequestService.cancelEmergencyRequest(id, request, clientIp, userAgent);
    }

    public boolean isIdempotencyConstraintViolation(DataIntegrityViolationException ex) {
        if (ex == null) {
            return false;
        }
        // 1. Inspect nested Hibernate ConstraintViolationException and SQLException if available
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraint = cve.getConstraintName();
                if (constraint != null && constraint.toLowerCase(Locale.ROOT).contains("uq_idempotency_user_key_resource")) {
                    return true;
                }
            }
            if (current instanceof java.sql.SQLException sqlEx) {
                String sqlMsg = sqlEx.getMessage();
                if (sqlMsg != null && sqlMsg.toLowerCase(Locale.ROOT).contains("uq_idempotency_user_key_resource")) {
                    return true;
                }
            }
            current = current.getCause();
        }

        // 2. Safe message-based fallback for H2 and diverse JDBC drivers
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable mostSpecific = ex.getMostSpecificCause();
        String mostSpecificMsg = (mostSpecific != null && mostSpecific.getMessage() != null)
                ? mostSpecific.getMessage()
                : "";
        String combined = (msg + " " + mostSpecificMsg).toLowerCase(Locale.ROOT);

        if (combined.contains("uq_idempotency_user_key_resource")) {
            return true;
        }

        boolean isUniqueViolation = combined.contains("unique") || combined.contains("23505") || combined.contains("duplicate");
        boolean isIdempotencyTable = combined.contains("idempotency_records");
        boolean hasCompositeKeys = combined.contains("user_id") && combined.contains("idempotency_key") && combined.contains("resource_type");

        return isUniqueViolation && isIdempotencyTable && hasCompositeKeys;
    }

    private void validateEmergencyDeadline(Instant requiredBy) {
        if (requiredBy == null) {
            throw new ValidationException("Required-by deadline is required.");
        }
        Instant now = Instant.now();
        if (!requiredBy.isAfter(now)) {
            throw new ValidationException("Required-by deadline must be strictly in the future.");
        }
        Instant maxDeadline = now.plus(MAX_EMERGENCY_WINDOW);
        if (requiredBy.isAfter(maxDeadline)) {
            throw new ValidationException("Emergency blood requests must specify a required-by deadline within 72 hours.");
        }
    }
}
