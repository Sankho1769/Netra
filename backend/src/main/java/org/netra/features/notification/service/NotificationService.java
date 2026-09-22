package org.netra.features.notification.service;

import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.notification.dto.*;
import org.netra.features.notification.entity.*;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationTransactionalService transactionalService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserDeviceTokenRepository userDeviceTokenRepository,
            PushNotificationService pushNotificationService) {
        this(notificationRepository, userDeviceTokenRepository, pushNotificationService,
                new NotificationTransactionalService(notificationRepository, userDeviceTokenRepository));
    }

    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            UserDeviceTokenRepository userDeviceTokenRepository,
            PushNotificationService pushNotificationService,
            NotificationTransactionalService transactionalService) {
        this.notificationRepository = notificationRepository;
        this.userDeviceTokenRepository = userDeviceTokenRepository;
        this.pushNotificationService = pushNotificationService;
        this.transactionalService = transactionalService != null ? transactionalService
                : new NotificationTransactionalService(notificationRepository, userDeviceTokenRepository);
    }

    /**
     * Persists an in-app notification idempotently and triggers push delivery asynchronously.
     * Uses isolated transactional persistence with deduplication fallback.
     */
    public Notification createNotification(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String body,
            NotificationReferenceType referenceType,
            UUID referenceId,
            String idempotencyKey) {

        if (recipientUserId == null) {
            throw new ValidationException("Recipient user ID is required for notification.");
        }
        if (type == null) {
            throw new ValidationException("Notification type is required.");
        }
        if (title == null || title.isBlank()) {
            throw new ValidationException("Notification title is required.");
        }
        if (body == null || body.isBlank()) {
            throw new ValidationException("Notification body is required.");
        }

        // Idempotency pre-check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                log.info("Duplicate notification suppressed by idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        Notification notification = new Notification(
                recipientUserId,
                type,
                title.trim(),
                body.trim(),
                referenceType,
                referenceId,
                idempotencyKey != null ? idempotencyKey.trim() : null,
                Instant.now()
        );

        Notification saved;
        try {
            saved = transactionalService.saveNotificationRequiresNew(notification);
        } catch (DataIntegrityViolationException ex) {
            if (idempotencyKey != null) {
                for (int attempt = 0; attempt < 5; attempt++) {
                    Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey.trim());
                    if (existing.isPresent()) {
                        log.info("Concurrent duplicate notification suppressed by idempotency key: {}", idempotencyKey);
                        return existing.get();
                    }
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            throw ex;
        }

        log.info("Created notification {} of type {} for user {}", saved.getId(), type, recipientUserId);

        // Asynchronously dispatch push delivery
        try {
            pushNotificationService.sendPushForNotification(saved.getId());
        } catch (Exception ex) {
            log.error("Failed to trigger push notification dispatch: {}", ex.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(UUID currentUserId, boolean unreadOnly, Pageable pageable) {
        if (currentUserId == null) {
            throw new UnauthorizedSessionAccessException("User authentication required.");
        }

        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(currentUserId, pageable)
                : notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(currentUserId, pageable);

        return page.map(NotificationDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(UUID currentUserId) {
        if (currentUserId == null) {
            throw new UnauthorizedSessionAccessException("User authentication required.");
        }
        long count = notificationRepository.countByRecipientUserIdAndReadAtIsNull(currentUserId);
        return new UnreadCountDto(count);
    }

    public NotificationDto markAsRead(UUID notificationId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new UnauthorizedSessionAccessException("User authentication required.");
        }
        if (notificationId == null) {
            throw new ValidationException("Notification ID is required.");
        }

        for (int retry = 0; retry < 5; retry++) {
            try {
                return transactionalService.markAsReadTransactional(notificationId, currentUserId);
            } catch (ConcurrencyFailureException ex) {
                log.info("Concurrent mark-as-read conflict for notification {}, retrying (attempt {})", notificationId, retry + 1);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Final attempt or fallback to latest entity state
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        return NotificationDto.fromEntity(n);
    }

    @Transactional
    public MarkAllReadResponseDto markAllAsRead(UUID currentUserId) {
        if (currentUserId == null) {
            throw new UnauthorizedSessionAccessException("User authentication required.");
        }

        int updated = notificationRepository.markAllAsRead(currentUserId, Instant.now());
        log.info("Marked {} notifications as read for user {}", updated, currentUserId);
        return new MarkAllReadResponseDto(updated);
    }

    public DeviceTokenDto registerDeviceToken(UUID currentUserId, RegisterDeviceTokenRequest request) {
        if (currentUserId == null) {
            throw new UnauthorizedSessionAccessException("User authentication required.");
        }
        if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new ValidationException("Device token must not be empty.");
        }
        if (request.getProvider() != null && !request.getProvider().trim().equalsIgnoreCase("FCM")) {
            throw new ValidationException("Only FCM provider is supported for device tokens.");
        }

        String rawToken = request.getToken().trim();
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        Instant now = Instant.now();

        Optional<UserDeviceToken> existingOpt = userDeviceTokenRepository.findByToken(rawToken);

        UserDeviceToken tokenEntity;
        if (existingOpt.isPresent()) {
            tokenEntity = existingOpt.get();
            // Securely reassign token to current authenticated user
            tokenEntity.setUserId(currentUserId);
            tokenEntity.setPlatform(request.getPlatform() != null ? request.getPlatform() : DevicePlatform.ANDROID);
            tokenEntity.setProvider("FCM");
            tokenEntity.setActive(true);
            tokenEntity.setRevokedAt(null);
            tokenEntity.setLastSeenAt(now);
            tokenEntity.setUpdatedAt(now);
        } else {
            tokenEntity = new UserDeviceToken(
                    currentUserId,
                    rawToken,
                    tokenHash,
                    request.getPlatform(),
                    "FCM"
            );
        }

        UserDeviceToken saved;
        try {
            saved = transactionalService.saveDeviceTokenRequiresNew(tokenEntity);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent registration: re-fetch the concurrent record and update
            Optional<UserDeviceToken> concurrent = userDeviceTokenRepository.findByToken(rawToken);
            if (concurrent.isPresent()) {
                tokenEntity = concurrent.get();
                tokenEntity.setUserId(currentUserId);
                tokenEntity.setPlatform(request.getPlatform() != null ? request.getPlatform() : DevicePlatform.ANDROID);
                tokenEntity.setProvider("FCM");
                tokenEntity.setActive(true);
                tokenEntity.setRevokedAt(null);
                tokenEntity.setLastSeenAt(now);
                tokenEntity.setUpdatedAt(now);
                saved = transactionalService.saveDeviceTokenRequiresNew(tokenEntity);
            } else {
                throw ex;
            }
        }

        log.info("Registered device token {} for user {}", saved.getId(), currentUserId);
        return DeviceTokenDto.fromEntity(saved);
    }

    @Transactional
    public void revokeDeviceToken(UUID tokenId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new UnauthorizedSessionAccessException("User authentication required.");
        }
        if (tokenId == null) {
            throw new ValidationException("Token ID is required.");
        }

        UserDeviceToken token = userDeviceTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Device token not found with id: " + tokenId));

        if (!token.getUserId().equals(currentUserId)) {
            log.warn("IDOR attempt: User {} attempted to revoke token {} belonging to user {}",
                    currentUserId, tokenId, token.getUserId());
            throw new UnauthorizedSessionAccessException("Access denied to device token.");
        }

        token.revoke(Instant.now());
        userDeviceTokenRepository.save(token);
        log.info("Revoked device token {} for user {}", tokenId, currentUserId);
    }
}
