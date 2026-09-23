package org.netra.features.fulfillment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodbank.entity.*;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.fulfillment.dto.CancelFulfillmentRequest;
import org.netra.features.fulfillment.dto.CreateFulfillmentRequest;
import org.netra.features.fulfillment.dto.FailFulfillmentRequest;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.NotificationReferenceType;
import org.netra.features.notification.entity.NotificationType;
import org.netra.features.notification.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FulfillmentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FulfillmentRepository fulfillmentRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonorProfileRepository donorProfileRepository;
    @Autowired private BloodBankRepository bloodBankRepository;
    @Autowired private BloodBankAccountRepository bloodBankAccountRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    private User requester;
    private String tokenRequester;
    private User donor;
    private String tokenDonor;
    private User staff;
    private String tokenStaff;
    private User admin;
    private String tokenAdmin;

    private BloodBank bloodBank;
    private BloodRequest bloodRequest;
    private Donation donation;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        donationRepository.deleteAllInBatch();
        bloodRequestRepository.deleteAllInBatch();
        donorProfileRepository.deleteAllInBatch();
        bloodBankAccountRepository.deleteAllInBatch();
        bloodBankRepository.deleteAllInBatch();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        requester = userRepository.save(new User("Requester", "req.it." + suffix + "@test.org", "+91981" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_RECEIVER)));
        tokenRequester = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        donor = userRepository.save(new User("Donor", "donor.it." + suffix + "@test.org", "+91982" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_DONOR)));
        tokenDonor = jwtTokenProvider.generateAccessToken(donor.getId(), List.of("ROLE_DONOR"));

        DonorProfile profile = new DonorProfile();
        profile.setUserId(donor.getId());
        profile.setBloodGroup(BloodGroup.O_POSITIVE);
        donorProfileRepository.save(profile);

        staff = userRepository.save(new User("Staff Bank", "staff.it." + suffix + "@test.org", "+91983" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_BLOODBANK)));
        tokenStaff = jwtTokenProvider.generateAccessToken(staff.getId(), List.of("ROLE_BLOODBANK"));

        admin = userRepository.save(new User("Admin", "admin.it." + suffix + "@test.org", "+91984" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_ADMIN)));
        tokenAdmin = jwtTokenProvider.generateAccessToken(admin.getId(), List.of("ROLE_ADMIN"));

        bloodBank = bloodBankRepository.save(new BloodBank(
                "City Blood Bank", "LIC-IT-" + suffix, "Main St", "City", "State", "100001",
                20.0, 80.0, "1234567890", "cb.it." + suffix + "@test.org",
                BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN
        ));

        bloodBankAccountRepository.save(new BloodBankAccount(staff.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE));

        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUnitsFulfilled(0);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City Clinic");
        req.setHospitalAddress("Clinic Rd");
        req.setCity("City");
        req.setState("State");
        req.setPostalCode("100001");
        req.setLatitude(20.0);
        req.setLongitude(80.0);
        req.setRequiredBy(Instant.now().plusSeconds(86400));
        bloodRequest = bloodRequestRepository.save(req);

        donation = new Donation(donor.getId(), DonationSourceType.BLOOD_REQUEST, bloodRequest.getId(), null, LocalDate.now(), "Notes");
        donation.setVerificationStatus(DonationVerificationStatus.VERIFIED);
        donation = donationRepository.save(donation);
    }

    @Test
    @DisplayName("End-to-End: Create, Start, Complete Fulfillment with Notifications and Request Completion")
    void fullFulfillmentLifecycleHappyPath() throws Exception {
        // 1. Create Fulfillment (POST /api/v1/fulfillments)
        CreateFulfillmentRequest createReq = new CreateFulfillmentRequest(bloodRequest.getId(), donation.getId(), 1, "Delivering to ICU");
        String createResponse = mockMvc.perform(post("/api/v1/fulfillments")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.units").value(1))
                .andExpect(jsonPath("$.bloodRequestId").value(bloodRequest.getId().toString()))
                .andExpect(jsonPath("$.donationId").value(donation.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        String fulfillmentIdStr = objectMapper.readTree(createResponse).get("id").asText();
        UUID fulfillmentId = UUID.fromString(fulfillmentIdStr);

        // Check FULFILLMENT_CREATED notification sent to requester and donor
        List<Notification> createdNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(requester.getId(), Pageable.unpaged()).getContent();
        assertTrue(createdNotifs.stream().anyMatch(n -> n.getType() == NotificationType.FULFILLMENT_CREATED));

        // 2. Start Fulfillment (POST /api/v1/fulfillments/{id}/start)
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillmentId + "/start")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());

        // 3. Complete Fulfillment (POST /api/v1/fulfillments/{id}/complete)
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillmentId + "/complete")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        // 4. Verify BloodRequest is marked FULFILLED atomically
        BloodRequest updatedReq = bloodRequestRepository.findById(bloodRequest.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.FULFILLED, updatedReq.getStatus());
        assertEquals(1, updatedReq.getUnitsFulfilled());
        assertNotNull(updatedReq.getFulfilledAt());
        assertEquals(staff.getId(), updatedReq.getFulfilledBy());

        // 5. Verify FULFILLMENT_COMPLETED notification
        List<Notification> donorNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(donor.getId(), Pageable.unpaged()).getContent();
        assertTrue(donorNotifs.stream().anyMatch(n -> n.getType() == NotificationType.FULFILLMENT_COMPLETED));
    }

    @Test
    @DisplayName("End-to-End: Start then Fail Fulfillment releases reservation")
    void startThenFailFulfillment() throws Exception {
        Fulfillment f = new Fulfillment(bloodRequest.getId(), donation.getId(), 1, staff.getId(), "Notes");
        f = fulfillmentRepository.save(f);

        // Start
        mockMvc.perform(post("/api/v1/fulfillments/" + f.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Fail
        FailFulfillmentRequest failReq = new FailFulfillmentRequest("Clot detected in unit", "Discarded");
        mockMvc.perform(post("/api/v1/fulfillments/" + f.getId() + "/fail")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Clot detected in unit"));

        // Verify request remains OPEN and unitsFulfilled is 0
        BloodRequest req = bloodRequestRepository.findById(bloodRequest.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.OPEN, req.getStatus());
        assertEquals(0, req.getUnitsFulfilled());

        // Verify notification
        List<Notification> reqNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(requester.getId(), Pageable.unpaged()).getContent();
        assertTrue(reqNotifs.stream().anyMatch(n -> n.getType() == NotificationType.FULFILLMENT_FAILED));
    }

    @Test
    @DisplayName("End-to-End: Cancel Fulfillment in READY status")
    void cancelFulfillmentInReadyStatus() throws Exception {
        Fulfillment f = new Fulfillment(bloodRequest.getId(), donation.getId(), 1, requester.getId(), "Notes");
        f = fulfillmentRepository.save(f);

        CancelFulfillmentRequest cancelReq = new CancelFulfillmentRequest("Requester arranged family donor", null);
        mockMvc.perform(post("/api/v1/fulfillments/" + f.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenRequester)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Requester arranged family donor"));
    }

    @Test
    @DisplayName("Queries: GET /my, GET /pending, and GET /{id}")
    void queryEndpoints() throws Exception {
        Fulfillment f = new Fulfillment(bloodRequest.getId(), donation.getId(), 1, staff.getId(), "Notes");
        f = fulfillmentRepository.save(f);

        // GET /my for staff
        mockMvc.perform(get("/api/v1/fulfillments/my")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(f.getId().toString()));

        // GET /pending for staff
        mockMvc.perform(get("/api/v1/fulfillments/pending")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(f.getId().toString()));

        // GET /{id} for requester
        mockMvc.perform(get("/api/v1/fulfillments/" + f.getId())
                        .header("Authorization", "Bearer " + tokenRequester))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(f.getId().toString()))
                .andExpect(jsonPath("$.hospitalName").value("City Clinic"))
                .andExpect(jsonPath("$.requestedBloodGroup").value("O+"));
    }
}
