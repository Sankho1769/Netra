package org.netra.features.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.auth.dto.LoginRequest;
import org.netra.features.bloodbank.dto.UpdateInventoryRequest;
import org.netra.features.bloodbank.entity.*;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.bloodrequest.dto.CreateBloodRequestRequest;
import org.netra.features.bloodrequest.dto.UpdateBloodRequestRequest;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.dto.AdminCorrectionRequest;
import org.netra.features.donation.dto.VerifyDonationRequest;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.entity.DonationEventType;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.NotificationReferenceType;
import org.netra.features.notification.entity.NotificationType;
import org.netra.features.notification.repository.NotificationRepository;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private FulfillmentRepository fulfillmentRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

    @Autowired
    private DonationEventRepository donationEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private org.netra.features.user.repository.RefreshSessionRepository refreshSessionRepository;

    private User donorA;
    private User donorB;
    private User bankStaffA;
    private User bankStaffB;
    private User adminUser;

    private BloodBank bankA;
    private BloodBank bankB;

    @BeforeEach
    void setUp() {
        donorA = createUser("Donor A", "donor.a." + UUID.randomUUID() + "@netra.org", Set.of(UserRole.ROLE_DONOR));
        donorB = createUser("Donor B", "donor.b." + UUID.randomUUID() + "@netra.org", Set.of(UserRole.ROLE_DONOR));
        bankStaffA = createUser("Staff A", "staff.a." + UUID.randomUUID() + "@netra.org", Set.of(UserRole.ROLE_BLOODBANK));
        bankStaffB = createUser("Staff B", "staff.b." + UUID.randomUUID() + "@netra.org", Set.of(UserRole.ROLE_BLOODBANK));
        adminUser = createUser("Admin", "admin." + UUID.randomUUID() + "@netra.org", Set.of(UserRole.ROLE_ADMIN));

        bankA = createBloodBank("City Hospital Blood Center", "BB-A-" + UUID.randomUUID());
        bankB = createBloodBank("Metro Care Blood Bank", "BB-B-" + UUID.randomUUID());

        linkStaffToBank(bankStaffA.getId(), bankA.getId(), BloodBankAccountStatus.ACTIVE);
        linkStaffToBank(bankStaffB.getId(), bankB.getId(), BloodBankAccountStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        fulfillmentRepository.deleteAll();
        donationRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        notificationRepository.deleteAll();
        donationEventRepository.deleteAll();
        bloodBankAccountRepository.deleteAll();
        bloodBankRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String tokenFor(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), roleNames);
    }

    private User createUser(String name, String email, Set<UserRole> roles) {
        User user = new User(name, email.toLowerCase(), "+919876543210", passwordEncoder.encode("SecurePass123"), roles);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private BloodBank createBloodBank(String name, String regNo) {
        BloodBank bank = new BloodBank(name, regNo, "123 Healthcare Ave", "Mumbai", "Maharashtra", "400001",
                19.0760, 72.8777, "+912212345678", name.toLowerCase().replace(" ", "") + "@test.org",
                BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN);
        return bloodBankRepository.save(bank);
    }

    private void linkStaffToBank(UUID userId, UUID bloodBankId, BloodBankAccountStatus status) {
        BloodBankAccount account = new BloodBankAccount(userId, bloodBankId, status);
        bloodBankAccountRepository.save(account);
    }

    // =========================================================================
    // 1. AUTHENTICATION SECURITY
    // =========================================================================

    @Test
    @DisplayName("Auth Security: Oversized login password (> 128 chars) is rejected with 400 Bad Request to prevent BCrypt DoS")
    void testAuthentication_OversizedPassword_RejectedAtValidation() throws Exception {
        String longPassword = "A".repeat(129);
        LoginRequest request = new LoginRequest("test@netra.org", longPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Auth Security: Malformed JWT token returns 401 Unauthorized without stack trace")
    void testAuthentication_MalformedToken_Returns401WithoutStacktrace() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at org."))));
    }

    // =========================================================================
    // 2. AUTHORIZATION / IDOR / BOLA PROTECTION
    // =========================================================================

    @Test
    @DisplayName("IDOR Security: User cannot access another user's profile via /api/v1/users/{otherId}")
    void testIdor_UserProfileAccess_RejectedForOtherUser() throws Exception {
        // Donor A tries to read Donor B's profile
        mockMvc.perform(get("/api/v1/users/" + donorB.getId())
                        .header("Authorization", tokenFor(donorA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        // Donor A can read their own profile
        mockMvc.perform(get("/api/v1/users/" + donorA.getId())
                        .header("Authorization", tokenFor(donorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(donorA.getId().toString())));

        // Admin can read Donor B's profile
        mockMvc.perform(get("/api/v1/users/" + donorB.getId())
                        .header("Authorization", tokenFor(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(donorB.getId().toString())));
    }

    private BloodRequest createBloodRequest(UUID requesterId, String hospitalName) {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requesterId);
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
        req.setUnitsFulfilled(0);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setHospitalName(hospitalName);
        req.setHospitalAddress("Street 1");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(19.0760);
        req.setLongitude(72.8777);
        req.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        req.setDescription("Urgent need");
        return bloodRequestRepository.save(req);
    }

    @Test
    @DisplayName("IDOR Security: User cannot modify or cancel another user's blood request")
    void testIdor_BloodRequestModification_RejectedForNonOwner() throws Exception {
        BloodRequest req = createBloodRequest(donorA.getId(), "City Clinic");

        // Donor B tries to update Donor A's request
        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setHospitalName("Hacked Hospital");

        mockMvc.perform(patch("/api/v1/blood-requests/" + req.getId())
                        .header("Authorization", tokenFor(donorB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        // Donor B tries to cancel Donor A's request
        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Cancelled by intruder");
        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/cancel")
                        .header("Authorization", tokenFor(donorB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("IDOR Security: User cannot mark another user's notification as read")
    void testIdor_NotificationMarkAsRead_RejectedForOtherUser() throws Exception {
        Notification notif = new Notification(donorA.getId(), NotificationType.MATCH_CREATED,
                "Match Available", "A new match is ready", NotificationReferenceType.DONOR_MATCH,
                UUID.randomUUID(), "sec-test-idem-" + UUID.randomUUID(), Instant.now());
        notif = notificationRepository.save(notif);

        // Donor B tries to mark Donor A's notification as read
        mockMvc.perform(patch("/api/v1/notifications/" + notif.getId() + "/read")
                        .header("Authorization", tokenFor(donorB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("IDOR Security: Unrelated user cannot view another user's fulfillment record")
    void testIdor_FulfillmentAccess_RejectedForUnrelatedUser() throws Exception {
        BloodRequest req = createBloodRequest(donorA.getId(), "Central Hospital");

        Donation donation = new Donation(donorA.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null,
                LocalDate.now(), "Fulfillment donation");
        donation.verify(bankStaffA.getId(), Instant.now(), "Verified");
        donation = donationRepository.save(donation);

        Fulfillment fulfillment = new Fulfillment(req.getId(), donation.getId(), 1, bankStaffA.getId(), "Test notes");
        fulfillment = fulfillmentRepository.save(fulfillment);

        // Donor B (unrelated to request, donation, or fulfillment) attempts to view fulfillment
        mockMvc.perform(get("/api/v1/fulfillments/" + fulfillment.getId())
                        .header("Authorization", tokenFor(donorB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    // =========================================================================
    // 3. ROLE / PRIVILEGE ESCALATION
    // =========================================================================

    @Test
    @DisplayName("Privilege Escalation: Donor cannot perform clinical verification on a donation")
    void testRoleEscalation_DonorCannotVerifyDonation() throws Exception {
        BloodRequest req = createBloodRequest(donorA.getId(), "Hospital");
        Donation donation = new Donation(donorA.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null,
                LocalDate.now(), "Claim notes");
        donation = donationRepository.save(donation);

        VerifyDonationRequest verifyReq = new VerifyDonationRequest("Attempted self-verification");

        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/verify")
                        .header("Authorization", tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Privilege Escalation: Donor cannot start a clinical fulfillment")
    void testRoleEscalation_DonorCannotStartFulfillment() throws Exception {
        BloodRequest req = createBloodRequest(donorA.getId(), "Hospital");
        Donation donation = new Donation(donorA.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null,
                LocalDate.now(), "Claim notes");
        donation.verify(bankStaffA.getId(), Instant.now(), "Verified");
        donation = donationRepository.save(donation);

        Fulfillment fulfillment = new Fulfillment(req.getId(), donation.getId(), 1, bankStaffA.getId(), "Notes");
        fulfillment = fulfillmentRepository.save(fulfillment);

        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/start")
                        .header("Authorization", tokenFor(donorA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Privilege Escalation: Non-admin (staff or donor) cannot call admin correction endpoint")
    void testRoleEscalation_NonAdminCannotCallAdminCorrection() throws Exception {
        BloodRequest req = createBloodRequest(donorA.getId(), "Hospital");
        Donation donation = new Donation(donorA.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null,
                LocalDate.now(), "Claim notes");
        donation.reject(bankStaffA.getId(), Instant.now(), "Rejected");
        donation = donationRepository.save(donation);

        AdminCorrectionRequest correctionReq = new AdminCorrectionRequest(DonationVerificationStatus.VERIFIED, "Illegal correction");

        // Staff tries admin correction
        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/admin-correction")
                        .header("Authorization", tokenFor(bankStaffA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctionReq)))
                .andExpect(status().isForbidden());

        // Donor tries admin correction
        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/admin-correction")
                        .header("Authorization", tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctionReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-Tenant Scoping: Staff from Blood Bank B cannot verify donations for Bank A's event")
    void testCrossTenant_StaffCannotVerifyOtherBanksEventDonation() throws Exception {
        DonationEvent eventA = new DonationEvent();
        eventA.setBloodBankId(bankA.getId());
        eventA.setTitle("Blood Camp A");
        eventA.setDescription("Description");
        eventA.setEventType(DonationEventType.BLOOD_DONATION_CAMP);
        eventA.setStartAt(Instant.now().plus(1, ChronoUnit.DAYS));
        eventA.setEndAt(Instant.now().plus(2, ChronoUnit.DAYS));
        eventA.setRegistrationOpenAt(Instant.now().minus(1, ChronoUnit.DAYS));
        eventA.setRegistrationCloseAt(Instant.now().plus(1, ChronoUnit.DAYS));
        eventA.setDonorCapacity(100);
        eventA.setVenueName("Camp Grounds");
        eventA.setAddress("Camp Road");
        eventA.setCity("Mumbai");
        eventA.setState("Maharashtra");
        eventA.setPostalCode("400001");
        eventA.setLatitude(19.0760);
        eventA.setLongitude(72.8777);
        eventA.setCreatedBy(bankStaffA.getId());
        eventA.setStatus(DonationEventStatus.PUBLISHED);
        eventA = donationEventRepository.save(eventA);

        Donation eventDonation = new Donation(donorA.getId(), DonationSourceType.DONATION_EVENT, null, eventA.getId(),
                LocalDate.now(), "Event donation");
        eventDonation = donationRepository.save(eventDonation);

        VerifyDonationRequest verifyReq = new VerifyDonationRequest("Verified by wrong bank staff");

        // Bank Staff B (from Bank B) attempts to verify Bank A's donation
        mockMvc.perform(post("/api/v1/donations/" + eventDonation.getId() + "/verify")
                        .header("Authorization", tokenFor(bankStaffB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    // =========================================================================
    // 4. INPUT VALIDATION & ABUSE PROTECTION
    // =========================================================================

    @Test
    @DisplayName("Input Validation: Negative and excessively large inventory units are rejected with 400 Bad Request")
    void testInputValidation_NegativeOrExcessiveInventory_Rejected() throws Exception {
        // Negative inventory units
        UpdateInventoryRequest negativeReq = new UpdateInventoryRequest(BloodGroup.O_POSITIVE, -5);
        mockMvc.perform(put("/api/v1/bloodbanks/" + bankA.getId() + "/inventory")
                        .header("Authorization", tokenFor(bankStaffA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));

        // Excessively large inventory units (> 10000)
        UpdateInventoryRequest excessiveReq = new UpdateInventoryRequest(BloodGroup.O_POSITIVE, 50000);
        mockMvc.perform(put("/api/v1/bloodbanks/" + bankA.getId() + "/inventory")
                        .header("Authorization", tokenFor(bankStaffA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Input Validation: Malformed UUID in path variable returns 400 Bad Request without leaking internal stack traces")
    void testInputValidation_MalformedUuidInPath_Returns400NonLeaking() throws Exception {
        mockMvc.perform(get("/api/v1/blood-requests/not-a-valid-uuid")
                        .header("Authorization", tokenFor(donorA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_ARGUMENT")))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at org."))));
    }

    @Test
    @DisplayName("Input Validation: Invalid latitude/longitude bounds in blood request return 400 Bad Request")
    void testInputValidation_InvalidCoordinates_Returns400() throws Exception {
        CreateBloodRequestRequest request = new CreateBloodRequestRequest();
        request.setBloodGroup(BloodGroup.A_POSITIVE);
        request.setUnitsRequired(2);
        request.setHospitalName("City General");
        request.setHospitalAddress("Park Road");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setPostalCode("400001");
        request.setLatitude(120.0); // Invalid: latitude must be <= 90.0
        request.setLongitude(72.8777);
        request.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    // =========================================================================
    // 5. API SECURITY & HTTP HEADERS
    // =========================================================================

    @Test
    @DisplayName("API Security: Responses contain Content-Security-Policy, X-Frame-Options DENY, and Referrer-Policy")
    void testSecurityHeaders_FrameOptionsCspReferrerPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/blood-requests"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    // =========================================================================
    // 6. SENSITIVE DATA EXPOSURE
    // =========================================================================

    @Test
    @DisplayName("Sensitive Data Protection: Public blood request detail omits requester ID and exact coordinates")
    void testSensitiveData_BloodRequestPublicDetail_OmitPrivateCoordinatesAndRequester() throws Exception {
        BloodRequest req = createBloodRequest(donorA.getId(), "City Hospital");

        // Anonymous or non-owner viewer
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalName", is("City Hospital")))
                .andExpect(jsonPath("$.city", is("Mumbai")))
                .andExpect(jsonPath("$.requesterUserId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
    }
}
