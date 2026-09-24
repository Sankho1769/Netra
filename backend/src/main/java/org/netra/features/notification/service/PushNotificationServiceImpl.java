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
import org.springframework.beans.factory.annotation.Autowired;
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
    private final NotificationTransactionalService transactionalService;
    private final org.netra.core.observability.NetraMetrics netraMetrics;

    public PushNotificationServiceImpl(
            PushNotificationProvider pushNotificationProvider,
            UserDeviceTokenRepository userDeviceTokenRepository,
            NotificationRepository notificationRepository) {
        this(pushNotificationProvider, userDeviceTokenRepository, notificationRepository,
                new NotificationTransactionalService(notificationRepository, userDeviceTokenRepository), null);
    }

    public PushNotificationServiceImpl(
            PushNotificationProvider pushNotificationProvider,
            UserDeviceTokenRepository userDeviceTokenRepository,
            NotificationRepository notificationRepository,
            NotificationTransactionalService transactionalService) {
        this(pushNotificationProvider, userDeviceTokenRepository, notificationRepository, transactionalService, null);
    }

    @Autowired
    public PushNotificationServiceImpl(
            PushNotificationProvider pushNotificationProvider,
            UserDeviceTokenRepository userDeviceTokenRepository,
            NotificationRepository notificationRepository,
            NotificationTransactionalService transactionalService,
            @Autowired(required = false) org.netra.core.observability.NetraMetrics netraMetrics) {
        this.pushNotificationProvider = pushNotificationProvider;
        this.userDeviceTokenRepository = userDeviceTokenRepository;
        this.notificationRepository = notificationRepository;
        this.transactionalService = transactionalService != null ? transactionalService
                : new NotificationTransactionalService(notificationRepository, userDeviceTokenRepository);
        this.netraMetrics = netraMetrics;
    }

    @Override
    @Async("notificationTaskExecutor")
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
            log.debug("No active device tokens for recipient {}. Marking notification as NO_DEVICES.", recipientId);
            transactionalService.updateDeliveryStatus(notificationId, NotificationDeliveryStatus.NO_DEVICES);
            if (netraMetrics != null) {
                netraMetrics.incrementNotificationDelivery("NO_DEVICES");
            }
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
                    if (netraMetrics != null) {
                        netraMetrics.incrementNotificationDelivery("SENT");
                    }
                    dt.markSeen(now);
                    transactionalService.saveDeviceTokenRequiresNew(dt);
                } else if (result.isInvalidToken()) {
                    if (netraMetrics != null) {
                        netraMetrics.incrementNotificationDelivery("INVALID_TOKEN");
                    }
                    log.warn("Deactivating invalid device token for user {}", recipientId);
                    dt.revoke(now);
                    transactionalService.saveDeviceTokenRequiresNew(dt);
                } else {
                    if (netraMetrics != null) {
                        netraMetrics.incrementNotificationDelivery("FAILED");
                    }
                    log.warn("Transient push delivery failure for user {}: {}", recipientId, result.getMessage());
                }
            } catch (Exception ex) {
                if (netraMetrics != null) {
                    netraMetrics.incrementNotificationDelivery("FAILED");
                }
                log.error("Unexpected error during push dispatch to device: {}", ex.getMessage());
            }
        }

        NotificationDeliveryStatus finalStatus = atLeastOneSuccess
                ? NotificationDeliveryStatus.SENT
                : NotificationDeliveryStatus.FAILED;

        transactionalService.updateDeliveryStatus(notificationId, finalStatus);
        org.netra.core.observability.StructuredLogger.logOperation(
                "PUSH_DISPATCH",
                recipientId,
                null,
                "Notification",
                notificationId,
                "DISPATCH",
                null,
                finalStatus.name()
        );
    }
}
