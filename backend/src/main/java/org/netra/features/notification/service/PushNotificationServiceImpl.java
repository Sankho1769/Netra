package org.netra.features.notification.service;

import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.NotificationDeliveryStatus;
import org.netra.features.notification.entity.UserDeviceToken;
import org.netra.features.notification.provider.PushDeliveryResult;
import org.netra.features.notification.provider.PushNotificationProvider;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service orchestrating push notification delivery across user devices.
 * Operates asynchronously and detached from core business transactions.
 */
@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    private final PushNotificationProvider pushNotificationProvider;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final NotificationRepository notificationRepository;

    public PushNotificationServiceImpl(
            PushNotificationProvider pushNotificationProvider,
            UserDeviceTokenRepository userDeviceTokenRepository,
            NotificationRepository notificationRepository) {
        this.pushNotificationProvider = pushNotificationProvider;
        this.userDeviceTokenRepository = userDeviceTokenRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Async
    @Transactional
    public void sendPushForNotification(UUID notificationId) {
        if (notificationId == null) {
            return;
        }

        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Cannot dispatch push: notification not found: {}", notificationId);
            return;
        }

        UUID recipientId = notification.getRecipientUserId();
        List<UserDeviceToken> tokens = userDeviceTokenRepository.findByUserIdAndActiveTrue(recipientId);

        if (tokens.isEmpty()) {
            log.debug("No active device tokens for recipient {}. Marking notification as SENT.", recipientId);
            notification.setDeliveryStatus(NotificationDeliveryStatus.SENT);
            notificationRepository.save(notification);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", notification.getId().toString());
        data.put("type", notification.getType().name());
        if (notification.getReferenceType() != null) {
            data.put("referenceType", notification.getReferenceType().name());
        }
        if (notification.getReferenceId() != null) {
            data.put("referenceId", notification.getReferenceId().toString());
        }

        boolean atLeastOneSuccess = false;
        Instant now = Instant.now();

        for (UserDeviceToken dt : tokens) {
            try {
                PushDeliveryResult result = pushNotificationProvider.sendPush(
                        dt.getToken(),
                        notification.getTitle(),
                        notification.getBody(),
                        data
                );

                if (result.isSuccess()) {
                    atLeastOneSuccess = true;
                    dt.markSeen(now);
                    userDeviceTokenRepository.save(dt);
                } else if (result.isInvalidToken()) {
                    log.warn("Deactivating invalid device token for user {}", recipientId);
                    dt.revoke(now);
                    userDeviceTokenRepository.save(dt);
                } else {
                    log.warn("Transient push delivery failure for user {}: {}", recipientId, result.getMessage());
                }
            } catch (Exception ex) {
                log.error("Unexpected error during push dispatch to device: {}", ex.getMessage());
            }
        }

        if (atLeastOneSuccess) {
            notification.setDeliveryStatus(NotificationDeliveryStatus.SENT);
        } else {
            notification.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
        }
        notificationRepository.save(notification);
    }
}
