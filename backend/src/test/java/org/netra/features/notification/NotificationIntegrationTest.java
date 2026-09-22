package org.netra.features.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.donor.entity.*;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.matching.dto.CreateDonorMatchRequest;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.matching.service.DonorMatchLifecycleService;
import org.netra.features.matching.service.DonorResponseService;
import org.netra.features.notification.dto.RegisterDeviceTokenRequest;
import org.netra.features.notification.entity.*;
import org.netra.features.notification.event.MatchExpiredEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationIntegrationTest {

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
    private DonorMatchRepository donorMatchRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorResponseService donorResponseService;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private DonorMatchLifecycleService donorMatchLifecycleService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User requester;
    private String requesterToken;

    private User donorUser;
    private DonorProfile donorProfile;
    private String donorToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        userDeviceTokenRepository.deleteAllInBatch();
        donorMatchRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();

        // Requester setup
        requester = createTestUser("Requester", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        requesterToken = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        // Donor setup
        donorUser = createTestUser("Donor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        donorToken = jwtTokenProvider.generateAccessToken(donorUser.getId(), List.of("ROLE_DONOR"));

        donorProfile = new DonorProfile();
        donorProfile.setUserId(donorUser.getId());
        donorProfile.setBloodGroup(BloodGroup.O_POSITIVE);
        donorProfile.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        donorProfile.setAvailabilityStatus(DonorAvailabilityStatus.AVAILABLE);
        donorProfile.setLatitude(18.9400);
        donorProfile.setLongitude(72.8350);
        donorProfile = donorProfileRepository.save(donorProfile);
    }

    private User createTestUser(String name, UserRole role, UserStatus status) {
        User u = new User(
                name + " User",
                name.toLowerCase() + "_" + UUID.randomUUID() + "@test.org",
                "+9198" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)),
                passwordEncoder.encode("Password123!"),
                Set.of(role)
        );
        u.setStatus(status);
        return userRepository.save(u);
    }

    private BloodRequest createOpenBloodRequest() {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.URGENT);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City General Hospital");
        req.setHospitalAddress("Fort, Mumbai");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9400);
        req.setLongitude(72.8350);
        req.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        return bloodRequestRepository.save(req);
    }

    @Test
    @DisplayName("Flow: Match creation automatically creates MATCH_CREATED notification for donor")
    void testMatchCreated_TriggersNotification() {
        BloodRequest req = createOpenBloodRequest();

        CreateDonorMatchRequest matchReq = new CreateDonorMatchRequest(donorProfile.getId());
        donorResponseService.createMatch(req.getId(), matchReq, requester.getId(), "127.0.0.1", "TestAgent");

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification n = notifications.get(0);
        assertEquals(donorUser.getId(), n.getRecipientUserId());
        assertEquals(NotificationType.MATCH_CREATED, n.getType());
        assertEquals(NotificationReferenceType.DONOR_MATCH, n.getReferenceType());
        assertFalse(n.isRead());
    }

    @Test
    @DisplayName("Flow: Match acceptance automatically creates MATCH_ACCEPTED notification for requester")
    void testMatchAccepted_TriggersNotification() {
        BloodRequest req = createOpenBloodRequest();

        CreateDonorMatchRequest matchReq = new CreateDonorMatchRequest(donorProfile.getId());
        var matchDto = donorResponseService.createMatch(req.getId(), matchReq, requester.getId(), "127.0.0.1", "TestAgent");

        // Accept match as donor
        donorResponseService.acceptMatch(matchDto.getMatchId(), donorUser.getId(), "127.0.0.1", "TestAgent");

        List<Notification> requesterNotifications = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(requester.getId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertEquals(1, requesterNotifications.size());
        Notification n = requesterNotifications.get(0);
        assertEquals(NotificationType.MATCH_ACCEPTED, n.getType());
        assertEquals(matchDto.getMatchId(), n.getReferenceId());
    }

    @Test
    @DisplayName("Flow: Match decline automatically creates MATCH_DECLINED notification for requester")
    void testMatchDeclined_TriggersNotification() {
        BloodRequest req = createOpenBloodRequest();

        CreateDonorMatchRequest matchReq = new CreateDonorMatchRequest(donorProfile.getId());
        var matchDto = donorResponseService.createMatch(req.getId(), matchReq, requester.getId(), "127.0.0.1", "TestAgent");

        // Decline match as donor
        donorResponseService.declineMatch(matchDto.getMatchId(), donorUser.getId(), "127.0.0.1", "TestAgent");

        List<Notification> requesterNotifications = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(requester.getId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertEquals(1, requesterNotifications.size());
        Notification n = requesterNotifications.get(0);
        assertEquals(NotificationType.MATCH_DECLINED, n.getType());
    }

    @Test
    @DisplayName("Flow: Match expiration event creates MATCH_EXPIRED notification for donor")
    void testMatchExpired_TriggersNotification() {
        BloodRequest req = createOpenBloodRequest();

        DonorMatch match = new DonorMatch(req.getId(), donorUser.getId(), Instant.now().minusSeconds(10));
        donorMatchRepository.saveAndFlush(match);

        // Publish expired event
        eventPublisher.publishEvent(new MatchExpiredEvent(match.getId(), req.getId(), donorUser.getId(), requester.getId()));

        List<Notification> donorNotifications = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(donorUser.getId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertEquals(1, donorNotifications.size());
        assertEquals(NotificationType.MATCH_EXPIRED, donorNotifications.get(0).getType());
    }

    @Test
    @DisplayName("Flow: Request cancellation automatically creates BLOOD_REQUEST_CANCELLED notification for matched donors")
    void testBloodRequestCancelled_TriggersNotificationForMatchedDonors() {
        BloodRequest req = createOpenBloodRequest();

        CreateDonorMatchRequest matchReq = new CreateDonorMatchRequest(donorProfile.getId());
        donorResponseService.createMatch(req.getId(), matchReq, requester.getId(), "127.0.0.1", "TestAgent");

        // Cancel the blood request matches as requester
        donorMatchLifecycleService.cancelActiveMatchesForRequest(req.getId(), requester.getId(), "127.0.0.1", "TestAgent");

        List<Notification> donorNotifications = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(donorUser.getId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        // 1 MATCH_CREATED, 1 BLOOD_REQUEST_CANCELLED
        assertEquals(2, donorNotifications.size());
        assertTrue(donorNotifications.stream().anyMatch(n -> n.getType() == NotificationType.BLOOD_REQUEST_CANCELLED));
    }

    @Test
    @DisplayName("API: GET /api/v1/notifications returns user's notifications and unread count")
    void testGetNotificationsAndUnreadCount() throws Exception {
        notificationService.createNotification(
                donorUser.getId(),
                NotificationType.MATCH_CREATED,
                "Test Title",
                "Test Body",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                "KEY:test-1"
        );

        // Check unread count
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(1)));

        // Check list
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Test Title")))
                .andExpect(jsonPath("$.content[0].read", is(false)));
    }

    @Test
    @DisplayName("API: PATCH /api/v1/notifications/{id}/read marks notification as read")
    void testMarkAsReadApi() throws Exception {
        Notification n = notificationService.createNotification(
                donorUser.getId(),
                NotificationType.MATCH_CREATED,
                "Test Title",
                "Test Body",
                NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(),
                "KEY:read-1"
        );

        mockMvc.perform(patch("/api/v1/notifications/" + n.getId() + "/read")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(n.getId().toString())))
                .andExpect(jsonPath("$.read", is(true)))
                .andExpect(jsonPath("$.readAt", notNullValue()));

        // Unread count is now 0
        assertEquals(0L, notificationService.getUnreadCount(donorUser.getId()).getUnreadCount());
    }

    @Test
    @DisplayName("API: PATCH /api/v1/notifications/read-all marks all notifications as read")
    void testMarkAllAsReadApi() throws Exception {
        notificationService.createNotification(donorUser.getId(), NotificationType.MATCH_CREATED, "T1", "B1", null, null, "KEY:1");
        notificationService.createNotification(donorUser.getId(), NotificationType.MATCH_CREATED, "T2", "B2", null, null, "KEY:2");

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount", is(2)));

        assertEquals(0L, notificationService.getUnreadCount(donorUser.getId()).getUnreadCount());
    }

    @Test
    @DisplayName("API: POST and DELETE /api/v1/devices/tokens registers and revokes device token")
    void testDeviceTokenApi() throws Exception {
        RegisterDeviceTokenRequest req = new RegisterDeviceTokenRequest("device_fcm_token_12345", DevicePlatform.ANDROID, "FCM");

        String response = mockMvc.perform(post("/api/v1/devices/tokens")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.platform", is("ANDROID")))
                .andExpect(jsonPath("$.active", is(true)))
                .andReturn().getResponse().getContentAsString();

        UUID tokenId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        // Revoke token
        mockMvc.perform(delete("/api/v1/devices/tokens/" + tokenId)
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isNoContent());

        UserDeviceToken token = userDeviceTokenRepository.findById(tokenId).orElseThrow();
        assertFalse(token.isActive());
        assertNotNull(token.getRevokedAt());
    }

    @Test
    @DisplayName("API: POST /api/v1/devices/tokens rejects unsupported non-FCM provider")
    void testDeviceTokenApi_RejectsNonFcmProvider() throws Exception {
        RegisterDeviceTokenRequest req = new RegisterDeviceTokenRequest("device_token_xyz", DevicePlatform.ANDROID, "APNS");

        mockMvc.perform(post("/api/v1/devices/tokens")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
