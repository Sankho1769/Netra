package org.netra.features.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.features.notification.dto.DeviceTokenDto;
import org.netra.features.notification.dto.NotificationDto;
import org.netra.features.notification.dto.RegisterDeviceTokenRequest;
import org.netra.features.notification.entity.*;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.netra.features.notification.service.NotificationService;
import org.netra.features.notification.service.PushNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userDeviceTokenRepository,
                pushNotificationService
        );
    }

    @Test
    @DisplayName("Create: Persists notification and triggers push delivery")
    void testCreateNotification_Success() {
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();

        Notification saved = new Notification(
                userId,
                NotificationType.MATCH_CREATED,
                "Match Created",
                "You have a match",
                NotificationReferenceType.DONOR_MATCH,
                refId,
                "KEY:123",
                Instant.now()
        );
        saved.setId(UUID.randomUUID());

        when(notificationRepository.findByIdempotencyKey("KEY:123")).thenReturn(Optional.empty());
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(saved);

        Notification result = notificationService.createNotification(
                userId,
                NotificationType.MATCH_CREATED,
                "Match Created",
                "You have a match",
                NotificationReferenceType.DONOR_MATCH,
                refId,
                "KEY:123"
        );

        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
        assertEquals(NotificationType.MATCH_CREATED, result.getType());
        verify(notificationRepository).saveAndFlush(any(Notification.class));
        verify(pushNotificationService).sendPushForNotification(saved.getId());
    }

    @Test
    @DisplayName("Create: Suppresses duplicate notification when idempotency key exists")
    void testCreateNotification_IdempotencySuppressed() {
        UUID userId = UUID.randomUUID();
        Notification existing = new Notification(
                userId,
                NotificationType.MATCH_CREATED,
                "Match Created",
                "You have a match",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                "KEY:123",
                Instant.now()
        );
        existing.setId(UUID.randomUUID());

        when(notificationRepository.findByIdempotencyKey("KEY:123")).thenReturn(Optional.of(existing));

        Notification result = notificationService.createNotification(
                userId,
                NotificationType.MATCH_CREATED,
                "Match Created",
                "You have a match",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                "KEY:123"
        );

        assertNotNull(result);
        assertEquals(existing.getId(), result.getId());
        verify(notificationRepository, never()).saveAndFlush(any());
        verify(pushNotificationService, never()).sendPushForNotification(any());
    }

    @Test
    @DisplayName("Create: Rejects invalid inputs")
    void testCreateNotification_ValidationErrors() {
        assertThrows(ValidationException.class, () ->
                notificationService.createNotification(null, NotificationType.MATCH_CREATED, "Title", "Body", null, null, null));
        assertThrows(ValidationException.class, () ->
                notificationService.createNotification(UUID.randomUUID(), null, "Title", "Body", null, null, null));
        assertThrows(ValidationException.class, () ->
                notificationService.createNotification(UUID.randomUUID(), NotificationType.MATCH_CREATED, "  ", "Body", null, null, null));
        assertThrows(ValidationException.class, () ->
                notificationService.createNotification(UUID.randomUUID(), NotificationType.MATCH_CREATED, "Title", "", null, null, null));
    }

    @Test
    @DisplayName("Query: Retrieves paginated notifications strictly for authenticated user")
    void testGetNotifications_Success() {
        UUID currentUserId = UUID.randomUUID();
        Notification n = new Notification(
                currentUserId,
                NotificationType.MATCH_ACCEPTED,
                "Donor Accepted",
                "Match details available",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                null,
                Instant.now()
        );
        n.setId(UUID.randomUUID());

        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(eq(currentUserId), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationDto> result = notificationService.getNotifications(currentUserId, false, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Donor Accepted", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Unread Count: Returns accurate counter for user")
    void testGetUnreadCount() {
        UUID currentUserId = UUID.randomUUID();
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(currentUserId)).thenReturn(5L);

        assertEquals(5L, notificationService.getUnreadCount(currentUserId).getUnreadCount());
    }

    @Test
    @DisplayName("Mark Read: Successfully marks unread notification as read")
    void testMarkAsRead_Success() {
        UUID currentUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        Notification n = new Notification(
                currentUserId,
                NotificationType.MATCH_CREATED,
                "Title",
                "Body",
                null,
                null,
                null,
                Instant.now()
        );
        n.setId(notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDto dto = notificationService.markAsRead(notificationId, currentUserId);

        assertTrue(dto.isRead());
        assertNotNull(dto.getReadAt());
        verify(notificationRepository).save(n);
    }

    @Test
    @DisplayName("Mark Read IDOR: Rejects marking another user's notification as read")
    void testMarkAsRead_IdorForbidden() {
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        Notification n = new Notification(
                otherUserId, // Belongs to other user
                NotificationType.MATCH_CREATED,
                "Title",
                "Body",
                null,
                null,
                null,
                Instant.now()
        );
        n.setId(notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(n));

        assertThrows(UnauthorizedSessionAccessException.class, () ->
                notificationService.markAsRead(notificationId, currentUserId));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mark Read: Throws 404 when notification does not exist")
    void testMarkAsRead_NotFound() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                notificationService.markAsRead(notificationId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Mark All Read: Marks all user's notifications as read")
    void testMarkAllAsRead() {
        UUID currentUserId = UUID.randomUUID();
        when(notificationRepository.markAllAsRead(eq(currentUserId), any(Instant.class))).thenReturn(3);

        assertEquals(3, notificationService.markAllAsRead(currentUserId).getUpdatedCount());
    }

    @Test
    @DisplayName("Device Token: Successfully registers new token")
    void testRegisterDeviceToken_New() {
        UUID currentUserId = UUID.randomUUID();
        RegisterDeviceTokenRequest req = new RegisterDeviceTokenRequest("device_token_xyz", DevicePlatform.ANDROID, "FCM");

        when(userDeviceTokenRepository.findByToken("device_token_xyz")).thenReturn(Optional.empty());
        when(userDeviceTokenRepository.saveAndFlush(any(UserDeviceToken.class))).thenAnswer(inv -> {
            UserDeviceToken dt = inv.getArgument(0);
            dt.setId(UUID.randomUUID());
            return dt;
        });

        DeviceTokenDto dto = notificationService.registerDeviceToken(currentUserId, req);

        assertNotNull(dto);
        assertEquals(DevicePlatform.ANDROID, dto.getPlatform());
        assertTrue(dto.isActive());
    }

    @Test
    @DisplayName("Device Token: Safely reassigns existing token to current user")
    void testRegisterDeviceToken_Reassign() {
        UUID currentUserId = UUID.randomUUID();
        UUID previousUserId = UUID.randomUUID();
        RegisterDeviceTokenRequest req = new RegisterDeviceTokenRequest("device_token_xyz", DevicePlatform.IOS, "FCM");

        UserDeviceToken existing = new UserDeviceToken(previousUserId, "device_token_xyz", "hash123", DevicePlatform.ANDROID, "FCM");
        when(userDeviceTokenRepository.findByToken("device_token_xyz")).thenReturn(Optional.of(existing));
        when(userDeviceTokenRepository.saveAndFlush(any(UserDeviceToken.class))).thenReturn(existing);

        DeviceTokenDto dto = notificationService.registerDeviceToken(currentUserId, req);

        assertEquals(currentUserId, existing.getUserId());
        assertEquals(DevicePlatform.IOS, existing.getPlatform());
        assertTrue(existing.isActive());
    }

    @Test
    @DisplayName("Device Token: Revokes device token belonging to current user")
    void testRevokeDeviceToken_Success() {
        UUID currentUserId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        UserDeviceToken token = new UserDeviceToken(currentUserId, "token_123", "hash", DevicePlatform.ANDROID, "FCM");
        token.setId(tokenId);

        when(userDeviceTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        notificationService.revokeDeviceToken(tokenId, currentUserId);

        assertFalse(token.isActive());
        assertNotNull(token.getRevokedAt());
        verify(userDeviceTokenRepository).save(token);
    }

    @Test
    @DisplayName("Device Token IDOR: Rejects revoking another user's device token")
    void testRevokeDeviceToken_IdorForbidden() {
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        UserDeviceToken token = new UserDeviceToken(otherUserId, "token_123", "hash", DevicePlatform.ANDROID, "FCM");
        token.setId(tokenId);

        when(userDeviceTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertThrows(UnauthorizedSessionAccessException.class, () ->
                notificationService.revokeDeviceToken(tokenId, currentUserId));
    }
}
