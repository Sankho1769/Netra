package org.netra.features.fulfillment.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.fulfillment.dto.*;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;
import org.netra.features.fulfillment.event.*;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.matching.rules.BloodCompatibilityMatrix;
import org.netra.features.user.entity.User;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class FulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);

    private final FulfillmentRepository fulfillmentRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;
    private final FulfillmentAuthorizationService authorizationService;
    private final BloodCompatibilityMatrix compatibilityMatrix;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.netra.core.observability.NetraMetrics netraMetrics;

    public void setNetraMetrics(org.netra.core.observability.NetraMetrics netraMetrics) {
        this.netraMetrics = netraMetrics;
    }

    public FulfillmentService(
            FulfillmentRepository fulfillmentRepository,
            BloodRequestRepository bloodRequestRepository,
            DonationRepository donationRepository,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            FulfillmentAuthorizationService authorizationService,
            BloodCompatibilityMatrix compatibilityMatrix,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donationRepository = donationRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.compatibilityMatrix = compatibilityMatrix;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new operational fulfillment claim in READY status.
     * Concurrency safe via pessimistic lock on BloodRequest.
     */
    @Transactional
    public FulfillmentDto createFulfillment(CreateFulfillmentRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));

        // 1. Lock BloodRequest for update to prevent concurrent over-allocation
        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(request.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + request.getBloodRequestId()));

        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot fulfill blood request that is " + bloodRequest.getStatus() + ".");
        }

        // 2. Find and validate Verified Donation
        Donation donation = donationRepository.findById(request.getDonationId())
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + request.getDonationId()));

        if (donation.getVerificationStatus() != DonationVerificationStatus.VERIFIED) {
            throw new ValidationException("Only VERIFIED donations may be used for fulfillment. Current status: " + donation.getVerificationStatus());
        }

        // 3. Authorization check (strictly scoped to donation's host blood bank if event-based)
        authorizationService.verifyCanCreateFulfillment(currentUserId, bloodRequest, donation);

        // 4. Prevent double consumption / reservation
        if (fulfillmentRepository.findActiveByDonationId(donation.getId()).isPresent()) {
            throw new ValidationException("Donation has already been consumed or reserved for another fulfillment.");
        }

        // 5. Blood group compatibility check
        DonorProfile donorProfile = donorProfileRepository.findByUserId(donation.getDonorUserId()).orElse(null);
        if (donorProfile != null && donorProfile.getBloodGroup() != null) {
            Set<BloodGroup> compatible = compatibilityMatrix.getCompatibleDonorGroups(bloodRequest.getBloodGroup());
            if (!compatible.contains(donorProfile.getBloodGroup())) {
                throw new ValidationException("Donor blood group (" + donorProfile.getBloodGroup().getCode() +
                        ") is incompatible with requested blood group (" + bloodRequest.getBloodGroup().getCode() + ").");
            }
        }

        // 6. Quantity check against remaining unreserved units
        int reservedUnits = fulfillmentRepository.sumReservedUnitsForBloodRequest(bloodRequest.getId());
        int remainingUnreserved = bloodRequest.getUnitsRequired() - bloodRequest.getUnitsFulfilled() - reservedUnits;
        if (request.getUnits() > remainingUnreserved) {
            if (netraMetrics != null) {
                netraMetrics.incrementFulfillmentContention();
            }
            throw new ValidationException("Requested fulfillment units (" + request.getUnits() +
                    ") exceeds the blood request's remaining unreserved quantity (" + remainingUnreserved + ").");
        }

        // 7. Persist fulfillment
        Fulfillment fulfillment = new Fulfillment(
                bloodRequest.getId(),
                donation.getId(),
                request.getUnits(),
                currentUserId,
                request.getNotes()
        );
        Fulfillment saved = fulfillmentRepository.save(fulfillment);

        // 8. Audit log
        auditService.logAuthEvent(
                "FULFILLMENT_CREATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"fulfillmentId\":\"" + saved.getId() + "\",\"bloodRequestId\":\"" + bloodRequest.getId() + "\",\"units\":" + saved.getUnits() + "}"
        );

        if (netraMetrics != null) {
            netraMetrics.incrementFulfillmentsCreated();
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "FULFILLMENT_CREATED", currentUserId, null, "Fulfillment", saved.getId(), "CREATE", null, "SUCCESS");

        // 9. Publish domain event
        eventPublisher.publishEvent(new FulfillmentCreatedEvent(
                saved.getId(),
                bloodRequest.getId(),
                donation.getId(),
                bloodRequest.getRequesterUserId(),
                donation.getDonorUserId(),
                saved.getUnits()
        ));

        return FulfillmentDto.fromEntity(saved);
    }

    /**
     * Starts clinical fulfillment (READY -> IN_PROGRESS).
     * Authorized only for blood bank staff and admins.
     */
    @Transactional
    public FulfillmentDto startFulfillment(UUID id, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));

        Fulfillment fulfillment = fulfillmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + id));

        // 1. Lock BloodRequest for update in consistent lock order (BloodRequest -> Donation -> Fulfillment)
        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(fulfillment.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + fulfillment.getBloodRequestId()));

        // 2. Fetch Donation
        Donation donation = donationRepository.findById(fulfillment.getDonationId()).orElse(null);

        // 3. Authorization check
        authorizationService.verifyCanOperateFulfillment(currentUserId, fulfillment, donation);

        // 4. Invariant checks against freshly locked BloodRequest and verified Donation
        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot start fulfillment for blood request that is " + bloodRequest.getStatus() + ".");
        }

        if (donation == null || donation.getVerificationStatus() != DonationVerificationStatus.VERIFIED) {
            throw new ValidationException("Cannot start fulfillment with an unverified donation.");
        }

        fulfillment.start(currentUserId, Instant.now());
        Fulfillment saved = fulfillmentRepository.save(fulfillment);

        auditService.logAuthEvent(
                "FULFILLMENT_STARTED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"fulfillmentId\":\"" + saved.getId() + "\"}"
        );

        if (donation != null) {
            eventPublisher.publishEvent(new FulfillmentStartedEvent(
                    saved.getId(),
                    bloodRequest.getId(),
                    donation.getId(),
                    bloodRequest.getRequesterUserId(),
                    donation.getDonorUserId()
            ));
        }

        return FulfillmentDto.fromEntity(saved);
    }

    /**
     * Completes clinical fulfillment (IN_PROGRESS -> FULFILLED).
     * Atomically increments fulfilled units and marks BloodRequest FULFILLED if complete.
     */
    @Transactional
    public FulfillmentDto completeFulfillment(UUID id, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));

        Fulfillment fulfillment = fulfillmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + id));

        // 1. Lock BloodRequest for update in consistent lock order (BloodRequest -> Donation -> Fulfillment)
        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(fulfillment.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + fulfillment.getBloodRequestId()));

        // 2. Fetch Donation
        Donation donation = donationRepository.findById(fulfillment.getDonationId()).orElse(null);

        // 3. Authorization check
        authorizationService.verifyCanOperateFulfillment(currentUserId, fulfillment, donation);

        // 4. Invariant checks against freshly locked BloodRequest and verified Donation
        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot complete fulfillment for blood request that is " + bloodRequest.getStatus() + ".");
        }

        if (donation == null || donation.getVerificationStatus() != DonationVerificationStatus.VERIFIED) {
            throw new ValidationException("Cannot complete fulfillment with an unverified donation.");
        }

        // 5. Quantity correctness evaluated against freshly locked BloodRequest
        int newFulfilled = bloodRequest.getUnitsFulfilled() + fulfillment.getUnits();
        if (newFulfilled > bloodRequest.getUnitsRequired()) {
            throw new ValidationException("Fulfillment units would exceed remaining required units on the blood request.");
        }

        Instant now = Instant.now();
        fulfillment.complete(currentUserId, now);

        bloodRequest.setUnitsFulfilled(newFulfilled);

        boolean requestCompleted = newFulfilled >= bloodRequest.getUnitsRequired();
        if (requestCompleted) {
            bloodRequest.setStatus(BloodRequestStatus.FULFILLED);
            bloodRequest.setFulfilledAt(now);
            bloodRequest.setFulfilledBy(currentUserId);
        }
        bloodRequest.setUpdatedAt(now);

        bloodRequestRepository.save(bloodRequest);
        Fulfillment saved = fulfillmentRepository.save(fulfillment);

        auditService.logAuthEvent(
                "FULFILLMENT_COMPLETED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"fulfillmentId\":\"" + saved.getId() + "\",\"bloodRequestId\":\"" + bloodRequest.getId() +
                        "\",\"unitsFulfilled\":" + newFulfilled + ",\"requestCompleted\":" + requestCompleted + "}"
        );

        if (donation != null) {
            eventPublisher.publishEvent(new FulfillmentCompletedEvent(
                    saved.getId(),
                    bloodRequest.getId(),
                    donation.getId(),
                    bloodRequest.getRequesterUserId(),
                    donation.getDonorUserId(),
                    fulfillment.getUnits(),
                    requestCompleted
            ));
        }

        return FulfillmentDto.fromEntity(saved);
    }

    /**
     * Fails clinical fulfillment (IN_PROGRESS -> FAILED).
     * Releases reservation on the donation and request.
     */
    @Transactional
    public FulfillmentDto failFulfillment(UUID id, FailFulfillmentRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));

        Fulfillment fulfillment = fulfillmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + id));

        // Lock BloodRequest for update in consistent lock order
        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(fulfillment.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + fulfillment.getBloodRequestId()));

        Donation donation = donationRepository.findById(fulfillment.getDonationId()).orElse(null);
        authorizationService.verifyCanOperateFulfillment(currentUserId, fulfillment, donation);

        fulfillment.fail(currentUserId, request.getFailureReason(), Instant.now());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            fulfillment.setNotes((fulfillment.getNotes() != null ? fulfillment.getNotes() + "\n" : "") +
                    "[Failure Notes]: " + request.getNotes().trim());
        }
        Fulfillment saved = fulfillmentRepository.save(fulfillment);

        auditService.logAuthEvent(
                "FULFILLMENT_FAILED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"fulfillmentId\":\"" + saved.getId() + "\",\"reason\":\"" + request.getFailureReason() + "\"}"
        );

        if (donation != null) {
            eventPublisher.publishEvent(new FulfillmentFailedEvent(
                    saved.getId(),
                    bloodRequest.getId(),
                    donation.getId(),
                    bloodRequest.getRequesterUserId(),
                    donation.getDonorUserId(),
                    request.getFailureReason()
            ));
        }

        return FulfillmentDto.fromEntity(saved);
    }

    /**
     * Cancels fulfillment in READY status.
     * Releases reservation on the donation and request.
     */
    @Transactional
    public FulfillmentDto cancelFulfillment(UUID id, CancelFulfillmentRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));

        Fulfillment fulfillment = fulfillmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + id));

        // Lock BloodRequest for update in consistent lock order
        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(fulfillment.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + fulfillment.getBloodRequestId()));

        Donation donation = donationRepository.findById(fulfillment.getDonationId()).orElse(null);
        authorizationService.verifyCanCancelFulfillment(currentUserId, fulfillment, bloodRequest, donation);

        fulfillment.cancel(currentUserId, request.getCancellationReason(), Instant.now());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            fulfillment.setNotes((fulfillment.getNotes() != null ? fulfillment.getNotes() + "\n" : "") +
                    "[Cancellation Notes]: " + request.getNotes().trim());
        }
        Fulfillment saved = fulfillmentRepository.save(fulfillment);

        auditService.logAuthEvent(
                "FULFILLMENT_CANCELLED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"fulfillmentId\":\"" + saved.getId() + "\",\"reason\":\"" + request.getCancellationReason() + "\"}"
        );

        if (donation != null) {
            eventPublisher.publishEvent(new FulfillmentCancelledEvent(
                    saved.getId(),
                    bloodRequest.getId(),
                    donation.getId(),
                    bloodRequest.getRequesterUserId(),
                    donation.getDonorUserId(),
                    request.getCancellationReason()
            ));
        }

        return FulfillmentDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<FulfillmentDto> getMyFulfillments(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));
        Pageable bounded = boundPageable(pageable);
        return fulfillmentRepository.findMyFulfillments(currentUserId, bounded)
                .map(FulfillmentDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<FulfillmentDto> getPendingFulfillments(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));
        authorizationService.verifyCanViewPendingQueue(currentUserId);
        Pageable bounded = boundPageable(pageable);
        return fulfillmentRepository.findPendingFulfillments(bounded)
                .map(FulfillmentDto::fromEntity);
    }

    private Pageable boundPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(50, Math.max(1, pageable.getPageSize()));
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(page, size, sort);
    }

    @Transactional(readOnly = true)
    public FulfillmentDetailDto getFulfillmentById(UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication required."));

        Fulfillment fulfillment = fulfillmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + id));

        BloodRequest bloodRequest = bloodRequestRepository.findById(fulfillment.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated blood request not found."));

        Donation donation = donationRepository.findById(fulfillment.getDonationId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated donation not found."));

        authorizationService.verifyCanViewFulfillment(currentUserId, fulfillment, bloodRequest, donation);

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        DonorProfile donorProfile = donorProfileRepository.findByUserId(donation.getDonorUserId()).orElse(null);
        BloodGroup donorBloodGroup = donorProfile != null ? donorProfile.getBloodGroup() : null;

        boolean isRequester = bloodRequest.getRequesterUserId().equals(currentUserId);
        boolean isDonor = donation.getDonorUserId().equals(currentUserId);
        boolean canManage = authorizationService.canManage(currentUserId, currentUser);

        return FulfillmentDetailDto.from(
                fulfillment,
                bloodRequest,
                donation,
                donorBloodGroup,
                isRequester,
                isDonor,
                canManage
        );
    }
}
