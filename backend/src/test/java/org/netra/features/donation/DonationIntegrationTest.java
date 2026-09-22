package org.netra.features.donation;

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
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.dto.*;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.entity.DonorStatus;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
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
public class DonationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonationEventRepository donationEventRepository;

    @Autowired
    private org.netra.features.events.repository.DonationEventRegistrationRepository registrationRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User donorUser;
    private String donorToken;
    private User staffUser;
    private String staffToken;
    private User adminUser;
    private String adminToken;

    private BloodBank bloodBank;
    private BloodRequest bloodRequest;
    private DonationEvent donationEvent;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        donationRepository.deleteAll();
        donorMatchRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        registrationRepository.deleteAll();
        donationEventRepository.deleteAll();
        bloodBankAccountRepository.deleteAll();
        bloodBankRepository.deleteAll();
        donorProfileRepository.deleteAll();

        donorUser = userRepository.save(new User(
                "Integrated Donor",
                "donor.int." + UUID.randomUUID() + "@netra.org",
                "+919830000001",
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_DONOR)
        ));
        donorToken = jwtTokenProvider.generateAccessToken(donorUser.getId(), List.of("ROLE_DONOR"));

        DonorProfile dp = new DonorProfile(donorUser.getId(), BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        dp.setDonorStatus(DonorStatus.ACTIVE);
        donorProfileRepository.save(dp);

        staffUser = userRepository.save(new User(
                "Hospital Staff",
                "staff.int." + UUID.randomUUID() + "@netra.org",
                "+919830000002",
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_BLOODBANK)
        ));
        staffToken = jwtTokenProvider.generateAccessToken(staffUser.getId(), List.of("ROLE_BLOODBANK"));

        adminUser = userRepository.save(new User(
                "System Admin",
                "admin.int." + UUID.randomUUID() + "@netra.org",
                "+919830000003",
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_ADMIN)
        ));
        adminToken = jwtTokenProvider.generateAccessToken(adminUser.getId(), List.of("ROLE_ADMIN"));

        bloodBank = bloodBankRepository.save(new BloodBank(
                "Apex Blood Center",
                "REG-INT-100",
                "Park Street",
                "Kolkata",
                "WB",
                "700016",
                22.55,
                88.35,
                "+913322001122",
                "apex@netra.org",
                BloodBankVerificationStatus.VERIFIED,
                BloodBankOperatingStatus.OPEN
        ));

        bloodBankAccountRepository.save(new BloodBankAccount(
                staffUser.getId(),
                bloodBank.getId(),
                BloodBankAccountStatus.ACTIVE
        ));

        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(staffUser.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("Apex Hospital");
        req.setHospitalAddress("Park Street");
        req.setCity("Kolkata");
        req.setState("WB");
        req.setPostalCode("700016");
        req.setLatitude(22.55);
        req.setLongitude(88.35);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        bloodRequest = bloodRequestRepository.save(req);

        DonationEvent evt = new DonationEvent();
        evt.setBloodBankId(bloodBank.getId());
        evt.setTitle("Community Blood Drive");
        evt.setDescription("Annual drive");
        evt.setStatus(DonationEventStatus.PUBLISHED);
        evt.setVenueName("Community Hall");
        evt.setAddress("Park Street");
        evt.setCity("Kolkata");
        evt.setState("WB");
        evt.setPostalCode("700016");
        evt.setLatitude(22.55);
        evt.setLongitude(88.35);
        evt.setStartAt(Instant.now().minus(2, ChronoUnit.HOURS));
        evt.setEndAt(Instant.now().plus(4, ChronoUnit.HOURS));
        evt.setRegistrationOpenAt(Instant.now().minus(2, ChronoUnit.DAYS));
        evt.setRegistrationCloseAt(Instant.now().minus(3, ChronoUnit.HOURS));
        evt.setDonorCapacity(100);
        evt.setCreatedBy(staffUser.getId());
        donationEvent = donationEventRepository.save(evt);

        registrationRepository.save(new org.netra.features.events.entity.DonationEventRegistration(
                donationEvent.getId(),
                donorUser.getId(),
                org.netra.features.events.entity.DonationEventRegistrationStatus.REGISTERED
        ));
    }

    @Test
    @DisplayName("End-to-End: Donor claims donation, staff verifies, donor lastDonationDate updated")
    void testEndToEndDonorClaimAndVerification() throws Exception {
        // 1. DonorMatch ACCEPTED
        DonorMatch match = new DonorMatch(
                UUID.randomUUID(),
                bloodRequest.getId(),
                donorUser.getId(),
                MatchStatus.ACCEPTED,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        donorMatchRepository.save(match);

        // 2. Donor submits claim via POST /api/v1/donations
        CreateDonationClaimRequest claimReq = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Donated 1 unit at Apex Hospital"
        );

        String claimResponseJson = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.donorUserId").value(donorUser.getId().toString()))
                .andExpect(jsonPath("$.sourceType").value("BLOOD_REQUEST"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"))
                .andReturn().getResponse().getContentAsString();

        DonationDetailDto claimResponse = objectMapper.readValue(claimResponseJson, DonationDetailDto.class);
        UUID donationId = claimResponse.getId();

        // 3. Donor checks history via GET /api/v1/donations/my
        mockMvc.perform(get("/api/v1/donations/my")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(donationId.toString()))
                .andExpect(jsonPath("$.content[0].verificationStatus").value("PENDING_VERIFICATION"));

        // 4. Staff verifies donation via POST /api/v1/donations/{id}/verify
        VerifyDonationRequest verifyReq = new VerifyDonationRequest("Donor verified healthy and eligible");

        mockMvc.perform(post("/api/v1/donations/" + donationId + "/verify")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(donationId.toString()))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedByUserId").value(staffUser.getId().toString()));

        // 5. Verify DonorProfile lastDonationDate was authoritatively updated
        DonorProfile updatedProfile = donorProfileRepository.findByUserId(donorUser.getId()).orElseThrow();
        assertEquals(LocalDate.now(), updatedProfile.getLastDonationDate());

        // 6. Verify notification was recorded
        var notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(donorUser.getId(), Pageable.unpaged());
        assertTrue(notifications.stream().anyMatch(n -> n.getType().name().equals("DONATION_VERIFIED")));
    }

    @Test
    @DisplayName("End-to-End: Staff directly records verified donation (e.g. walk-in at camp)")
    void testDirectRecordVerifiedDonation() throws Exception {
        RecordVerifiedDonationRequest recordReq = new RecordVerifiedDonationRequest(
                donorUser.getId(),
                DonationSourceType.DONATION_EVENT,
                null,
                donationEvent.getId(),
                LocalDate.now(),
                "Successful camp donation"
        );

        mockMvc.perform(post("/api/v1/donations/record-verified")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donorUserId").value(donorUser.getId().toString()))
                .andExpect(jsonPath("$.sourceType").value("DONATION_EVENT"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedByUserId").value(staffUser.getId().toString()));

        // Authoritative lastDonationDate check
        DonorProfile updatedProfile = donorProfileRepository.findByUserId(donorUser.getId()).orElseThrow();
        assertEquals(LocalDate.now(), updatedProfile.getLastDonationDate());
    }

    @Test
    @DisplayName("End-to-End: Donor claims, staff rejects claim with feedback")
    void testStaffRejectionFlow() throws Exception {
        DonorMatch match = new DonorMatch(
                UUID.randomUUID(),
                bloodRequest.getId(),
                donorUser.getId(),
                MatchStatus.ACCEPTED,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        donorMatchRepository.save(match);

        CreateDonationClaimRequest claimReq = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Claim to be rejected"
        );

        String res = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        DonationDetailDto claim = objectMapper.readValue(res, DonationDetailDto.class);

        RejectDonationRequest rejectReq = new RejectDonationRequest("Hospital records show donor did not arrive for donation");

        mockMvc.perform(post("/api/v1/donations/" + claim.getId() + "/reject")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Hospital records show donor did not arrive for donation"));

        // Donor lastDonationDate MUST NOT be updated on rejection
        DonorProfile unchanged = donorProfileRepository.findByUserId(donorUser.getId()).orElseThrow();
        assertNull(unchanged.getLastDonationDate());

        // Notification of rejection
        var notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(donorUser.getId(), Pageable.unpaged());
        assertTrue(notifications.stream().anyMatch(n -> n.getType().name().equals("DONATION_REJECTED")));
    }

    @Test
    @DisplayName("End-to-End: Donor cancels pending claim")
    void testDonorCancelClaimFlow() throws Exception {
        DonorMatch match = new DonorMatch(
                UUID.randomUUID(),
                bloodRequest.getId(),
                donorUser.getId(),
                MatchStatus.ACCEPTED,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        donorMatchRepository.save(match);

        CreateDonationClaimRequest claimReq = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Accidental claim"
        );

        String res = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        DonationDetailDto claim = objectMapper.readValue(res, DonationDetailDto.class);

        mockMvc.perform(post("/api/v1/donations/" + claim.getId() + "/cancel")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("End-to-End: Admin correction on verified donation")
    void testAdminCorrectionFlow() throws Exception {
        RecordVerifiedDonationRequest recordReq = new RecordVerifiedDonationRequest(
                donorUser.getId(),
                DonationSourceType.DONATION_EVENT,
                null,
                donationEvent.getId(),
                LocalDate.now(),
                "Initial note"
        );

        String res = mockMvc.perform(post("/api/v1/donations/record-verified")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        DonationDetailDto claim = objectMapper.readValue(res, DonationDetailDto.class);

        AdminCorrectionRequest adminReq = new AdminCorrectionRequest(
                DonationVerificationStatus.VERIFIED,
                "Corrected by admin: valid donation"
        );

        mockMvc.perform(post("/api/v1/donations/" + claim.getId() + "/admin-correction")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
    }
}
