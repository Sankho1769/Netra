package org.netra.features.notification.service;

import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.features.notification.dto.NotificationDto;
import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.UserDeviceToken;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationTransactionalService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTransactionalService.class);

    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;

    public NotificationTransactionalService(
            NotificationRepository notificationRepository,
            UserDeviceTokenRepository userDeviceTokenRepository) {
        this.notificationRepository = notificationRepository;
        this.userDeviceTokenRepository = userDeviceTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification saveNotificationRequiresNew(Notification notification) {
        return notificationRepository.saveAndFlush(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserDeviceToken saveDeviceTokenRequiresNew(UserDeviceToken token) {
        return userDeviceTokenRepository.saveAndFlush(token);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto markAsReadTransactional(UUID notificationId, UUID currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipientUserId().equals(currentUserId)) {
            log.warn("IDOR attempt: User {} attempted to read notification {} belonging to user {}",
                    currentUserId, notificationId, notification.getRecipientUserId());
            throw new UnauthorizedSessionAccessException("Access denied to notification.");
        }

        if (!notification.isRead()) {
            notification.markAsRead(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationDto.fromEntity(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDeliveryStatus(UUID notificationId, org.netra.features.notification.entity.NotificationDeliveryStatus status) {
        notificationRepository.updateDeliveryStatus(notificationId, status, Instant.now());
    }
}
