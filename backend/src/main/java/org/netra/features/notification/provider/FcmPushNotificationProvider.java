package org.netra.features.notification.provider;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Production push provider implementation using Google Firebase Cloud Messaging (FCM HTTP v1).
 *
 * Operational safety:
 * - Credentials and project IDs are injected strictly via configuration/environment variables.
 * - Raw credentials or secret keys are never committed to the repository.
 * - If FCM credentials or configuration are missing, fails explicitly with descriptive error
 *   (never silently fakes delivery).
 * - Device tokens are masked in operational logging to protect recipient privacy.
 */
@Component
@ConditionalOnProperty(name = "netra.notifications.push.provider", havingValue = "fcm")
public class FcmPushNotificationProvider implements PushNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationProvider.class);
    private static final String APP_NAME = "NETRA_FCM_APP";

    private final FirebaseApp firebaseApp;
    private final String initErrorMessage;

    @Autowired
    public FcmPushNotificationProvider(
            @Value("${netra.notifications.push.fcm.project-id:${FCM_PROJECT_ID:}}") String projectId,
            @Value("${netra.notifications.push.fcm.credentials-path:${FCM_CREDENTIALS_PATH:}}") String credentialsPath) {

        FirebaseApp app = null;
        String error = null;

        try {
            for (FirebaseApp existing : FirebaseApp.getApps()) {
                if (existing.getName().equals(APP_NAME) || existing.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                    app = existing;
                    break;
                }
            }

            if (app == null) {
                GoogleCredentials credentials = null;
                if (credentialsPath != null && !credentialsPath.isBlank()) {
                    File file = new File(credentialsPath);
                    if (file.exists() && file.isFile()) {
                        try (InputStream stream = new FileInputStream(file)) {
                            credentials = GoogleCredentials.fromStream(stream);
                        }
                    } else {
                        InputStream cpStream = getClass().getClassLoader().getResourceAsStream(credentialsPath);
                        if (cpStream != null) {
                            try (cpStream) {
                                credentials = GoogleCredentials.fromStream(cpStream);
                            }
                        } else {
                            throw new IllegalArgumentException("Configured FCM credentials path not found: " + credentialsPath);
                        }
                    }
                } else {
                    credentials = GoogleCredentials.getApplicationDefault();
                }

                FirebaseOptions.Builder builder = FirebaseOptions.builder().setCredentials(credentials);
                if (projectId != null && !projectId.isBlank()) {
                    builder.setProjectId(projectId);
                }
                app = FirebaseApp.initializeApp(builder.build(), APP_NAME);
                log.info("Initialized Firebase Admin SDK for NETRA push notifications (project: {})", projectId);
            }
        } catch (Exception ex) {
            log.error("Failed to initialize Firebase Admin SDK: {}", ex.getMessage());
            error = ex.getMessage();
        }

        this.firebaseApp = app;
        this.initErrorMessage = error;
    }

    public FcmPushNotificationProvider(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
        this.initErrorMessage = null;
    }

    @Override
    public PushDeliveryResult sendPush(String deviceToken, String title, String body, Map<String, String> data) {
        String maskedToken = maskToken(deviceToken);

        if (firebaseApp == null) {
            log.error("FCM push delivery failed: Firebase Admin SDK is not initialized. Error: {}", initErrorMessage);
            return PushDeliveryResult.failedTransient("FCM uninitialized: " + (initErrorMessage != null ? initErrorMessage : "Missing credentials"));
        }

        if (deviceToken == null || deviceToken.isBlank()) {
            return PushDeliveryResult.failedInvalidToken("Device token is blank or null");
        }

        try {
            com.google.firebase.messaging.Notification fcmNotification =
                    com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(fcmNotification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
            String messageId = messaging.send(messageBuilder.build());

            log.info("Successfully dispatched FCM push notification to device: {}, messageId: {}", maskedToken, messageId);
            return PushDeliveryResult.success(messageId);

        } catch (FirebaseMessagingException fme) {
            MessagingErrorCode errorCode = fme.getMessagingErrorCode();
            String detail = fme.getMessage();
            log.warn("FCM delivery failure for device {}: code={}, message={}", maskedToken, errorCode, detail);

            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                return PushDeliveryResult.failedInvalidToken("FCM invalid token (" + errorCode + "): " + detail);
            }
            return PushDeliveryResult.failedTransient("FCM send failure (" + errorCode + "): " + detail);

        } catch (Exception ex) {
            log.error("Unexpected error dispatching FCM push to device {}: {}", maskedToken, ex.getMessage());
            return PushDeliveryResult.failedTransient("Unexpected FCM error: " + ex.getMessage());
        }
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
