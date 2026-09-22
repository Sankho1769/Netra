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
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.dto.AdminCorrectionRequest;
import org.netra.features.donation.dto.RejectDonationRequest;
import org.netra.features.donation.dto.VerifyDonationRequest;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.repository.DonorMatchRepository;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DonationSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DonationRepository donationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorProfileRepository donorProfileRepository;
    @Autowired private BloodBankRepository bloodBankRepository;
    @Autowired private BloodBankAccountRepository bloodBankAccountRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonationEventRepository donationEventRepository;
    @Autowired private DonorMatchRepository donorMatchRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    private User donorA;
    private String tokenDonorA;
    private User donorB;
    private String tokenDonorB;
    private User staffBank1;
    private String tokenStaffBank1;
    private User admin;
    private String tokenAdmin;

    private BloodBank bank1;
    private BloodBank bank2;
    private DonationEvent eventBank2;
    private BloodRequest bloodRequest;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        donationRepository.deleteAllInBatch();
        donorMatchRepository.deleteAllInBatch();
        bloodRequestRepository.deleteAllInBatch();
        donationEventRepository.deleteAllInBatch();
        bloodBankAccountRepository.deleteAllInBatch();
        bloodBankRepository.deleteAllInBatch();
        donorProfileRepository.deleteAllInBatch();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        donorA = userRepository.save(new User("Donor Alpha", "alpha_" + suffix + "@test.org", "+91981" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_DONOR)));
        tokenDonorA = jwtTokenProvider.generateAccessToken(donorA.getId(), List.of("ROLE_DONOR"));

        DonorProfile profileA = new DonorProfile();
        profileA.setUserId(donorA.getId());
        profileA.setBloodGroup(BloodGroup.O_POSITIVE);
        donorProfileRepository.save(profileA);

        donorB = userRepository.save(new User("Donor Beta", "beta_" + suffix + "@test.org", "+91982" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_DONOR)));
        tokenDonorB = jwtTokenProvider.generateAccessToken(donorB.getId(), List.of("ROLE_DONOR"));

        DonorProfile profileB = new DonorProfile();
        profileB.setUserId(donorB.getId());
        profileB.setBloodGroup(BloodGroup.A_POSITIVE);
        donorProfileRepository.save(profileB);

        staffBank1 = userRepository.save(new User("Staff Bank 1", "staff1_" + suffix + "@test.org", "+91983" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_BLOODBANK)));
        tokenStaffBank1 = jwtTokenProvider.generateAccessToken(staffBank1.getId(), List.of("ROLE_BLOODBANK"));

        admin = userRepository.save(new User("Admin User", "admin_" + suffix + "@test.org", "+91984" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_ADMIN)));
        tokenAdmin = jwtTokenProvider.generateAccessToken(admin.getId(), List.of("ROLE_ADMIN"));

        bank1 = bloodBankRepository.save(new BloodBank("Bank One", "LIC-1-" + suffix, "St 1", "City", "State", "100001", 20.0, 80.0, "1234567890", "b1_" + suffix + "@test.org", BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN));
        bank2 = bloodBankRepository.save(new BloodBank("Bank Two", "LIC-2-" + suffix, "St 2", "City", "State", "100001", 20.0, 80.0, "1234567891", "b2_" + suffix + "@test.org", BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN));

        bloodBankAccountRepository.save(new BloodBankAccount(staffBank1.getId(), bank1.getId(), BloodBankAccountStatus.ACTIVE));

        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(admin.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setHospitalName("City Clinic");
        req.setHospitalAddress("Clinic Rd");
        req.setCity("City");
        req.setState("State");
        req.setPostalCode("100001");
        req.setLatitude(20.0);
        req.setLongitude(80.0);
        req.setRequiredBy(Instant.now().plusSeconds(86400));
        bloodRequest = bloodRequestRepository.save(req);

        DonationEvent evt = new DonationEvent();
        evt.setBloodBankId(bank2.getId());
        evt.setTitle("Bank 2 Drive");
        evt.setVenueName("Hall 2");
        evt.setAddress("Addr 2");
        evt.setCity("City");
        evt.setState("State");
        evt.setPostalCode("100001");
        evt.setLatitude(20.0);
        evt.setLongitude(80.0);
        evt.setStartAt(Instant.now().plusSeconds(86400));
        evt.setEndAt(Instant.now().plusSeconds(172800));
        evt.setRegistrationOpenAt(Instant.now().minusSeconds(86400));
        evt.setRegistrationCloseAt(Instant.now().plusSeconds(86400));
        evt.setDonorCapacity(100);
        evt.setCreatedBy(admin.getId());
        evt.setStatus(DonationEventStatus.PUBLISHED);
        eventBank2 = donationEventRepository.save(evt);
    }

    @Test
    @DisplayName("Security: Unauthenticated access returns 401 Unauthorized")
    void testUnauthenticated_Returns401() throws Exception {
        UUID donationId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/donations/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/donations/" + donationId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/donations/pending")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/verify")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/reject")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/admin-correction")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Normal donor cannot access pending verification queue (403 Forbidden)")
    void testNormalDonor_CannotAccessPendingQueue_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/donations/pending")
                        .header("Authorization", "Bearer " + tokenDonorA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Normal donor cannot verify or reject donations (403 Forbidden)")
    void testNormalDonor_CannotVerifyOrReject_Returns403() throws Exception {
        Donation donation = donationRepository.save(new Donation(
                donorB.getId(),
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Notes"
        ));

        // Donor A tries to verify
        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/verify")
                        .header("Authorization", "Bearer " + tokenDonorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyDonationRequest("hacked"))))
                .andExpect(status().isForbidden());

        // Donor A tries to reject
        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/reject")
                        .header("Authorization", "Bearer " + tokenDonorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectDonationRequest("rejected by donor"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Normal donor cannot call admin correction (403 Forbidden)")
    void testNormalDonor_CannotCallAdminCorrection_Returns403() throws Exception {
        Donation donation = donationRepository.save(new Donation(
                donorB.getId(),
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Notes"
        ));

        AdminCorrectionRequest req = new AdminCorrectionRequest(DonationVerificationStatus.VERIFIED, "fake correction");

        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/admin-correction")
                        .header("Authorization", "Bearer " + tokenDonorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: IDOR/BOLA - Donor cannot view details of another donor's donation (403 Forbidden)")
    void testIdor_DonorCannotViewOtherDonorDonation_Returns403() throws Exception {
        Donation donationB = donationRepository.save(new Donation(
                donorB.getId(),
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Confidential Notes"
        ));

        // Donor A attempts to view Donor B's donation
        mockMvc.perform(get("/api/v1/donations/" + donationB.getId())
                        .header("Authorization", "Bearer " + tokenDonorA))
                .andExpect(status().isForbidden());

        // Donor B can view their own donation
        mockMvc.perform(get("/api/v1/donations/" + donationB.getId())
                        .header("Authorization", "Bearer " + tokenDonorB))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: IDOR - Donor cannot cancel another donor's donation claim (403 Forbidden)")
    void testIdor_DonorCannotCancelOtherDonorClaim_Returns403() throws Exception {
        Donation donationB = donationRepository.save(new Donation(
                donorB.getId(),
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Notes"
        ));

        // Donor A tries to cancel Donor B's claim
        mockMvc.perform(post("/api/v1/donations/" + donationB.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenDonorA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Staff of Bank 1 cannot verify donation for Bank 2's event (403 Forbidden)")
    void testBloodBankStaff_CannotVerifyEventOfAnotherBank_Returns403() throws Exception {
        Donation eventDonation = donationRepository.save(new Donation(
                donorA.getId(),
                DonationSourceType.DONATION_EVENT,
                null,
                eventBank2.getId(),
                LocalDate.now(),
                "Event claim"
        ));

        // Staff of Bank 1 tries to verify Bank 2's event donation
        mockMvc.perform(post("/api/v1/donations/" + eventDonation.getId() + "/verify")
                        .header("Authorization", "Bearer " + tokenStaffBank1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyDonationRequest("unauthorized staff"))))
                .andExpect(status().isForbidden());

        // Admin can verify
        mockMvc.perform(post("/api/v1/donations/" + eventDonation.getId() + "/verify")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyDonationRequest("admin verified"))))
                .andExpect(status().isOk());
    }
}
