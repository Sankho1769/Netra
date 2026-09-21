package org.netra.features.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.notification.dto.RegisterDeviceTokenRequest;
import org.netra.features.notification.entity.*;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.netra.features.notification.service.NotificationService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private String tokenUserA;

    private User userB;
    private String tokenUserB;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userDeviceTokenRepository.deleteAll();

        userA = new User(
                "User Alpha",
                "alpha_" + UUID.randomUUID() + "@test.org",
                "+9198" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)),
                passwordEncoder.encode("Password123!"),
                Set.of(UserRole.ROLE_DONOR)
        );
        userA = userRepository.save(userA);
        tokenUserA = jwtTokenProvider.generateAccessToken(userA.getId(), List.of("ROLE_DONOR"));

        userB = new User(
                "User Beta",
                "beta_" + UUID.randomUUID() + "@test.org",
                "+9198" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)),
                passwordEncoder.encode("Password123!"),
                Set.of(UserRole.ROLE_RECEIVER)
        );
        userB = userRepository.save(userB);
        tokenUserB = jwtTokenProvider.generateAccessToken(userB.getId(), List.of("ROLE_RECEIVER"));
    }

    @Test
    @DisplayName("Security: Unauthenticated access to /api/v1/notifications returns 401")
    void testUnauthenticated_NotificationsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/notifications/" + UUID.randomUUID() + "/read"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Unauthenticated access to /api/v1/devices/tokens returns 401")
    void testUnauthenticated_DevicesEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/devices/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/devices/tokens/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("IDOR / BOLA: User A cannot mark User B's notification as read -> 403 Forbidden")
    void testIdor_MarkReadForbidden() throws Exception {
        Notification notificationB = notificationService.createNotification(
                userB.getId(),
                NotificationType.MATCH_ACCEPTED,
                "User B Notification",
                "Details for B",
                null,
                null,
                "KEY:B-1"
        );

        // User A attempts to mark User B's notification as read
        mockMvc.perform(patch("/api/v1/notifications/" + notificationB.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());

        // Verify notification remains unread in database
        Notification refreshed = notificationRepository.findById(notificationB.getId()).orElseThrow();
        assertFalse(refreshed.isRead());
    }

    @Test
    @DisplayName("IDOR / Isolation: User A cannot view User B's notifications in list or count")
    void testIdor_NotificationListIsolation() throws Exception {
        notificationService.createNotification(
                userA.getId(),
                NotificationType.MATCH_CREATED,
                "Alpha Notice",
                "Body Alpha",
                null,
                null,
                "KEY:A-1"
        );

        notificationService.createNotification(
                userB.getId(),
                NotificationType.MATCH_ACCEPTED,
                "Beta Notice",
                "Body Beta",
                null,
                null,
                "KEY:B-1"
        );

        // User A list should only contain Alpha Notice
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Alpha Notice")));

        // User A unread count is 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(1)));
    }

    @Test
    @DisplayName("IDOR / BOLA: User A cannot revoke User B's device token -> 403 Forbidden")
    void testIdor_RevokeDeviceTokenForbidden() throws Exception {
        UserDeviceToken tokenB = new UserDeviceToken(
                userB.getId(),
                "fcm_token_beta_device",
                "hash_beta",
                DevicePlatform.ANDROID,
                "FCM"
        );
        tokenB = userDeviceTokenRepository.saveAndFlush(tokenB);

        // User A attempts to revoke User B's token
        mockMvc.perform(delete("/api/v1/devices/tokens/" + tokenB.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());

        // Token remains active
        UserDeviceToken refreshed = userDeviceTokenRepository.findById(tokenB.getId()).orElseThrow();
        assertTrue(refreshed.isActive());
    }

    @Test
    @DisplayName("Privacy: Notification payload contains zero sensitive medical/questionnaire data")
    void testPrivacy_NoSensitiveDataInNotification() {
        Notification n = notificationService.createNotification(
                userA.getId(),
                NotificationType.MATCH_CREATED,
                "New Blood Donation Match Request",
                "You have been matched with an urgent blood request. Please review details in the app to accept or decline.",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                "KEY:privacy-1"
        );

        String fullText = (n.getTitle() + " " + n.getBody()).toLowerCase();
        assertFalse(fullText.contains("hiv"));
        assertFalse(fullText.contains("hepatitis"));
        assertFalse(fullText.contains("questionnaire"));
        assertFalse(fullText.contains("screening"));
        assertFalse(fullText.contains("latitude"));
        assertFalse(fullText.contains("longitude"));
        assertFalse(fullText.contains("password"));
        assertFalse(fullText.contains("token"));
    }
}
