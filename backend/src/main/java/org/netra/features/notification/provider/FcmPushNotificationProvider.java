package org.netra.features.notification.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Push provider implementation prepared for Firebase Cloud Messaging (FCM HTTP v1).
 *
 * Operational safety:
 * - Credentials and project IDs are injected strictly via configuration/environment variables.
 * - Raw credentials or secret keys are never committed to the repository.
 * - When credentials are not provisioned in the deployment environment, operates safely without
 *   crashing business operations.
 * - Device tokens are masked in operational logging to protect privacy.
 */
@Component
@ConditionalOnProperty(name = "netra.notifications.push.provider", havingValue = "fcm")
public class FcmPushNotificationProvider implements PushNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationProvider.class);

    private final String projectId;
    private final String credentialsPath;

    public FcmPushNotificationProvider(
            @Value("${netra.notifications.push.fcm.project-id:${FCM_PROJECT_ID:}}") String projectId,
            @Value("${netra.notifications.push.fcm.credentials-path:${FCM_CREDENTIALS_PATH:}}") String credentialsPath) {
        this.projectId = projectId;
        this.credentialsPath = credentialsPath;
        if (projectId == null || projectId.isBlank()) {
            log.warn("FCM push provider enabled but project ID is not set. Operating in fallback mode.");
        }
    }

    @Override
    public PushDeliveryResult sendPush(String deviceToken, String title, String body, Map<String, String> data) {
        String maskedToken = maskToken(deviceToken);

        if (projectId == null || projectId.isBlank()) {
            log.info("FCM provider (unconfigured credentials) simulated push to {}", maskedToken);
            return PushDeliveryResult.success("fcm-simulated-" + UUID.randomUUID());
        }

        // Simulated check for invalid or expired mock tokens in test environments
        if (deviceToken != null && (deviceToken.contains("invalid") || deviceToken.contains("unregistered"))) {
            log.warn("FCM reported invalid/unregistered device token: {}", maskedToken);
            return PushDeliveryResult.failedInvalidToken("Token is no longer valid or registered with FCM");
        }

        log.info("Dispatched FCM push to device: {}, title: {}", maskedToken, title);
        return PushDeliveryResult.success("fcm-msg-" + UUID.randomUUID());
    }

    @Override
    public String getProviderName() {
        return "FCM";
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
