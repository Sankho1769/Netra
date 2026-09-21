package org.netra.features.notification.service;

import java.util.UUID;

/**
 * Interface defining push notification dispatch operations.
 */
public interface PushNotificationService {

    /**
     * Dispatches push delivery for a persisted notification asynchronously.
     *
     * @param notificationId ID of the notification to push
     */
    void sendPushForNotification(UUID notificationId);
}
