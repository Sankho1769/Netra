package org.netra.features.bloodbank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodbank.dto.*;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.repository.BloodInventoryRepository;
import org.netra.features.donor.entity.BloodGroup;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BloodBankIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodInventoryRepository bloodInventoryRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

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

    private BloodBank createPersistedBloodBank(String name, String city, double lat, double lng, BloodBankVerificationStatus status) {
        BloodBank bank = new BloodBank();
        bank.setName(name);
        bank.setAddress("123 Hospital Road");
        bank.setCity(city);
        bank.setState("Maharashtra");
        bank.setPostalCode("400001");
        bank.setLatitude(lat);
        bank.setLongitude(lng);
        bank.setPhone("+912224177000");
        bank.setEmail("contact@" + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".org");
        bank.setOperatingStatus(BloodBankOperatingStatus.OPEN);
        bank.setVerificationStatus(status);
        return bloodBankRepository.save(bank);
    }

    private BloodBankAccount linkStaff(User user, BloodBank bank, BloodBankAccountStatus status) {
        BloodBankAccount account = new BloodBankAccount(user.getId(), bank.getId(), status);
        return bloodBankAccountRepository.save(account);
    }

    @Test
    @DisplayName("Authorization 1: Unauthenticated user cannot create a blood bank -> 401")
    void testUnauthenticatedUserCannotCreateBloodBank() throws Exception {
        CreateBloodBankRequest request = new CreateBloodBankRequest();
        request.setName("City Central Blood Centre");
        request.setAddress("10 Main St");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setPostalCode("400001");
        request.setLatitude(18.9220);
        request.setLongitude(72.8347);
        request.setPhone("+912224177000");

        mockMvc.perform(post("/api/v1/bloodbanks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization 2 & 3: DONOR and RECEIVER cannot create a blood bank -> 403")
    void testDonorAndReceiverCannotCreateBloodBank() throws Exception {
        User donor = createTestUser("DonorBankCreator", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String donorToken = getAccessToken(donor);

        User receiver = createTestUser("ReceiverBankCreator", UserStatus.ACTIVE, Set.of(UserRole.ROLE_RECEIVER));
        String receiverToken = getAccessToken(receiver);

        CreateBloodBankRequest request = new CreateBloodBankRequest();
        request.setName("Unauthorized Blood Centre");
        request.setAddress("10 Main St");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setPostalCode("400001");
        request.setLatitude(18.9220);
        request.setLongitude(72.8347);
        request.setPhone("+912224177000");

        mockMvc.perform(post("/api/v1/bloodbanks")
                .header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/bloodbanks")
                .header("Authorization", "Bearer " + receiverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorization 4 & 5: DONOR and RECEIVER cannot modify a blood bank -> 403")
    void testDonorAndReceiverCannotModifyBloodBank() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank For Modify Check", "Pune", 18.5204, 73.8567, BloodBankVerificationStatus.VERIFIED);

        User donor = createTestUser("DonorModifier", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String donorToken = getAccessToken(donor);

        UpdateBloodBankRequest updateReq = new UpdateBloodBankRequest();
        updateReq.setName("Maliciously Renamed Bank");

        mockMvc.perform(put("/api/v1/bloodbanks/" + bank.getId())
                .header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorization 6: DONOR and RECEIVER cannot update inventory -> 403")
    void testUnauthorizedUserCannotUpdateInventory() throws Exception {
        BloodBank bank = createPersistedBloodBank("Bank For Inv Check", "Mumbai", 19.0760, 72.8777, BloodBankVerificationStatus.VERIFIED);

        User donor = createTestUser("DonorInvModifier", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String donorToken = getAccessToken(donor);

        UpdateInventoryRequest invReq = new UpdateInventoryRequest(BloodGroup.O_POSITIVE, 10);

        mockMvc.perform(put("/api/v1/bloodbanks/" + bank.getId() + "/inventory")
                .header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lifecycle 1: Authorized ADMIN creates blood bank -> 201, starts unconditionally as PENDING")
    void testAdminCreatesBloodBankDefaultsToPending() throws Exception {
        User admin = createTestUser("AdminCreator", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        String adminToken = getAccessToken(admin);

        CreateBloodBankRequest request = new CreateBloodBankRequest();
        request.setName("National Apex Blood Centre");
        request.setRegistrationNumber("REG-MH-2026-9901");
        request.setAddress("45 Apex Avenue");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setPostalCode("400012");
        request.setLatitude(18.9980);
        request.setLongitude(72.8420);
        request.setPhone("+912224199999");
        request.setEmail("contact@apexblood.org");

        MvcResult result = mockMvc.perform(post("/api/v1/bloodbanks")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("National Apex Blood Centre")))
                .andExpect(jsonPath("$.verificationStatus", is("PENDING")))
                .andExpect(jsonPath("$.operatingStatus", is("OPEN")))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID bankId = UUID.fromString(root.get("id").asText());

        // Verify database state
        BloodBank persisted = bloodBankRepository.findById(bankId).orElseThrow();
        assertEquals(BloodBankVerificationStatus.PENDING, persisted.getVerificationStatus());
        assertEquals("REG-MH-2026-9901", persisted.getRegistrationNumber());

        // Verify audit event
        List<SecurityAuditLog> logs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "BLOOD_BANK_CREATED".equals(l.getEventType()))
                .toList();
        assertFalse(logs.isEmpty(), "Security audit log for BLOOD_BANK_CREATED must exist");
    }

    @Test
    @DisplayName("Lifecycle 2: ADMIN verification workflow transitions PENDING -> VERIFIED -> SUSPENDED; invalid transitions rejected -> 400")
    void testAdminTransitionsVerificationStatusAndInvalidTransitions() throws Exception {
        BloodBank bank = createPersistedBloodBank("Pending Hospital Blood Centre", "Nagpur", 21.1458, 79.0882, BloodBankVerificationStatus.PENDING);

        User donor = createTestUser("DonorAttemptingVerify", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String donorToken = getAccessToken(donor);

        User admin = createTestUser("AdminVerifier", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        String adminToken = getAccessToken(admin);

        // 1. Unauthorized attempt returns 403
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/verification")
                .header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBloodBankVerificationRequest(BloodBankVerificationStatus.VERIFIED))))
                .andExpect(status().isForbidden());

        // 2. Invalid transition PENDING -> SUSPENDED returns 400
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBloodBankVerificationRequest(BloodBankVerificationStatus.SUSPENDED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));

        // 3. Valid transition PENDING -> VERIFIED returns 200
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBloodBankVerificationRequest(BloodBankVerificationStatus.VERIFIED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("VERIFIED")));

        BloodBank verified = bloodBankRepository.findById(bank.getId()).orElseThrow();
        assertEquals(BloodBankVerificationStatus.VERIFIED, verified.getVerificationStatus());

        // 4. Valid transition VERIFIED -> SUSPENDED returns 200
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBloodBankVerificationRequest(BloodBankVerificationStatus.SUSPENDED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("SUSPENDED")));

        // 5. Valid transition SUSPENDED -> VERIFIED returns 200
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBloodBankVerificationRequest(BloodBankVerificationStatus.VERIFIED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("VERIFIED")));
    }

    @Test
    @DisplayName("IDOR / BOLA: Staff of Bank A can manage Bank A, but CANNOT manage Bank B (403); Admin can manage both")
    void testStaffOwnershipAndIDORProtection() throws Exception {
        BloodBank bankA = createPersistedBloodBank("Bank Alpha", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.VERIFIED);
        BloodBank bankB = createPersistedBloodBank("Bank Beta", "Mumbai", 18.9410, 72.8350, BloodBankVerificationStatus.VERIFIED);

        User staffA = createTestUser("StaffAlpha", UserStatus.ACTIVE, Set.of(UserRole.ROLE_BLOODBANK));
        linkStaff(staffA, bankA, BloodBankAccountStatus.ACTIVE);
        String staffAToken = getAccessToken(staffA);

        User admin = createTestUser("SuperAdmin", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        String adminToken = getAccessToken(admin);

        UpdateInventoryRequest invReq = new UpdateInventoryRequest(BloodGroup.O_POSITIVE, 15);

        // 1. Staff A -> Bank A inventory = ALLOWED (200)
        mockMvc.perform(put("/api/v1/bloodbanks/" + bankA.getId() + "/inventory")
                .header("Authorization", "Bearer " + staffAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsAvailable", is(15)));

        // 2. Staff A -> Bank B inventory = FORBIDDEN (403) - BOLA/IDOR Defense
        mockMvc.perform(put("/api/v1/bloodbanks/" + bankB.getId() + "/inventory")
                .header("Authorization", "Bearer " + staffAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        // 3. Staff A -> Bank B details update = FORBIDDEN (403)
        UpdateBloodBankRequest detailsReq = new UpdateBloodBankRequest();
        detailsReq.setName("Illegally Modifying Bank B");
        mockMvc.perform(put("/api/v1/bloodbanks/" + bankB.getId())
                .header("Authorization", "Bearer " + staffAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(detailsReq)))
                .andExpect(status().isForbidden());

        // 4. Staff A -> Bank A details update = ALLOWED (200)
        UpdateBloodBankRequest ownBankReq = new UpdateBloodBankRequest();
        ownBankReq.setName("Bank Alpha Renamed Legally");
        mockMvc.perform(put("/api/v1/bloodbanks/" + bankA.getId())
                .header("Authorization", "Bearer " + staffAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownBankReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Bank Alpha Renamed Legally")));

        // 5. Admin -> Bank B inventory = ALLOWED (200)
        mockMvc.perform(put("/api/v1/bloodbanks/" + bankB.getId() + "/inventory")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateInventoryRequest(BloodGroup.O_POSITIVE, 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsAvailable", is(20)));
    }

    @Test
    @DisplayName("Account Status: SUSPENDED staff account cannot manage linked blood bank -> 403")
    void testSuspendedStaffAccountCannotManage() throws Exception {
        BloodBank bank = createPersistedBloodBank("Status Gated Bank", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.VERIFIED);

        User staff = createTestUser("SuspendedStaff", UserStatus.ACTIVE, Set.of(UserRole.ROLE_BLOODBANK));
        BloodBankAccount account = linkStaff(staff, bank, BloodBankAccountStatus.SUSPENDED);
        String staffToken = getAccessToken(staff);

        UpdateInventoryRequest invReq = new UpdateInventoryRequest(BloodGroup.B_POSITIVE, 10);

        // Staff account is SUSPENDED -> 403 Forbidden
        mockMvc.perform(put("/api/v1/bloodbanks/" + bank.getId() + "/inventory")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Staff Linking: Admin links staff user to blood bank and updates status")
    void testAdminStaffAccountLinkingAndLifecycle() throws Exception {
        BloodBank bank = createPersistedBloodBank("Linking Test Bank", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.VERIFIED);
        User user = createTestUser("FutureStaff", UserStatus.ACTIVE, Set.of(UserRole.ROLE_BLOODBANK));

        User admin = createTestUser("LinkingAdmin", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        String adminToken = getAccessToken(admin);

        // 1. Link staff account
        MvcResult result = mockMvc.perform(post("/api/v1/bloodbanks/" + bank.getId() + "/accounts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LinkStaffAccountRequest(user.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID accountId = UUID.fromString(root.get("id").asText());

        // 2. Suspend staff account
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/accounts/" + accountId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateStaffAccountStatusRequest(BloodBankAccountStatus.SUSPENDED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUSPENDED")));

        BloodBankAccount updated = bloodBankAccountRepository.findById(accountId).orElseThrow();
        assertEquals(BloodBankAccountStatus.SUSPENDED, updated.getStatus());
    }

    @Test
    @DisplayName("Discovery 1 & 2: Public discovery returns only VERIFIED banks and supports city filter")
    void testPublicDiscoveryAndCityFilter() throws Exception {
        String uniqueCity = "Nashik-" + UUID.randomUUID().toString().substring(0, 8);

        BloodBank verifiedBank = createPersistedBloodBank("Nashik Verified Centre", uniqueCity, 19.9975, 73.7898, BloodBankVerificationStatus.VERIFIED);
        BloodBank pendingBank = createPersistedBloodBank("Nashik Pending Centre", uniqueCity, 19.9980, 73.7900, BloodBankVerificationStatus.PENDING);

        mockMvc.perform(get("/api/v1/bloodbanks")
                .param("city", uniqueCity))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Nashik Verified Centre")))
                .andExpect(jsonPath("$.content[0].verificationStatus", is("VERIFIED")));
    }

    @Test
    @DisplayName("Discovery 3: Excessive page size (> 50) is bounded to 50")
    void testExcessivePageSizeIsBounded() throws Exception {
        mockMvc.perform(get("/api/v1/bloodbanks")
                .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(50)));
    }

    @Test
    @DisplayName("Discovery 4 & 5: Nearby search computes Haversine distance, orders by distance, and enforces coordinates/radius")
    void testNearbySearchDistanceAndValidation() throws Exception {
        // Point: MG Road Bengaluru (12.9716, 77.5946)
        double refLat = 12.9716;
        double refLon = 77.5946;

        // Bank 1: Close (~0.5 km away)
        BloodBank closeBank = createPersistedBloodBank("Bengaluru Central Blood Centre", "Bengaluru", 12.9750, 77.5980, BloodBankVerificationStatus.VERIFIED);
        // Bank 2: Far away (~290 km away, Chennai)
        BloodBank farBank = createPersistedBloodBank("Chennai Blood Centre", "Chennai", 13.0827, 80.2707, BloodBankVerificationStatus.VERIFIED);

        // 1. Valid nearby search within 5 km
        mockMvc.perform(get("/api/v1/bloodbanks/nearby")
                .param("latitude", String.valueOf(refLat))
                .param("longitude", String.valueOf(refLon))
                .param("radiusKm", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Bengaluru Central Blood Centre")))
                .andExpect(jsonPath("$[0].distanceKm", notNullValue()));

        // 2. Reject invalid latitude (> 90)
        mockMvc.perform(get("/api/v1/bloodbanks/nearby")
                .param("latitude", "95.0")
                .param("longitude", "77.5946")
                .param("radiusKm", "10.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));

        // 3. Reject excessive radius (> 100km)
        mockMvc.perform(get("/api/v1/bloodbanks/nearby")
                .param("latitude", "12.9716")
                .param("longitude", "77.5946")
                .param("radiusKm", "150.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Inventory 1: Negative inventory units rejected -> 400")
    void testNegativeUnitsRejected() throws Exception {
        BloodBank bank = createPersistedBloodBank("Inventory Reject Bank", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.VERIFIED);

        User admin = createTestUser("AdminNegativeTester", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        String adminToken = getAccessToken(admin);

        UpdateInventoryRequest negativeReq = new UpdateInventoryRequest(BloodGroup.O_POSITIVE, -5);

        mockMvc.perform(put("/api/v1/bloodbanks/" + bank.getId() + "/inventory")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(negativeReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Inventory 2: Authorized staff updates inventory -> 200, freshness is FRESH, audit logged")
    void testAuthorizedInventoryUpdateAndFreshness() throws Exception {
        BloodBank bank = createPersistedBloodBank("Inventory Fresh Bank", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.VERIFIED);

        User bloodbankStaff = createTestUser("StaffInvTester", UserStatus.ACTIVE, Set.of(UserRole.ROLE_BLOODBANK));
        linkStaff(bloodbankStaff, bank, BloodBankAccountStatus.ACTIVE);
        String staffToken = getAccessToken(bloodbankStaff);

        UpdateInventoryRequest invReq = new UpdateInventoryRequest(BloodGroup.A_POSITIVE, 12);

        mockMvc.perform(put("/api/v1/bloodbanks/" + bank.getId() + "/inventory")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroup", is("A+")))
                .andExpect(jsonPath("$.unitsAvailable", is(12)))
                .andExpect(jsonPath("$.freshness", is("FRESH")));

        // Verify public inventory endpoint
        mockMvc.perform(get("/api/v1/bloodbanks/" + bank.getId() + "/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bloodGroup", is("A+")))
                .andExpect(jsonPath("$[0].unitsAvailable", is(12)))
                .andExpect(jsonPath("$[0].freshness", is("FRESH")));

        // Verify audit log for inventory update
        List<SecurityAuditLog> invLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "INVENTORY_UPDATED".equals(l.getEventType()))
                .toList();
        assertFalse(invLogs.isEmpty(), "Audit event INVENTORY_UPDATED must be logged");
    }

    @Test
    @DisplayName("Inventory 3: Suspended blood bank cannot perform inventory operations -> 403")
    void testSuspendedBloodBankCannotPerformInventoryUpdates() throws Exception {
        BloodBank suspendedBank = createPersistedBloodBank("Suspended Central Bank", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.SUSPENDED);

        User admin = createTestUser("AdminSuspendedCheck", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        String adminToken = getAccessToken(admin);

        UpdateInventoryRequest invReq = new UpdateInventoryRequest(BloodGroup.B_POSITIVE, 10);

        mockMvc.perform(put("/api/v1/bloodbanks/" + suspendedBank.getId() + "/inventory")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("ACCOUNT_DISABLED")));
    }

    @Test
    @DisplayName("Discovery 6: Blood group filter only returns verified blood banks with stock > 0")
    void testBloodGroupFilterReturnsBanksWithStock() throws Exception {
        String filterCity = "Surat-" + UUID.randomUUID().toString().substring(0, 8);

        BloodBank bankWithStock = createPersistedBloodBank("Surat Bank With Stock", filterCity, 21.1702, 72.8311, BloodBankVerificationStatus.VERIFIED);
        BloodBank bankWithoutStock = createPersistedBloodBank("Surat Bank Without Stock", filterCity, 21.1710, 72.8320, BloodBankVerificationStatus.VERIFIED);

        // Add AB- stock to bankWithStock
        org.netra.features.bloodbank.entity.BloodInventory stock = new org.netra.features.bloodbank.entity.BloodInventory(
                bankWithStock.getId(), BloodGroup.AB_NEGATIVE, 8);
        bloodInventoryRepository.save(stock);

        mockMvc.perform(get("/api/v1/bloodbanks")
                .param("city", filterCity)
                .param("bloodGroup", "AB-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Surat Bank With Stock")));
    }

    // =========================================================================
    // MULTI-ROLE AUTHORIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Multi-Role 1: User with [ROLE_DONOR, ROLE_ADMIN] can transition verification status -> 200")
    void testMultiRoleDonorAndAdminCanPerformAdminAction() throws Exception {
        BloodBank bank = createPersistedBloodBank("MultiRole Admin Bank", "Pune", 18.5204, 73.8567, BloodBankVerificationStatus.PENDING);

        // Multi-role user: both DONOR and ADMIN
        User multiRoleAdmin = createTestUser("DonorAdminUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR, UserRole.ROLE_ADMIN));
        String token = getAccessToken(multiRoleAdmin);

        UpdateBloodBankVerificationRequest req = new UpdateBloodBankVerificationRequest(BloodBankVerificationStatus.VERIFIED);

        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/verification")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("VERIFIED")));
    }

    @Test
    @DisplayName("Multi-Role 2: User with [ROLE_DONOR, ROLE_RECEIVER] is rejected from admin actions -> 403")
    void testMultiRoleDonorAndReceiverForbiddenFromAdminAction() throws Exception {
        BloodBank bank = createPersistedBloodBank("MultiRole Forbidden Bank", "Pune", 18.5204, 73.8567, BloodBankVerificationStatus.PENDING);

        User regularUser = createTestUser("DonorReceiverUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR, UserRole.ROLE_RECEIVER));
        String token = getAccessToken(regularUser);

        UpdateBloodBankVerificationRequest req = new UpdateBloodBankVerificationRequest(BloodBankVerificationStatus.VERIFIED);

        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/verification")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Multi-Role 3: User with [ROLE_DONOR, ROLE_BLOODBANK] linked to bank can update operating status -> 200")
    void testMultiRoleDonorAndBloodBankStaffCanManageLinkedBank() throws Exception {
        BloodBank bank = createPersistedBloodBank("MultiRole Staff Bank", "Pune", 18.5204, 73.8567, BloodBankVerificationStatus.VERIFIED);

        User multiRoleStaff = createTestUser("DonorStaffUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR, UserRole.ROLE_BLOODBANK));
        linkStaff(multiRoleStaff, bank, BloodBankAccountStatus.ACTIVE);
        String token = getAccessToken(multiRoleStaff);

        UpdateOperatingStatusRequest req = new UpdateOperatingStatusRequest(BloodBankOperatingStatus.CLOSED);

        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatingStatus", is("CLOSED")));
    }

    @Test
    @DisplayName("Multi-Role 4: User with [ROLE_BLOODBANK, ROLE_RECEIVER] linked to bank can update inventory -> 200")
    void testMultiRoleBloodBankAndReceiverCanUpdateInventory() throws Exception {
        BloodBank bank = createPersistedBloodBank("MultiRole Inv Bank", "Pune", 18.5204, 73.8567, BloodBankVerificationStatus.VERIFIED);

        User multiRoleStaff = createTestUser("StaffReceiverUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_BLOODBANK, UserRole.ROLE_RECEIVER));
        linkStaff(multiRoleStaff, bank, BloodBankAccountStatus.ACTIVE);
        String token = getAccessToken(multiRoleStaff);

        UpdateInventoryRequest invReq = new UpdateInventoryRequest(BloodGroup.O_NEGATIVE, 20);

        mockMvc.perform(put("/api/v1/bloodbanks/" + bank.getId() + "/inventory")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsAvailable", is(20)));
    }

    // =========================================================================
    // CORS PREFLIGHT TESTS
    // =========================================================================

    @Test
    @DisplayName("CORS 1: Preflight OPTIONS request for PATCH verification allows PATCH method")
    void testCorsPreflightForPatchVerification() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(options("/api/v1/bloodbanks/" + randomId + "/verification")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS 2: Preflight OPTIONS request for PATCH operating status allows PATCH method")
    void testCorsPreflightForPatchOperatingStatus() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(options("/api/v1/bloodbanks/" + randomId + "/status")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }

    @Test
    @DisplayName("CORS 3: Preflight OPTIONS request from untrusted origin is not allowed")
    void testCorsPreflightUntrustedOriginNotAllowed() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(options("/api/v1/bloodbanks/" + randomId + "/status")
                .header("Origin", "http://untrusted-hacker-site.com")
                .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // =========================================================================
    // STAFF ROLE GRANT & LIFECYCLE AUDIT TESTS
    // =========================================================================

    @Test
    @DisplayName("Audit 1: Linking staff audits both ROLE_GRANTED and BLOOD_BANK_ACCOUNT_CREATED")
    void testStaffRoleGrantAndAccountCreationAuditEvents() throws Exception {
        BloodBank bank = createPersistedBloodBank("Staff Audit Bank", "Nagpur", 21.1458, 79.0882, BloodBankVerificationStatus.VERIFIED);
        User admin = createTestUser("AuditAdmin", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        User regularUser = createTestUser("NewStaffUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_DONOR));
        String adminToken = getAccessToken(admin);

        LinkStaffAccountRequest linkReq = new LinkStaffAccountRequest(regularUser.getId());

        mockMvc.perform(post("/api/v1/bloodbanks/" + bank.getId() + "/accounts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(regularUser.getId().toString())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // Verify ROLE_GRANTED audit event
        List<SecurityAuditLog> roleGrantedLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "ROLE_GRANTED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(regularUser.getId().toString()))
                .toList();
        assertFalse(roleGrantedLogs.isEmpty(), "Audit event ROLE_GRANTED must be logged with targetUserId");
        assertTrue(roleGrantedLogs.get(0).getMetadata().contains("ROLE_BLOODBANK"));
        assertEquals(admin.getId(), roleGrantedLogs.get(0).getUserId(), "Actor must be admin");

        // Verify BLOOD_BANK_ACCOUNT_CREATED audit event
        List<SecurityAuditLog> accountCreatedLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "BLOOD_BANK_ACCOUNT_CREATED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(regularUser.getId().toString()))
                .toList();
        assertFalse(accountCreatedLogs.isEmpty(), "Audit event BLOOD_BANK_ACCOUNT_CREATED must be logged");
        assertEquals(admin.getId(), accountCreatedLogs.get(0).getUserId(), "Actor must be admin");
    }

    @Test
    @DisplayName("Audit 2: Staff status transitions audit SUSPENDED, REVOKED, and REACTIVATED")
    void testStaffAccountLifecycleAuditEvents() throws Exception {
        BloodBank bank = createPersistedBloodBank("Lifecycle Bank", "Nashik", 19.9975, 73.7898, BloodBankVerificationStatus.VERIFIED);
        User admin = createTestUser("AuditAdmin2", UserStatus.ACTIVE, Set.of(UserRole.ROLE_ADMIN));
        User staff = createTestUser("StaffLifecycleUser", UserStatus.ACTIVE, Set.of(UserRole.ROLE_BLOODBANK));
        BloodBankAccount account = linkStaff(staff, bank, BloodBankAccountStatus.ACTIVE);
        String adminToken = getAccessToken(admin);

        // 1. Transition to SUSPENDED -> BLOOD_BANK_ACCOUNT_SUSPENDED
        UpdateStaffAccountStatusRequest suspendReq = new UpdateStaffAccountStatusRequest(BloodBankAccountStatus.SUSPENDED);
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/accounts/" + account.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUSPENDED")));

        List<SecurityAuditLog> suspendedLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "BLOOD_BANK_ACCOUNT_SUSPENDED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(account.getId().toString()))
                .toList();
        assertFalse(suspendedLogs.isEmpty(), "Audit event BLOOD_BANK_ACCOUNT_SUSPENDED must be logged");

        // 2. Transition to ACTIVE -> BLOOD_BANK_ACCOUNT_REACTIVATED
        UpdateStaffAccountStatusRequest reactivateReq = new UpdateStaffAccountStatusRequest(BloodBankAccountStatus.ACTIVE);
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/accounts/" + account.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        List<SecurityAuditLog> reactivatedLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "BLOOD_BANK_ACCOUNT_REACTIVATED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(account.getId().toString()))
                .toList();
        assertFalse(reactivatedLogs.isEmpty(), "Audit event BLOOD_BANK_ACCOUNT_REACTIVATED must be logged");

        // 3. Transition to REVOKED -> BLOOD_BANK_ACCOUNT_REVOKED
        UpdateStaffAccountStatusRequest revokeReq = new UpdateStaffAccountStatusRequest(BloodBankAccountStatus.REVOKED);
        mockMvc.perform(patch("/api/v1/bloodbanks/" + bank.getId() + "/accounts/" + account.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVOKED")));

        List<SecurityAuditLog> revokedLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "BLOOD_BANK_ACCOUNT_REVOKED".equals(l.getEventType()))
                .filter(l -> l.getMetadata() != null && l.getMetadata().contains(account.getId().toString()))
                .toList();
        assertFalse(revokedLogs.isEmpty(), "Audit event BLOOD_BANK_ACCOUNT_REVOKED must be logged");
    }

    // =========================================================================
    // LOCATION PRIVACY TESTS
    // =========================================================================

    @Test
    @DisplayName("Location Privacy 1: GET blood bank by ID does not mandate user coordinates -> distanceKm is null")
    void testGetBloodBankByIdWithoutCoordinatesReturnsNullDistance() throws Exception {
        BloodBank bank = createPersistedBloodBank("Privacy Test Bank", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.VERIFIED);

        mockMvc.perform(get("/api/v1/bloodbanks/" + bank.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bank.getId().toString())))
                .andExpect(jsonPath("$.name", is("Privacy Test Bank")))
                .andExpect(jsonPath("$.distanceKm").doesNotExist());
    }

    @Test
    @DisplayName("Location Privacy 2: GET blood bank by ID with optional coordinates computes distance without leaking coordinates in logs")
    void testGetBloodBankByIdWithCoordinatesComputesDistance() throws Exception {
        BloodBank bank = createPersistedBloodBank("Distance Calc Bank", "Mumbai", 18.9401, 72.8347, BloodBankVerificationStatus.VERIFIED);

        mockMvc.perform(get("/api/v1/bloodbanks/" + bank.getId())
                .param("userLat", "18.9400")
                .param("userLon", "72.8340"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bank.getId().toString())))
                .andExpect(jsonPath("$.distanceKm", notNullValue()));
    }
}

