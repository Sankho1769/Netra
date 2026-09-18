package org.netra.features.emergency.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.IdempotencyKeyReuseException;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.features.bloodrequest.dto.BloodRequestDetailDto;
import org.netra.features.bloodrequest.dto.CreateBloodRequestRequest;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.emergency.dto.EmergencyCreationResult;
import org.netra.features.emergency.entity.IdempotencyRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmergencyTransactionalService {

    private final BloodRequestService bloodRequestService;
    private final EmergencyIdempotencyService idempotencyService;
    private final RateLimitingService rateLimitingService;
    private final AuditService auditService;

    public EmergencyTransactionalService(
            BloodRequestService bloodRequestService,
            EmergencyIdempotencyService idempotencyService,
            RateLimitingService rateLimitingService,
            AuditService auditService) {
        this.bloodRequestService = bloodRequestService;
        this.idempotencyService = idempotencyService;
        this.rateLimitingService = rateLimitingService;
        this.auditService = auditService;
    }

    @Transactional
    public EmergencyCreationResult executeCreation(
            UUID currentUserId,
            EmergencyBloodRequestRequest request,
            String idempotencyKey,
            String fingerprint,
            String windowKey,
            String clientIp,
            String userAgent) {

        // Check if an active record was committed by another thread while entering
        Optional<IdempotencyRecord> activeOpt = idempotencyService.findRecord(
                currentUserId, idempotencyKey, EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST);

        if (activeOpt.isPresent()) {
            rateLimitingService.decrementEmergencyRateLimit(currentUserId.toString(), windowKey);
            IdempotencyRecord record = activeOpt.get();
            if (record.getRequestFingerprint().equals(fingerprint)) {
                BloodRequestDetailDto detail = (BloodRequestDetailDto) bloodRequestService.getRequestById(record.getResourceId());
                return new EmergencyCreationResult(detail, true);
            } else {
                throw new IdempotencyKeyReuseException("Idempotency key has already been used with a different request payload.");
            }
        }

        // Map to domain request with server-enforced CRITICAL urgency
        CreateBloodRequestRequest domainRequest = mapToDomainRequest(request);
        BloodRequestDetailDto created = bloodRequestService.createEmergencyBloodRequest(domainRequest, clientIp, userAgent);

        // Persistent insert: DB unique constraint enforces strictly 1 winner among concurrent threads
        IdempotencyRecord newRecord = new IdempotencyRecord(
                currentUserId,
                idempotencyKey,
                fingerprint,
                EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST,
                created.getId(),
                201,
                Instant.now().plus(EmergencyIdempotencyService.DEFAULT_TTL)
        );
        idempotencyService.saveAndFlush(newRecord);

        // Log dedicated EMERGENCY_REQUEST_CREATED audit event
        auditService.logAuthEvent(
                "EMERGENCY_REQUEST_CREATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"requestId\":\"" + created.getId() + "\"}"
        );

        return new EmergencyCreationResult(created, false);
    }

    private CreateBloodRequestRequest mapToDomainRequest(EmergencyBloodRequestRequest request) {
        CreateBloodRequestRequest domainRequest = new CreateBloodRequestRequest();
        domainRequest.setBloodGroup(request.getBloodGroup());
        domainRequest.setUnitsRequired(request.getUnitsRequired());
        domainRequest.setUrgency(BloodRequestUrgency.CRITICAL); // Server-enforced
        domainRequest.setHospitalName(request.getHospitalName());
        domainRequest.setHospitalAddress(request.getHospitalAddress());
        domainRequest.setCity(request.getCity());
        domainRequest.setState(request.getState());
        domainRequest.setPostalCode(request.getPostalCode());
        domainRequest.setLatitude(request.getLatitude());
        domainRequest.setLongitude(request.getLongitude());
        domainRequest.setRequiredBy(request.getRequiredBy());
        domainRequest.setDescription(request.getDescription());
        return domainRequest;
    }
}
