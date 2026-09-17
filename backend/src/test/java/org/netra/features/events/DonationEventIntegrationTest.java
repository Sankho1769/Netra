package org.netra.features.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.repository.BloodInventoryRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.events.dto.*;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventRegistration;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.entity.DonationEventType;
import org.netra.features.events.repository.DonationEventRegistrationRepository;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.netra.features.events.service.DonationEventLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
class DonationEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DonationEventRepository donationEventRepository;

    @Autowired
    private DonationEventRegistrationRepository registrationRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

    @Autowired
    private BloodInventoryRepository bloodInventoryRepository;

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DonationEventLifecycleService lifecycleService;

    @Autowired
    private org.netra.features.events.service.DonationEventService donationEventService;

    private User createTestUser(String prefix, UserRole... roles) {
        String email = prefix.toLowerCase() + "." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                prefix + " User",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(roles)
        );
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private String getAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getRoles().stream().map(Enum::name).toList()
        );
    }

    private BloodBank createPersistedBloodBank(String name) {
        BloodBank bank = new BloodBank();
        bank.setName(name);
        bank.setAddress("789 Central Avenue");
        bank.setCity("Mumbai");
        bank.setState("Maharashtra");
        bank.setPostalCode("400001");
        bank.setLatitude(18.9401);
        bank.setLongitude(72.8347);
        bank.setPhone("+912223456789");
        bank.setVerificationStatus(BloodBankVerificationStatus.VERIFIED);
        return bloodBankRepository.save(bank);
    }

    private BloodBankAccount linkStaff(User user, BloodBank bank) {
        BloodBankAccount account = new BloodBankAccount(user.getId(), bank.getId(), BloodBankAccountStatus.ACTIVE);
        return bloodBankAccountRepository.save(account);
    }

    private CreateDonationEventRequest createSampleEventRequest(UUID bloodBankId) {
        Instant now = Instant.now();
        CreateDonationEventRequest req = new CreateDonationEventRequest();
        req.setBloodBankId(bloodBankId);
        req.setTitle("Annual Blood Drive 2026");
        req.setDescription("Join us to save lives at the annual community drive.");
        req.setVenueName("Town Hall Auditorium");
        req.setAddress("123 Main Street");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9401);
        req.setLongitude(72.8347);
        req.setStartAt(now.plus(3, ChronoUnit.DAYS));
        req.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS));
        req.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        req.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        req.setDonorCapacity(150);
        return req;
    }

    // =========================================================================
    // 1. AUTHORIZATION & RBAC TESTS
    // =========================================================================

    @Test
    @DisplayName("Auth 1: Donor cannot create donation events -> 403 Forbidden")
    void testDonorCannotCreateEvent() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Donor Auth");
        User donor = createTestUser("DonorTest", UserRole.ROLE_DONOR);
        String token = getAccessToken(donor);

        CreateDonationEventRequest req = createSampleEventRequest(bank.getId());

        mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Auth 2: Receiver cannot create donation events -> 403 Forbidden")
    void testReceiverCannotCreateEvent() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Receiver Auth");
        User receiver = createTestUser("ReceiverTest", UserRole.ROLE_RECEIVER);
        String token = getAccessToken(receiver);

        CreateDonationEventRequest req = createSampleEventRequest(bank.getId());

        mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Auth 3: Authorized blood bank staff can create DRAFT event for linked blood bank -> 201 Created")
    void testStaffCanCreateDraftEventForLinkedBank() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Staff Auth");
        User staff = createTestUser("StaffLinked", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        String token = getAccessToken(staff);

        CreateDonationEventRequest req = createSampleEventRequest(bank.getId());

        mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.currentRegistrationCount", is(0)))
                .andExpect(jsonPath("$.donorCapacity", is(150)));
    }

    @Test
    @DisplayName("Auth 4: Staff cannot create event for an unlinked blood bank -> 403 Forbidden (BOLA/IDOR protection)")
    void testStaffCannotCreateEventForUnlinkedBank() throws Exception {
        BloodBank bankA = createPersistedBloodBank("Bank Staff A");
        BloodBank bankB = createPersistedBloodBank("Bank Staff B");

        User staffA = createTestUser("StaffA", UserRole.ROLE_BLOODBANK);
        linkStaff(staffA, bankA); // Staff A is linked ONLY to Bank A
        String token = getAccessToken(staffA);

        CreateDonationEventRequest reqForBankB = createSampleEventRequest(bankB.getId());

        mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqForBankB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Auth 5: Non-admin user cannot approve or reject event -> 403 Forbidden")
    void testNonAdminCannotApproveEvent() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Approval Check");
        User staff = createTestUser("StaffForApprove", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        String staffToken = getAccessToken(staff);

        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Pending Camp");
        event.setVenueName("Auditorium");
        event.setAddress("123 St");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(18.94);
        event.setLongitude(72.83);
        event.setStartAt(Instant.now().plus(2, ChronoUnit.DAYS));
        event.setEndAt(Instant.now().plus(2, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(Instant.now().plus(1, ChronoUnit.DAYS));
        event.setDonorCapacity(100);
        event.setStatus(DonationEventStatus.PENDING_APPROVAL);
        event.setCreatedBy(staff.getId());
        event = donationEventRepository.save(event);

        ApproveDonationEventRequest approveReq = new ApproveDonationEventRequest(DonationEventStatus.PUBLISHED, null);

        mockMvc.perform(patch("/api/v1/donation-events/" + event.getId() + "/approval")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. STATE MACHINE WORKFLOW TESTS
    // =========================================================================

    @Test
    @DisplayName("Workflow 1: Complete lifecycle: DRAFT -> submit -> PENDING_APPROVAL -> admin approve -> PUBLISHED -> cancel")
    void testEventLifecycleWorkflow() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Lifecycle");
        User staff = createTestUser("StaffWorkflow", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        User admin = createTestUser("AdminWorkflow", UserRole.ROLE_ADMIN);

        String staffToken = getAccessToken(staff);
        String adminToken = getAccessToken(admin);

        // 1. Create DRAFT
        CreateDonationEventRequest createReq = createSampleEventRequest(bank.getId());
        MvcResult createRes = mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andReturn();

        UUID eventId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString()).path("id").asText());

        // 2. Submit for approval: DRAFT -> PENDING_APPROVAL
        mockMvc.perform(post("/api/v1/donation-events/" + eventId + "/submit")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")));

        // 3. Admin approves: PENDING_APPROVAL -> PUBLISHED
        ApproveDonationEventRequest approveReq = new ApproveDonationEventRequest(DonationEventStatus.PUBLISHED, null);
        mockMvc.perform(patch("/api/v1/donation-events/" + eventId + "/approval")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedAt", notNullValue()));

        // 4. Staff cancels event: PUBLISHED -> CANCELLED
        CancelDonationEventRequest cancelReq = new CancelDonationEventRequest("Weather conditions forced cancellation.");
        mockMvc.perform(post("/api/v1/donation-events/" + eventId + "/cancel")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()));

        // Verify event still exists in DB (soft cancel, history preserved!)
        assertTrue(donationEventRepository.findById(eventId).isPresent(), "Cancelled event must be preserved in DB");
    }

    @Test
    @DisplayName("Workflow 2: Admin rejects event: PENDING_APPROVAL -> REJECTED")
    void testEventAdminRejection() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Reject Workflow");
        User staff = createTestUser("StaffReject", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        User admin = createTestUser("AdminReject", UserRole.ROLE_ADMIN);

        String staffToken = getAccessToken(staff);
        String adminToken = getAccessToken(admin);

        CreateDonationEventRequest createReq = createSampleEventRequest(bank.getId());
        MvcResult createRes = mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        UUID eventId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString()).path("id").asText());

        // Submit
        mockMvc.perform(post("/api/v1/donation-events/" + eventId + "/submit")
                .header("Authorization", "Bearer " + staffToken));

        // Admin rejects
        ApproveDonationEventRequest rejectReq = new ApproveDonationEventRequest(DonationEventStatus.REJECTED, "Incomplete venue safety documentation.");
        mockMvc.perform(patch("/api/v1/donation-events/" + eventId + "/approval")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    @DisplayName("Workflow 3: Invalid state transition rejected -> 400 Bad Request")
    void testInvalidStateTransitionsRejected() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Invalid Transitions");
        User staff = createTestUser("StaffInvalid", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        String staffToken = getAccessToken(staff);

        CreateDonationEventRequest createReq = createSampleEventRequest(bank.getId());
        MvcResult createRes = mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        UUID eventId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString()).path("id").asText());

        // Attempting to submit an event when it's already submitted (or published) will be tested
        mockMvc.perform(post("/api/v1/donation-events/" + eventId + "/submit")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        // Re-submitting while PENDING_APPROVAL should fail
        mockMvc.perform(post("/api/v1/donation-events/" + eventId + "/submit")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    // =========================================================================
    // 3. REGISTRATION LIFECYCLE & INVENTORY ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Registration 1: Donor registers successfully; registration does NOT modify inventory or log INVENTORY_UPDATED")
    void testRegistrationDoesNotModifyInventory() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Inv Isolation");

        // Seed some inventory in this blood bank
        org.netra.features.bloodbank.entity.BloodInventory inv = new org.netra.features.bloodbank.entity.BloodInventory(
                bank.getId(), BloodGroup.O_POSITIVE, 25);
        bloodInventoryRepository.save(inv);

        User donor = createTestUser("DonorInvCheck", UserRole.ROLE_DONOR);
        String donorToken = getAccessToken(donor);

        // Published event
        Instant now = Instant.now();
        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Weekend Camp");
        event.setVenueName("Community Center");
        event.setAddress("456 Park Road");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(18.94);
        event.setLongitude(72.83);
        event.setStartAt(now.plus(3, ChronoUnit.DAYS));
        event.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        event.setDonorCapacity(50);
        event.setCurrentRegistrationCount(0);
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setCreatedBy(donor.getId());
        event = donationEventRepository.save(event);

        // Capture audit log count prior to registration
        long invAuditCountBefore = securityAuditLogRepository.findAll().stream()
                .filter(l -> "INVENTORY_UPDATED".equals(l.getEventType()))
                .count();

        // Perform registration
        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("REGISTERED")))
                .andExpect(jsonPath("$.donorUserId", is(donor.getId().toString())));

        // CRITICAL CHECK: Blood Bank inventory MUST NOT have changed
        org.netra.features.bloodbank.entity.BloodInventory afterInv = bloodInventoryRepository
                .findByBloodBankIdAndBloodGroup(bank.getId(), BloodGroup.O_POSITIVE).orElseThrow();
        assertEquals(25, afterInv.getUnitsAvailable(), "Blood inventory must remain exactly 25 units after event registration!");

        // CRITICAL CHECK: Zero INVENTORY_UPDATED audit logs created by registration
        long invAuditCountAfter = securityAuditLogRepository.findAll().stream()
                .filter(l -> "INVENTORY_UPDATED".equals(l.getEventType()))
                .count();
        assertEquals(invAuditCountBefore, invAuditCountAfter, "Registration must NEVER emit INVENTORY_UPDATED audit event!");
    }

    @Test
    @DisplayName("Registration 2: Duplicate registration returns 409 Conflict with ALREADY_REGISTERED")
    void testDuplicateRegistrationReturnsAlreadyRegistered() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Duplicate Test");
        User donor = createTestUser("DonorDuplicate", UserRole.ROLE_DONOR);
        String donorToken = getAccessToken(donor);

        Instant now = Instant.now();
        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Unique Camp");
        event.setVenueName("Hall");
        event.setAddress("Street");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(18.94);
        event.setLongitude(72.83);
        event.setStartAt(now.plus(3, ChronoUnit.DAYS));
        event.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        event.setDonorCapacity(10);
        event.setCurrentRegistrationCount(0);
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setCreatedBy(donor.getId());
        event = donationEventRepository.save(event);

        // First registration -> 201 Created
        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        // Repeated registration -> 409 Conflict with ALREADY_REGISTERED
        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("ALREADY_REGISTERED")));
    }

    @Test
    @DisplayName("Registration 3: Donor cancels registration, slot released, and donor can reactivate registration")
    void testRegistrationCancellationAndReactivation() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Reactivation Test");
        User donor = createTestUser("DonorReactivate", UserRole.ROLE_DONOR);
        String donorToken = getAccessToken(donor);

        Instant now = Instant.now();
        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Reactivation Camp");
        event.setVenueName("Hall");
        event.setAddress("Street");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(18.94);
        event.setLongitude(72.83);
        event.setStartAt(now.plus(3, ChronoUnit.DAYS));
        event.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        event.setDonorCapacity(5);
        event.setCurrentRegistrationCount(0);
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setCreatedBy(donor.getId());
        event = donationEventRepository.save(event);

        // 1. Register -> count = 1
        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());
        assertEquals(1, donationEventRepository.findById(event.getId()).orElseThrow().getCurrentRegistrationCount());

        // 2. Cancel -> count = 0, status = CANCELLED
        mockMvc.perform(delete("/api/v1/donation-events/" + event.getId() + "/registration")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        assertEquals(0, donationEventRepository.findById(event.getId()).orElseThrow().getCurrentRegistrationCount());

        // 3. Re-register -> reactivates row, count = 1, status = REGISTERED
        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("REGISTERED")));
        assertEquals(1, donationEventRepository.findById(event.getId()).orElseThrow().getCurrentRegistrationCount());
    }

    // =========================================================================
    // 4. PRIVACY TESTS
    // =========================================================================

    @Test
    @DisplayName("Privacy 1: Donor can view own registration, but cannot query attendee list of all donors -> 403")
    void testRegistrationPrivacyControls() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Privacy");
        User donor1 = createTestUser("DonorP1", UserRole.ROLE_DONOR);
        User donor2 = createTestUser("DonorP2", UserRole.ROLE_DONOR);
        String donor1Token = getAccessToken(donor1);

        Instant now = Instant.now();
        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Privacy Shield Camp");
        event.setVenueName("Hall");
        event.setAddress("Street");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(18.94);
        event.setLongitude(72.83);
        event.setStartAt(now.plus(3, ChronoUnit.DAYS));
        event.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        event.setDonorCapacity(10);
        event.setCurrentRegistrationCount(0);
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setCreatedBy(donor1.getId());
        event = donationEventRepository.save(event);

        // Donor 1 registers
        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                .header("Authorization", "Bearer " + donor1Token))
                .andExpect(status().isCreated());

        // Donor 1 queries own registration -> 200 OK
        mockMvc.perform(get("/api/v1/donation-events/" + event.getId() + "/registration")
                .header("Authorization", "Bearer " + donor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorUserId", is(donor1.getId().toString())));

        // Donor 1 attempts to query attendee list -> 403 Forbidden!
        mockMvc.perform(get("/api/v1/donation-events/" + event.getId() + "/registrations")
                .header("Authorization", "Bearer " + donor1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Privacy 2: Public event details do NOT expose organizer personal phone or creator user ID")
    void testPublicEventDetailsPrivacy() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank PII Check");
        User staff = createTestUser("StaffSecret", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);

        Instant now = Instant.now();
        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Public Details Shield Camp");
        event.setDescription("Public description only.");
        event.setVenueName("Public Hall");
        event.setAddress("Public St");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(18.94);
        event.setLongitude(72.83);
        event.setStartAt(now.plus(3, ChronoUnit.DAYS));
        event.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        event.setDonorCapacity(20);
        event.setCurrentRegistrationCount(5);
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setCreatedBy(staff.getId());
        event = donationEventRepository.save(event);

        mockMvc.perform(get("/api/v1/donation-events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Public Details Shield Camp")))
                .andExpect(jsonPath("$.createdBy").doesNotExist()) // Never leak creator user ID!
                .andExpect(jsonPath("$.staffPhone").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    // =========================================================================
    // 5. AUDIT EVENT TESTS
    // =========================================================================

    @Test
    @DisplayName("Audit: Events emit DONATION_EVENT_CREATED, DONATION_EVENT_SUBMITTED, DONATION_EVENT_PUBLISHED, and REGISTRATION audit logs")
    void testAuditTrailGeneration() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Audit Verify");
        User staff = createTestUser("StaffAuditor", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        User admin = createTestUser("AdminAuditor", UserRole.ROLE_ADMIN);
        User donor = createTestUser("DonorAuditor", UserRole.ROLE_DONOR);

        String staffToken = getAccessToken(staff);
        String adminToken = getAccessToken(admin);
        String donorToken = getAccessToken(donor);

        // 1. Create event -> DONATION_EVENT_CREATED
        CreateDonationEventRequest createReq = createSampleEventRequest(bank.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/donation-events")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        UUID eventId = UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).path("id").asText());

        List<SecurityAuditLog> createdLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "DONATION_EVENT_CREATED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(eventId.toString()))
                .toList();
        assertFalse(createdLogs.isEmpty(), "DONATION_EVENT_CREATED audit log must exist");

        // 2. Submit event -> DONATION_EVENT_SUBMITTED
        mockMvc.perform(post("/api/v1/donation-events/" + eventId + "/submit")
                .header("Authorization", "Bearer " + staffToken));

        List<SecurityAuditLog> submittedLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "DONATION_EVENT_SUBMITTED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(eventId.toString()))
                .toList();
        assertFalse(submittedLogs.isEmpty(), "DONATION_EVENT_SUBMITTED audit log must exist");

        // 3. Approve event -> DONATION_EVENT_PUBLISHED
        ApproveDonationEventRequest approveReq = new ApproveDonationEventRequest(DonationEventStatus.PUBLISHED, null);
        mockMvc.perform(patch("/api/v1/donation-events/" + eventId + "/approval")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveReq)));

        List<SecurityAuditLog> publishedLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "DONATION_EVENT_PUBLISHED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(eventId.toString()))
                .toList();
        assertFalse(publishedLogs.isEmpty(), "DONATION_EVENT_PUBLISHED audit log must exist");

        // 4. Register donor -> DONATION_EVENT_REGISTRATION_CREATED
        mockMvc.perform(post("/api/v1/donation-events/" + eventId + "/register")
                .header("Authorization", "Bearer " + donorToken));

        List<SecurityAuditLog> regLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "DONATION_EVENT_REGISTRATION_CREATED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(eventId.toString()))
                .toList();
        assertFalse(regLogs.isEmpty(), "DONATION_EVENT_REGISTRATION_CREATED audit log must exist");
    }

    // =========================================================================
    // 6. HARDENED DISCOVERY, BOLA, PRIVACY & LIFECYCLE TESTS
    // =========================================================================

    @Test
    @DisplayName("Discovery: Public user with bloodBankId sees ONLY public statuses; DRAFT/PENDING_APPROVAL/REJECTED are excluded")
    void testPublicUserWithBloodBankIdSeesOnlyPublicStatuses() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank Public Discovery Filter");
        User staff = createTestUser("StaffPubDisc", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);

        Instant now = Instant.now();
        // 1. PUBLISHED event
        DonationEvent pubEvent = new DonationEvent();
        pubEvent.setBloodBankId(bank.getId());
        pubEvent.setTitle("Public Camp 1");
        pubEvent.setVenueName("Hall 1");
        pubEvent.setAddress("Street 1");
        pubEvent.setCity("Mumbai");
        pubEvent.setState("Maharashtra");
        pubEvent.setPostalCode("400001");
        pubEvent.setLatitude(18.94);
        pubEvent.setLongitude(72.83);
        pubEvent.setStartAt(now.plus(3, ChronoUnit.DAYS));
        pubEvent.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        pubEvent.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        pubEvent.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        pubEvent.setDonorCapacity(50);
        pubEvent.setStatus(DonationEventStatus.PUBLISHED);
        pubEvent.setCreatedBy(staff.getId());
        donationEventRepository.save(pubEvent);

        // 2. DRAFT event
        DonationEvent draftEvent = new DonationEvent();
        draftEvent.setBloodBankId(bank.getId());
        draftEvent.setTitle("Draft Camp Secret");
        draftEvent.setVenueName("Hall 2");
        draftEvent.setAddress("Street 2");
        draftEvent.setCity("Mumbai");
        draftEvent.setState("Maharashtra");
        draftEvent.setPostalCode("400001");
        draftEvent.setLatitude(18.94);
        draftEvent.setLongitude(72.83);
        draftEvent.setStartAt(now.plus(4, ChronoUnit.DAYS));
        draftEvent.setEndAt(now.plus(4, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        draftEvent.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        draftEvent.setRegistrationCloseAt(now.plus(3, ChronoUnit.DAYS));
        draftEvent.setDonorCapacity(50);
        draftEvent.setStatus(DonationEventStatus.DRAFT);
        draftEvent.setCreatedBy(staff.getId());
        donationEventRepository.save(draftEvent);

        // 3. PENDING_APPROVAL event
        DonationEvent pendingEvent = new DonationEvent();
        pendingEvent.setBloodBankId(bank.getId());
        pendingEvent.setTitle("Pending Camp Secret");
        pendingEvent.setVenueName("Hall 3");
        pendingEvent.setAddress("Street 3");
        pendingEvent.setCity("Mumbai");
        pendingEvent.setState("Maharashtra");
        pendingEvent.setPostalCode("400001");
        pendingEvent.setLatitude(18.94);
        pendingEvent.setLongitude(72.83);
        pendingEvent.setStartAt(now.plus(5, ChronoUnit.DAYS));
        pendingEvent.setEndAt(now.plus(5, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        pendingEvent.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        pendingEvent.setRegistrationCloseAt(now.plus(4, ChronoUnit.DAYS));
        pendingEvent.setDonorCapacity(50);
        pendingEvent.setStatus(DonationEventStatus.PENDING_APPROVAL);
        pendingEvent.setCreatedBy(staff.getId());
        donationEventRepository.save(pendingEvent);

        // 4. REJECTED event
        DonationEvent rejectedEvent = new DonationEvent();
        rejectedEvent.setBloodBankId(bank.getId());
        rejectedEvent.setTitle("Rejected Camp Secret");
        rejectedEvent.setVenueName("Hall 4");
        rejectedEvent.setAddress("Street 4");
        rejectedEvent.setCity("Mumbai");
        rejectedEvent.setState("Maharashtra");
        rejectedEvent.setPostalCode("400001");
        rejectedEvent.setLatitude(18.94);
        rejectedEvent.setLongitude(72.83);
        rejectedEvent.setStartAt(now.plus(6, ChronoUnit.DAYS));
        rejectedEvent.setEndAt(now.plus(6, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        rejectedEvent.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        rejectedEvent.setRegistrationCloseAt(now.plus(5, ChronoUnit.DAYS));
        rejectedEvent.setDonorCapacity(50);
        rejectedEvent.setStatus(DonationEventStatus.REJECTED);
        rejectedEvent.setCreatedBy(staff.getId());
        donationEventRepository.save(rejectedEvent);

        // Unauthenticated call to discover by bloodBankId
        MvcResult res = mockMvc.perform(get("/api/v1/donation-events?bloodBankId=" + bank.getId()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        JsonNode content = json.get("content");
        assertTrue(content.isArray());

        // Verify only PUBLISHED event is returned, none of DRAFT/PENDING_APPROVAL/REJECTED
        for (JsonNode item : content) {
            String title = item.get("title").asText();
            String st = item.get("status").asText();
            assertNotEquals("Draft Camp Secret", title);
            assertNotEquals("Pending Camp Secret", title);
            assertNotEquals("Rejected Camp Secret", title);
            assertTrue("PUBLISHED".equals(st) || "REGISTRATION_CLOSED".equals(st) || "ONGOING".equals(st) || "COMPLETED".equals(st));
        }
    }

    @Test
    @DisplayName("BOLA: Staff A cannot access Bank B's private event data through discovery; returns 403 on private status")
    void testStaffCannotAccessAnotherBloodBankPrivateEventData() throws Exception {
        BloodBank bankA = createPersistedBloodBank("Bank A Staff Check");
        BloodBank bankB = createPersistedBloodBank("Bank B Staff Check");

        User staffA = createTestUser("StaffABOLA", UserRole.ROLE_BLOODBANK);
        linkStaff(staffA, bankA);
        String staffAToken = getAccessToken(staffA);

        User staffB = createTestUser("StaffBBOLA", UserRole.ROLE_BLOODBANK);
        linkStaff(staffB, bankB);
        String staffBToken = getAccessToken(staffB);

        User admin = createTestUser("AdminBOLA", UserRole.ROLE_ADMIN);
        String adminToken = getAccessToken(admin);

        Instant now = Instant.now();
        // Bank B has 1 PUBLISHED and 1 DRAFT event
        DonationEvent pubB = new DonationEvent();
        pubB.setBloodBankId(bankB.getId());
        pubB.setTitle("Bank B Public Camp");
        pubB.setVenueName("Venue B1");
        pubB.setAddress("Address B1");
        pubB.setCity("Mumbai");
        pubB.setState("Maharashtra");
        pubB.setPostalCode("400001");
        pubB.setLatitude(18.94);
        pubB.setLongitude(72.83);
        pubB.setStartAt(now.plus(3, ChronoUnit.DAYS));
        pubB.setEndAt(now.plus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        pubB.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        pubB.setRegistrationCloseAt(now.plus(2, ChronoUnit.DAYS));
        pubB.setDonorCapacity(30);
        pubB.setStatus(DonationEventStatus.PUBLISHED);
        pubB.setCreatedBy(staffB.getId());
        donationEventRepository.save(pubB);

        DonationEvent draftB = new DonationEvent();
        draftB.setBloodBankId(bankB.getId());
        draftB.setTitle("Bank B Secret Draft");
        draftB.setVenueName("Venue B2");
        draftB.setAddress("Address B2");
        draftB.setCity("Mumbai");
        draftB.setState("Maharashtra");
        draftB.setPostalCode("400001");
        draftB.setLatitude(18.94);
        draftB.setLongitude(72.83);
        draftB.setStartAt(now.plus(4, ChronoUnit.DAYS));
        draftB.setEndAt(now.plus(4, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        draftB.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        draftB.setRegistrationCloseAt(now.plus(3, ChronoUnit.DAYS));
        draftB.setDonorCapacity(30);
        draftB.setStatus(DonationEventStatus.DRAFT);
        draftB.setCreatedBy(staffB.getId());
        donationEventRepository.save(draftB);

        // 1. Staff A queries Bank B without private status -> receives ONLY public events
        MvcResult staffARes = mockMvc.perform(get("/api/v1/donation-events?bloodBankId=" + bankB.getId())
                .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode contentA = objectMapper.readTree(staffARes.getResponse().getContentAsString()).get("content");
        for (JsonNode item : contentA) {
            assertNotEquals("Bank B Secret Draft", item.get("title").asText());
        }

        // 2. Staff A explicitly requests Bank B's DRAFT events -> 403 Forbidden!
        mockMvc.perform(get("/api/v1/donation-events?bloodBankId=" + bankB.getId() + "&status=DRAFT")
                .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden());

        // 3. Bank B authorized staff queries Bank B's DRAFT events -> 200 OK with draft event
        mockMvc.perform(get("/api/v1/donation-events?bloodBankId=" + bankB.getId() + "&status=DRAFT")
                .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title == 'Bank B Secret Draft')]").exists());

        // 4. Admin queries Bank B's DRAFT events -> 200 OK
        mockMvc.perform(get("/api/v1/donation-events?bloodBankId=" + bankB.getId() + "&status=DRAFT")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title == 'Bank B Secret Draft')]").exists());
    }

    @Test
    @DisplayName("Privacy: Event detail contract accepts ONLY id with no coordinates; nearby computes distanceKm")
    void testEventDetailUsesNoUserCoordinates() throws Exception {
        // 1. Verify Controller contract: getEventById accepts ONLY (UUID id), no coordinate parameters
        java.lang.reflect.Method controllerMethod =
                org.netra.features.events.controller.DonationEventController.class.getMethod("getEventById", UUID.class);
        assertEquals(1, controllerMethod.getParameterCount(), "Controller getEventById must accept ONLY eventId");
        assertEquals(UUID.class, controllerMethod.getParameterTypes()[0]);
        assertTrue(controllerMethod.getParameters()[0].isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class),
                "Controller parameter must be a @PathVariable");

        // 2. Verify Service contract: getEventById accepts ONLY (UUID eventId), no coordinate parameters
        java.lang.reflect.Method serviceMethod =
                org.netra.features.events.service.DonationEventService.class.getMethod("getEventById", UUID.class);
        assertEquals(1, serviceMethod.getParameterCount(), "Service getEventById must accept ONLY eventId");
        assertEquals(UUID.class, serviceMethod.getParameterTypes()[0]);

        // 3. Create test event
        BloodBank bank = createPersistedBloodBank("Bank Location Privacy Contract");
        User staff = createTestUser("StaffLocPriv2", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        Instant now = Instant.now();

        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Privacy Location Camp");
        event.setVenueName("Hall");
        event.setAddress("MG Road");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(18.9401);
        event.setLongitude(72.8347);
        event.setStartAt(now.plus(2, ChronoUnit.DAYS));
        event.setEndAt(now.plus(2, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(1, ChronoUnit.DAYS));
        event.setDonorCapacity(40);
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setCreatedBy(staff.getId());
        event = donationEventRepository.save(event);

        // 4. Verify HTTP GET /{id} returns no calculated distance from user coordinates
        mockMvc.perform(get("/api/v1/donation-events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").doesNotExist());

        // 5. Verify direct service call returns distanceKm == null
        DonationEventDetailDto detailDto = donationEventService.getEventById(event.getId());
        assertNull(detailDto.getDistanceKm(), "Detail DTO distanceKm must always be null");

        // 6. Verify nearby endpoint still returns distanceKm
        mockMvc.perform(get("/api/v1/donation-events/nearby?latitude=18.9401&longitude=72.8347&radiusKm=10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')].distanceKm").exists());
    }

    @Test
    @DisplayName("Lifecycle: Transitions run in order, idempotent, future windows untouched, terminal states untouched")
    void testLifecycleTransitionsAndIdempotence() {
        BloodBank bank = createPersistedBloodBank("Bank Lifecycle Test");
        User staff = createTestUser("StaffLifecycle", UserRole.ROLE_BLOODBANK);
        linkStaff(staff, bank);
        Instant now = Instant.now();

        // Event 1: registrationCloseAt past, startAt future -> PUBLISHED -> REGISTRATION_CLOSED
        DonationEvent ev1 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp Close Due", DonationEventStatus.PUBLISHED,
                now.plus(2, ChronoUnit.HOURS), now.plus(8, ChronoUnit.HOURS),
                now.minus(5, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));

        // Event 2: registrationCloseAt past, startAt past, endAt future -> PUBLISHED -> ONGOING
        DonationEvent ev2 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp Start Due", DonationEventStatus.PUBLISHED,
                now.minus(1, ChronoUnit.HOURS), now.plus(4, ChronoUnit.HOURS),
                now.minus(6, ChronoUnit.HOURS), now.minus(2, ChronoUnit.HOURS));

        // Event 3: all past -> PUBLISHED -> COMPLETED
        DonationEvent ev3 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp End Due", DonationEventStatus.PUBLISHED,
                now.minus(4, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                now.minus(8, ChronoUnit.HOURS), now.minus(5, ChronoUnit.HOURS));

        // Event 4: future registrationCloseAt -> remains PUBLISHED
        DonationEvent ev4 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp Future", DonationEventStatus.PUBLISHED,
                now.plus(24, ChronoUnit.HOURS), now.plus(30, ChronoUnit.HOURS),
                now.minus(2, ChronoUnit.HOURS), now.plus(12, ChronoUnit.HOURS));

        // Event 5: CANCELLED terminal state with past times -> remains CANCELLED
        DonationEvent ev5 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp Cancelled", DonationEventStatus.CANCELLED,
                now.minus(4, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                now.minus(8, ChronoUnit.HOURS), now.minus(5, ChronoUnit.HOURS));

        // Event 6: REJECTED terminal state with past times -> remains REJECTED
        DonationEvent ev6 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp Rejected", DonationEventStatus.REJECTED,
                now.minus(4, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                now.minus(8, ChronoUnit.HOURS), now.minus(5, ChronoUnit.HOURS));

        // Event 7: DRAFT unapproved state with past times -> remains DRAFT
        DonationEvent ev7 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp Draft", DonationEventStatus.DRAFT,
                now.minus(4, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                now.minus(8, ChronoUnit.HOURS), now.minus(5, ChronoUnit.HOURS));

        // Event 8: PENDING_APPROVAL unapproved state with past times -> remains PENDING_APPROVAL
        DonationEvent ev8 = createLifecycleEvent(bank.getId(), staff.getId(), "Camp Pending", DonationEventStatus.PENDING_APPROVAL,
                now.minus(4, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                now.minus(8, ChronoUnit.HOURS), now.minus(5, ChronoUnit.HOURS));

        // First execution: processes transitions
        DonationEventLifecycleService.LifecycleTransitionResult res1 = lifecycleService.processLifecycleTransitions(now);
        assertTrue(res1.totalTransitions() > 0, "Initial lifecycle pass must execute transitions");

        // Verify state updates
        assertEquals(DonationEventStatus.REGISTRATION_CLOSED, donationEventRepository.findById(ev1.getId()).orElseThrow().getStatus());
        assertEquals(DonationEventStatus.ONGOING, donationEventRepository.findById(ev2.getId()).orElseThrow().getStatus());
        assertEquals(DonationEventStatus.COMPLETED, donationEventRepository.findById(ev3.getId()).orElseThrow().getStatus());
        assertEquals(DonationEventStatus.PUBLISHED, donationEventRepository.findById(ev4.getId()).orElseThrow().getStatus());
        assertEquals(DonationEventStatus.CANCELLED, donationEventRepository.findById(ev5.getId()).orElseThrow().getStatus());
        assertEquals(DonationEventStatus.REJECTED, donationEventRepository.findById(ev6.getId()).orElseThrow().getStatus());
        assertEquals(DonationEventStatus.DRAFT, donationEventRepository.findById(ev7.getId()).orElseThrow().getStatus());
        assertEquals(DonationEventStatus.PENDING_APPROVAL, donationEventRepository.findById(ev8.getId()).orElseThrow().getStatus());

        // Second execution immediately after: IDEMPOTENT (0 transitions)
        DonationEventLifecycleService.LifecycleTransitionResult res2 = lifecycleService.processLifecycleTransitions(now);
        assertEquals(0, res2.totalTransitions(), "Repeated execution with same timestamp must be idempotent (0 transitions)");
    }

    @Test
    @DisplayName("Integrity: Unrelated DataIntegrityViolationException is reported as CONFLICT, not ALREADY_REGISTERED")
    void testUnrelatedDataIntegrityViolationHandling() {
        org.netra.core.exception.GlobalExceptionHandler handler = new org.netra.core.exception.GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/donation-events/123/register");

        // Simulate an unrelated integrity violation (e.g. check constraint failure)
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException("check constraint failed: chk_event_capacity");

        org.springframework.http.ResponseEntity<org.netra.features.eligibility.dto.ErrorResponse> response =
                handler.handleDataIntegrityViolation(ex, request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("CONFLICT", response.getBody().getError());
        assertNotEquals("ALREADY_REGISTERED", response.getBody().getError());
    }

    private DonationEvent createLifecycleEvent(UUID bankId, UUID creatorId, String title, DonationEventStatus status,
                                               Instant startAt, Instant endAt, Instant regOpenAt, Instant regCloseAt) {
        DonationEvent ev = new DonationEvent();
        ev.setBloodBankId(bankId);
        ev.setCreatedBy(creatorId);
        ev.setTitle(title);
        ev.setVenueName("Venue " + title);
        ev.setAddress("Address 123");
        ev.setCity("Mumbai");
        ev.setState("Maharashtra");
        ev.setPostalCode("400001");
        ev.setLatitude(18.94);
        ev.setLongitude(72.83);
        ev.setStartAt(startAt);
        ev.setEndAt(endAt);
        ev.setRegistrationOpenAt(regOpenAt);
        ev.setRegistrationCloseAt(regCloseAt);
        ev.setDonorCapacity(50);
        ev.setStatus(status);
        return donationEventRepository.save(ev);
    }
}
