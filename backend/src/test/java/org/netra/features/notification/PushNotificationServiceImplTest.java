package org.netra.features.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.netra.features.notification.entity.*;
import org.netra.features.notification.provider.PushDeliveryResult;
import org.netra.features.notification.provider.PushNotificationProvider;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.netra.features.notification.service.PushNotificationServiceImpl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

    @Mock
    private PushNotificationProvider pushNotificationProvider;

    @Mock
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private PushNotificationServiceImpl pushNotificationService;

    @BeforeEach
    void setUp() {
        pushNotificationService = new PushNotificationServiceImpl(
                pushNotificationProvider,
                userDeviceTokenRepository,
                notificationRepository
        );
    }

    @Test
    @DisplayName("Push: When no active devices exist, marks notification as NO_DEVICES")
    void testSendPush_NoActiveDevices_MarksNoDevices() {
        UUID notifId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        Notification notif = new Notification(
                recipientId,
                NotificationType.MATCH_CREATED,
                "Title",
                "Body",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                "KEY:1",
                Instant.now()
        );
        notif.setId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(userDeviceTokenRepository.findByUserIdAndActiveTrue(recipientId)).thenReturn(Collections.emptyList());

        pushNotificationService.sendPushForNotification(notifId);

        verify(notificationRepository).updateDeliveryStatus(eq(notifId), eq(NotificationDeliveryStatus.NO_DEVICES), any());
        verifyNoInteractions(pushNotificationProvider);
    }

    @Test
    @DisplayName("Push: When active devices exist and push succeeds, marks status as SENT")
    void testSendPush_Success_MarksSent() {
        UUID notifId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        Notification notif = new Notification(
                recipientId,
                NotificationType.MATCH_CREATED,
                "Title",
                "Body",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                "KEY:2",
                Instant.now()
        );
        notif.setId(notifId);

        UserDeviceToken token = new UserDeviceToken(recipientId, "fcm_token_1", "hash1", DevicePlatform.ANDROID, "FCM");
        token.setId(UUID.randomUUID());

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(userDeviceTokenRepository.findByUserIdAndActiveTrue(recipientId)).thenReturn(List.of(token));
        when(pushNotificationProvider.sendPush(eq("fcm_token_1"), anyString(), anyString(), anyMap()))
                .thenReturn(PushDeliveryResult.success("msg-123"));

        pushNotificationService.sendPushForNotification(notifId);

        verify(notificationRepository).updateDeliveryStatus(eq(notifId), eq(NotificationDeliveryStatus.SENT), any());
        verify(userDeviceTokenRepository).saveAndFlush(token);
        assertTrue(token.isActive());
    }

    @Test
    @DisplayName("Push: When push returns invalid token, revokes token and marks FAILED")
    void testSendPush_InvalidToken_RevokesTokenAndMarksFailed() {
        UUID notifId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        Notification notif = new Notification(
                recipientId,
                NotificationType.MATCH_CREATED,
                "Title",
                "Body",
                null,
                null,
                "KEY:3",
                Instant.now()
        );
        notif.setId(notifId);

        UserDeviceToken token = new UserDeviceToken(recipientId, "invalid_fcm_token", "hash_inv", DevicePlatform.ANDROID, "FCM");
        token.setId(UUID.randomUUID());

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(userDeviceTokenRepository.findByUserIdAndActiveTrue(recipientId)).thenReturn(List.of(token));
        when(pushNotificationProvider.sendPush(eq("invalid_fcm_token"), anyString(), anyString(), anyMap()))
                .thenReturn(PushDeliveryResult.failedInvalidToken("Token unregistered"));

        pushNotificationService.sendPushForNotification(notifId);

        verify(notificationRepository).updateDeliveryStatus(eq(notifId), eq(NotificationDeliveryStatus.FAILED), any());
        assertFalse(token.isActive());
        assertNotNull(token.getRevokedAt());
        verify(userDeviceTokenRepository).saveAndFlush(token);
    }

    @Test
    @DisplayName("Push: When provider throws unexpected exception, fails safely without bubbling")
    void testSendPush_ProviderThrowsException_FailsSafely() {
        UUID notifId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        Notification notif = new Notification(
                recipientId,
                NotificationType.MATCH_CREATED,
                "Title",
                "Body",
                null,
                null,
                "KEY:4",
                Instant.now()
        );
        notif.setId(notifId);

        UserDeviceToken token = new UserDeviceToken(recipientId, "fcm_token_throw", "hash_throw", DevicePlatform.ANDROID, "FCM");
        token.setId(UUID.randomUUID());

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(userDeviceTokenRepository.findByUserIdAndActiveTrue(recipientId)).thenReturn(List.of(token));
        when(pushNotificationProvider.sendPush(anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Simulated network timeout"));

        assertDoesNotThrow(() -> pushNotificationService.sendPushForNotification(notifId));

        verify(notificationRepository).updateDeliveryStatus(eq(notifId), eq(NotificationDeliveryStatus.FAILED), any());
    }
}
