package org.netra.features.emergency.service;

import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.emergency.entity.IdempotencyRecord;
import org.netra.features.emergency.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing persistent idempotency records for emergency requests.
 * Default persistent TTL is 24 hours. Replays within 24 hours return the original response;
 * expired records are safely purged or treated as new submissions.
 */
@Service
public class EmergencyIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyIdempotencyService.class);
    public static final String RESOURCE_TYPE_BLOOD_REQUEST = "BLOOD_REQUEST";
    /**
     * V1 persistent idempotency record TTL is strictly 24 hours.
     */
    public static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final IdempotencyRecordRepository repository;

    public EmergencyIdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findRecord(UUID userId, String idempotencyKey, String resourceType) {
        Optional<IdempotencyRecord> recordOpt = repository.findByUserIdAndIdempotencyKeyAndResourceType(
                userId, idempotencyKey, resourceType);

        if (recordOpt.isPresent()) {
            IdempotencyRecord record = recordOpt.get();
            if (record.isExpired()) {
                log.info("Found expired idempotency record for user {}. Treating as new request.", userId);
                return Optional.empty();
            }
            return recordOpt;
        }

        return Optional.empty();
    }

    public Optional<IdempotencyRecord> findForUpdate(UUID userId, String idempotencyKey, String resourceType) {
        return repository.findForUpdate(userId, idempotencyKey, resourceType);
    }

    @Transactional
    public IdempotencyRecord saveAndFlush(IdempotencyRecord record) {
        return repository.saveAndFlush(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteExpiredRecord(UUID userId, String idempotencyKey, String resourceType) {
        return repository.deleteExpiredRecord(userId, idempotencyKey, resourceType, Instant.now());
    }

    @Transactional
    public IdempotencyRecord saveRecord(
            UUID userId,
            String idempotencyKey,
            String requestFingerprint,
            String resourceType,
            UUID resourceId,
            int responseStatus,
            Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl != null ? ttl : DEFAULT_TTL);
        Optional<IdempotencyRecord> existingOpt = repository.findByUserIdAndIdempotencyKeyAndResourceType(
                userId, idempotencyKey, resourceType);

        IdempotencyRecord record;
        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.setRequestFingerprint(requestFingerprint);
            record.setResourceId(resourceId);
            record.setResponseStatus(responseStatus);
            record.setExpiresAt(expiresAt);
        } else {
            record = new IdempotencyRecord(
                    userId,
                    idempotencyKey,
                    requestFingerprint,
                    resourceType,
                    resourceId,
                    responseStatus,
                    expiresAt
            );
        }
        return repository.save(record);
    }

    public String computeFingerprint(EmergencyBloodRequestRequest request) {
        if (request == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(request.getBloodGroup() != null ? request.getBloodGroup().name() : "").append("|");
        sb.append(request.getUnitsRequired() != null ? request.getUnitsRequired() : "").append("|");
        sb.append(request.getHospitalName() != null ? request.getHospitalName().trim().toLowerCase() : "").append("|");
        sb.append(request.getHospitalAddress() != null ? request.getHospitalAddress().trim().toLowerCase() : "").append("|");
        sb.append(request.getCity() != null ? request.getCity().trim().toLowerCase() : "").append("|");
        sb.append(request.getState() != null ? request.getState().trim().toLowerCase() : "").append("|");
        sb.append(request.getPostalCode() != null ? request.getPostalCode().trim() : "").append("|");
        sb.append(request.getLatitude() != null ? Double.toString(request.getLatitude()) : "").append("|");
        sb.append(request.getLongitude() != null ? Double.toString(request.getLongitude()) : "").append("|");
        sb.append(request.getRequiredBy() != null ? request.getRequiredBy().toEpochMilli() : "").append("|");
        sb.append(request.getDescription() != null ? request.getDescription().trim() : "");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    public long cleanExpiredRecords() {
        long deleted = repository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
        return deleted;
    }

    @Scheduled(fixedRate = 3600000) // hourly background cleanup
    @Transactional
    public void cleanupExpiredRecordsTask() {
        cleanExpiredRecords();
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    @Transactional
    public void clear() {
        deleteAll();
    }
}
