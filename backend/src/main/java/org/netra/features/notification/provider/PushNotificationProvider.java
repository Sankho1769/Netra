package org.netra.features.notification.provider;

import java.util.Map;

/**
 * Pluggable abstraction for external push notification delivery.
 * Decouples core business transactions from third-party push networks (e.g. Firebase Cloud Messaging).
 */
public interface PushNotificationProvider {

    /**
     * Attempts push notification delivery to a specific device registration token.
     *
     * @param deviceToken The client device push token
     * @param title Notification title
     * @param body Notification body (privacy-safe, no medical data or exact coordinates)
     * @param data Key-value operational metadata
     * @return Delivery result indicating success, transient failure, or permanent token invalidation
     */
    PushDeliveryResult sendPush(String deviceToken, String title, String body, Map<String, String> data);

    /**
     * Provider identifier (e.g. FCM, NOOP).
     */
    String getProviderName();
}
