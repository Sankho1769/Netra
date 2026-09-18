package org.netra.features.donor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.donor.dto.CreateDonorProfileRequest;
import org.netra.features.donor.dto.UpdateDonorProfileRequest;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.entity.DonorStatus;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.user.dto.UpdateUserProfileRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DonorProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User createTestUser(String namePrefix, UserStatus status, Set<UserRole> roles) {
        String email = namePrefix.toLowerCase() + "." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                namePrefix + " User",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                roles
        );
        user.setStatus(status);
        return userRepository.save(user);
    }

    private String getAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getRoles().stream().map(Enum::name).toList()
        );
    }

    @Test
    @DisplayName("Profile 1 & 2: GET and UPDATE own user profile")
    void testGetAndUpdateOwnUserProfile() throws Exception {
        User user = createTestUser("UserOne", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // 1. GET own profile
        mockMvc.perform(get("/api/v1/profile/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().toString())))
                .andExpect(jsonPath("$.fullName", is("UserOne User")))
                .andExpect(jsonPath("$.email", is(user.getEmail())))
                .andExpect(jsonPath("$.phone", is("+919876543210")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_DONOR")));

        // 2. UPDATE own profile
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest("UserOne Updated", "+919999988888");
        mockMvc.perform(put("/api/v1/profile/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("UserOne Updated")))
                .andExpect(jsonPath("$.phone", is("+919999988888")));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("UserOne Updated", updated.getFullName());
        assertEquals("+919999988888", updated.getPhone());
    }

    @Test
    @DisplayName("Profile 3, 4, 5, 6: Mass assignment protection - client cannot modify email, roles, status or id")
    void testUserProfileMassAssignmentBlocked() throws Exception {
        User user = createTestUser("MassAssign", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);
        String originalEmail = user.getEmail();

        // Forged payload attempting to alter id, email, status, role, passwordHash
        String forgedPayload = "{"
                + "\"fullName\":\"Legit Name\","
                + "\"phone\":\"+919876543210\","
                + "\"id\":\"" + UUID.randomUUID() + "\","
                + "\"email\":\"hacker@evil.com\","
                + "\"status\":\"SUSPENDED\","
                + "\"role\":\"ROLE_ADMIN\","
                + "\"roles\":[\"ROLE_ADMIN\"],"
                + "\"passwordHash\":\"fakeHash\""
                + "}";

        mockMvc.perform(put("/api/v1/profile/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgedPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Legit Name")))
                .andExpect(jsonPath("$.email", is(originalEmail)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.roles", not(hasItem("ROLE_ADMIN"))));

        User refetched = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(originalEmail, refetched.getEmail(), "Email must remain read-only");
        assertEquals(UserStatus.ACTIVE, refetched.getStatus(), "Status must not be modified by user");
        assertTrue(refetched.getRoles().contains(UserRole.ROLE_DONOR));
        assertFalse(refetched.getRoles().contains(UserRole.ROLE_ADMIN), "Roles must not be modified by user");
    }

    @Test
    @DisplayName("Donor 7 & 8: GET 404 when not created, then POST creates donor profile with SELF_REPORTED status")
    void testGetNotFoundThenCreateDonorProfile() throws Exception {
        User user = createTestUser("DonorNew", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // 1. GET returns 404
        mockMvc.perform(get("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // 2. POST create
        CreateDonorProfileRequest request = new CreateDonorProfileRequest(BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.bloodGroup", is("O+")))
                .andExpect(jsonPath("$.bloodGroupVerificationStatus", is("SELF_REPORTED")))
                .andExpect(jsonPath("$.availabilityStatus", is("AVAILABLE")))
                .andExpect(jsonPath("$.donorStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.lastDonationDate", nullValue()))
                // Confirm userId is not in JSON (DTO minimization)
                .andExpect(jsonPath("$.userId").doesNotExist());

        // 3. GET now returns 200
        mockMvc.perform(get("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroup", is("O+")))
                .andExpect(jsonPath("$.bloodGroupVerificationStatus", is("SELF_REPORTED")));
    }

    @Test
    @DisplayName("Donor 9: Duplicate donor profile creation returns 409 Conflict")
    void testDuplicateDonorProfileReturns409Conflict() throws Exception {
        User user = createTestUser("DonorDup", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        CreateDonorProfileRequest request = new CreateDonorProfileRequest(BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt second creation
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Donor 10 & 11: Update blood group and availability status")
    void testUpdateBloodGroupAndAvailability() throws Exception {
        User user = createTestUser("DonorUpdate", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // Initial creation with B+
        CreateDonorProfileRequest createReq = new CreateDonorProfileRequest(BloodGroup.B_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Update to AB- and PAUSED
        UpdateDonorProfileRequest updateReq = new UpdateDonorProfileRequest(BloodGroup.AB_NEGATIVE, DonorAvailabilityStatus.PAUSED);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroup", is("AB-")))
                .andExpect(jsonPath("$.availabilityStatus", is("PAUSED")));

        DonorProfile profile = donorProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertEquals(BloodGroup.AB_NEGATIVE, profile.getBloodGroup());
        assertEquals(DonorAvailabilityStatus.PAUSED, profile.getAvailabilityStatus());
    }

    @Test
    @DisplayName("Donor 12 & 13: Invalid blood group and availability rejected with 400 Bad Request")
    void testInvalidBloodGroupAndAvailabilityRejected() throws Exception {
        User user = createTestUser("DonorInvalid", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // 1. Invalid blood group in POST
        String invalidBloodPayload = "{\"bloodGroup\":\"orange-positive\",\"availabilityStatus\":\"AVAILABLE\"}";
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBloodPayload))
                .andExpect(status().isBadRequest());

        // 2. Create valid profile
        CreateDonorProfileRequest validReq = new CreateDonorProfileRequest(BloodGroup.O_NEGATIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated());

        // 3. Invalid availability in PUT
        String invalidAvailPayload = "{\"availabilityStatus\":\"SUPER_READY\"}";
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidAvailPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Donor 14, 15, 16, 17: User cannot self-verify, change donorStatus, edit lastDonationDate, or substitute userId")
    void testDonorProfileSecurityRestrictionsEnforced() throws Exception {
        User userA = createTestUser("UserA", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        User userB = createTestUser("UserB", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String tokenA = getAccessToken(userA);

        // Create profile for userA
        CreateDonorProfileRequest createReq = new CreateDonorProfileRequest(BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Forged PUT attempting to set VERIFIED, INACTIVE, fake lastDonationDate, and substitute userId to userB
        String forgedPut = "{"
                + "\"bloodGroup\":\"O+\","
                + "\"bloodGroupVerificationStatus\":\"VERIFIED\","
                + "\"donorStatus\":\"INACTIVE\","
                + "\"lastDonationDate\":\"2026-09-15\","
                + "\"userId\":\"" + userB.getId() + "\""
                + "}";

        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgedPut))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroupVerificationStatus", is("SELF_REPORTED")))
                .andExpect(jsonPath("$.donorStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.lastDonationDate", nullValue()));

        DonorProfile profileA = donorProfileRepository.findByUserId(userA.getId()).orElseThrow();
        assertEquals(BloodGroupVerificationStatus.SELF_REPORTED, profileA.getBloodGroupVerificationStatus());
        assertEquals(DonorStatus.ACTIVE, profileA.getDonorStatus());
        assertNull(profileA.getLastDonationDate());

        // Confirm userB still has no donor profile (userId substitution failed)
        assertFalse(donorProfileRepository.existsByUserId(userB.getId()));
    }

    @Test
    @DisplayName("Donor 18: IDOR protection - User A cannot access or modify User B's user/donor profile")
    void testIdorProtectionAcrossUsers() throws Exception {
        User userA = createTestUser("IdorA", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        User userB = createTestUser("IdorB", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String tokenA = getAccessToken(userA);
        String tokenB = getAccessToken(userB);

        // Create donor profile for userB
        CreateDonorProfileRequest createB = new CreateDonorProfileRequest(BloodGroup.B_NEGATIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createB)))
                .andExpect(status().isCreated());

        // User A calls GET /api/v1/donor/profile -> gets User A's profile (404), NOT User B's profile
        mockMvc.perform(get("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        // User A attempts direct access to User B's user profile via /api/v1/users/{id}
        mockMvc.perform(get("/api/v1/users/" + userB.getId())
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Donor 19 & 20: Suspended and Deactivated users are blocked from profile and donor endpoints")
    void testSuspendedAndDeactivatedUsersBlocked() throws Exception {
        User suspendedUser = createTestUser("Suspended", UserStatus.SUSPENDED, Set.of(UserRole.ROLE_DONOR));
        String suspendedToken = getAccessToken(suspendedUser);

        User deactivatedUser = createTestUser("Deactivated", UserStatus.DEACTIVATED, Set.of(UserRole.ROLE_DONOR));
        String deactivatedToken = getAccessToken(deactivatedUser);

        // Suspended user blocked on GET /profile/me and /donor/profile
        mockMvc.perform(get("/api/v1/profile/me")
                .header("Authorization", "Bearer " + suspendedToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + suspendedToken))
                .andExpect(status().isUnauthorized());

        // Deactivated user blocked on GET /profile/me and /donor/profile
        mockMvc.perform(get("/api/v1/profile/me")
                .header("Authorization", "Bearer " + deactivatedToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + deactivatedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Donor 21: Changing blood group resets verification status from VERIFIED to SELF_REPORTED")
    void testBloodGroupChangeResetsVerificationStatus() throws Exception {
        User user = createTestUser("VerifiedDonor", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // Create profile
        CreateDonorProfileRequest createReq = new CreateDonorProfileRequest(BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Simulate authorized medical verification in DB
        DonorProfile profile = donorProfileRepository.findByUserId(user.getId()).orElseThrow();
        profile.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        donorProfileRepository.save(profile);

        // Verify it is currently VERIFIED
        mockMvc.perform(get("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroupVerificationStatus", is("VERIFIED")));

        // User now updates blood group to A+
        UpdateDonorProfileRequest updateReq = new UpdateDonorProfileRequest(BloodGroup.A_POSITIVE, null);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroup", is("A+")))
                .andExpect(jsonPath("$.bloodGroupVerificationStatus", is("SELF_REPORTED")));

        DonorProfile refetched = donorProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertEquals(BloodGroupVerificationStatus.SELF_REPORTED, refetched.getBloodGroupVerificationStatus(),
                "Changing blood group must immediately reset verification status to SELF_REPORTED");
    }

    @Test
    @DisplayName("Donor 22: Audit events logged for profile updates, donor profile lifecycle, blood group and availability changes")
    void testAuditEventsLoggedSafely() throws Exception {
        User user = createTestUser("AuditUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // 1. User profile update
        UpdateUserProfileRequest updateProfile = new UpdateUserProfileRequest("Audit NewName", "+919876543210");
        mockMvc.perform(put("/api/v1/profile/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfile)))
                .andExpect(status().isOk());

        // 2. Donor profile creation
        CreateDonorProfileRequest createDonor = new CreateDonorProfileRequest(BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDonor)))
                .andExpect(status().isCreated());

        // 3. Blood group and availability change
        UpdateDonorProfileRequest updateDonor = new UpdateDonorProfileRequest(BloodGroup.B_POSITIVE, DonorAvailabilityStatus.PAUSED);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDonor)))
                .andExpect(status().isOk());

        // Verify audit records exist for user
        List<SecurityAuditLog> userLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> user.getId().equals(l.getUserId()))
                .toList();

        Set<String> eventTypes = userLogs.stream().map(SecurityAuditLog::getEventType).collect(java.util.stream.Collectors.toSet());
        assertTrue(eventTypes.contains("PROFILE_UPDATED"), "PROFILE_UPDATED must be audited");
        assertTrue(eventTypes.contains("DONOR_PROFILE_CREATED"), "DONOR_PROFILE_CREATED must be audited");
        assertTrue(eventTypes.contains("BLOOD_GROUP_CHANGED"), "BLOOD_GROUP_CHANGED must be audited");
        assertTrue(eventTypes.contains("DONOR_AVAILABILITY_CHANGED"), "DONOR_AVAILABILITY_CHANGED must be audited");
        assertTrue(eventTypes.contains("DONOR_PROFILE_UPDATED"), "DONOR_PROFILE_UPDATED must be audited");

        // Verify no raw passwords or tokens are stored in audit logs
        for (SecurityAuditLog auditLog : userLogs) {
            assertFalse(auditLog.getMetadata() != null && auditLog.getMetadata().contains("SecurePass123"), "Audit log must never contain passwords");
            assertFalse(auditLog.getMetadata() != null && auditLog.getMetadata().contains(token), "Audit log must never contain raw tokens");
        }
    }

    @Test
    @DisplayName("Coordinates: create profile with invalid latitude returns 400")
    void testCreateDonorProfile_InvalidLatitude_Rejected400() throws Exception {
        User user = createTestUser("InvalidLatUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // latitude < -90
        CreateDonorProfileRequest reqTooLow = new CreateDonorProfileRequest(
                BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE, -95.0, 72.8);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqTooLow)))
                .andExpect(status().isBadRequest());

        // latitude > 90
        CreateDonorProfileRequest reqTooHigh = new CreateDonorProfileRequest(
                BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 95.0, 72.8);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqTooHigh)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Coordinates: create profile with invalid longitude returns 400")
    void testCreateDonorProfile_InvalidLongitude_Rejected400() throws Exception {
        User user = createTestUser("InvalidLngUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // longitude < -180
        CreateDonorProfileRequest reqTooLow = new CreateDonorProfileRequest(
                BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 18.9, -195.0);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqTooLow)))
                .andExpect(status().isBadRequest());

        // longitude > 180
        CreateDonorProfileRequest reqTooHigh = new CreateDonorProfileRequest(
                BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 18.9, 195.0);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqTooHigh)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Coordinates: create profile with latitude without longitude returns 400")
    void testCreateDonorProfile_LatitudeWithoutLongitude_Rejected400() throws Exception {
        User user = createTestUser("LatOnlyUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        CreateDonorProfileRequest req = new CreateDonorProfileRequest(
                BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 18.9450, null);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Coordinates: create profile with longitude without latitude returns 400")
    void testCreateDonorProfile_LongitudeWithoutLatitude_Rejected400() throws Exception {
        User user = createTestUser("LngOnlyUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        CreateDonorProfileRequest req = new CreateDonorProfileRequest(
                BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE, null, 72.8380);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Coordinates: create and update profile with valid coordinate pair is accepted")
    void testDonorProfile_ValidCoordinatePair_Accepted() throws Exception {
        User user = createTestUser("ValidCoordsUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String token = getAccessToken(user);

        // 1. Create with valid coordinates
        CreateDonorProfileRequest createReq = new CreateDonorProfileRequest(
                BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 18.9450, 72.8380);
        mockMvc.perform(post("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latitude").value(18.9450))
                .andExpect(jsonPath("$.longitude").value(72.8380));

        // 2. Update with invalid latitude -> 400
        UpdateDonorProfileRequest invalidLatReq = new UpdateDonorProfileRequest(
                BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 95.0, 72.8380);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLatReq)))
                .andExpect(status().isBadRequest());

        // 3. Update with invalid longitude -> 400
        UpdateDonorProfileRequest invalidLngReq = new UpdateDonorProfileRequest(
                BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 18.9450, 200.0);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLngReq)))
                .andExpect(status().isBadRequest());

        // 4. Update with latitude only -> 400
        UpdateDonorProfileRequest latOnlyReq = new UpdateDonorProfileRequest(
                BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 19.0000, null);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(latOnlyReq)))
                .andExpect(status().isBadRequest());

        // 5. Update with longitude only -> 400
        UpdateDonorProfileRequest lngOnlyReq = new UpdateDonorProfileRequest(
                BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE, null, 73.0000);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lngOnlyReq)))
                .andExpect(status().isBadRequest());

        // 6. Update with valid pair -> 200 accepted
        UpdateDonorProfileRequest validUpdate = new UpdateDonorProfileRequest(
                BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE, 19.0500, 73.0100);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(19.0500))
                .andExpect(jsonPath("$.longitude").value(73.0100));

        // 7. Update omitting coordinates (both null) preserves existing coordinates
        UpdateDonorProfileRequest omitCoords = new UpdateDonorProfileRequest(
                BloodGroup.A_POSITIVE, DonorAvailabilityStatus.PAUSED, null, null);
        mockMvc.perform(put("/api/v1/donor/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(omitCoords)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("PAUSED"))
                .andExpect(jsonPath("$.latitude").value(19.0500))
                .andExpect(jsonPath("$.longitude").value(73.0100));
    }
}
