package org.netra.features.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.notification.provider.FcmPushNotificationProvider;
import org.netra.features.notification.provider.PushDeliveryResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FcmPushNotificationProviderTest {

    @Test
    @DisplayName("FCM Provider: Uninitialized/missing credentials fails explicitly (never simulates success)")
    void testFcmProvider_UnconfiguredFailsExplicitly() {
        // Construct with nonexistent credentials path
        FcmPushNotificationProvider provider = new FcmPushNotificationProvider(
                "test-project-123",
                "nonexistent/path/to/service-account.json"
        );

        assertEquals("FCM", provider.getProviderName());

        PushDeliveryResult result = provider.sendPush(
                "some_device_token_abc123",
                "Test Notification",
                "This is a test body",
                Map.of("key", "val")
        );

        assertNotNull(result);
        assertFalse(result.isSuccess(), "Unconfigured FCM provider must not simulate success");
        assertEquals(PushDeliveryResult.Status.FAILED_TRANSIENT, result.getStatus());
        assertTrue(result.getMessage().contains("FCM uninitialized"));
    }

    @Test
    @DisplayName("FCM Provider: Blank or null device token returns failedInvalidToken")
    void testFcmProvider_BlankToken_ReturnsFailedInvalidToken() {
        FcmPushNotificationProvider provider = new FcmPushNotificationProvider("", "");

        PushDeliveryResult resultNull = provider.sendPush(null, "Title", "Body", Map.of());
        assertFalse(resultNull.isSuccess());

        PushDeliveryResult resultBlank = provider.sendPush("   ", "Title", "Body", Map.of());
        assertFalse(resultBlank.isSuccess());
    }
}
