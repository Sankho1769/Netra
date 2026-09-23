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
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.notification.repository.NotificationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FulfillmentSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FulfillmentRepository fulfillmentRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonorProfileRepository donorProfileRepository;
    @Autowired private BloodBankRepository bloodBankRepository;
    @Autowired private BloodBankAccountRepository bloodBankAccountRepository;
    @Autowired private org.netra.features.events.repository.DonationEventRepository donationEventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
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
    private User outsider;
    private String tokenOutsider;
    private User unassignedStaff;
    private String tokenUnassignedStaff;
    private User otherStaff;
    private String tokenOtherStaff;

    private BloodBank bloodBank;
    private BloodBank otherBank;
    private BloodRequest bloodRequest;
    private Donation donation;
    private Fulfillment fulfillment;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        donationRepository.deleteAllInBatch();
        donationEventRepository.deleteAllInBatch();
        bloodRequestRepository.deleteAllInBatch();
        donorProfileRepository.deleteAllInBatch();
        bloodBankAccountRepository.deleteAllInBatch();
        bloodBankRepository.deleteAllInBatch();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        requester = userRepository.save(new User("Requester", "req_" + suffix + "@test.org", "+91981" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_RECEIVER)));
        tokenRequester = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        donor = userRepository.save(new User("Donor", "donor_" + suffix + "@test.org", "+91982" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_DONOR)));
        tokenDonor = jwtTokenProvider.generateAccessToken(donor.getId(), List.of("ROLE_DONOR"));

        DonorProfile profile = new DonorProfile();
        profile.setUserId(donor.getId());
        profile.setBloodGroup(BloodGroup.O_POSITIVE);
        donorProfileRepository.save(profile);

        staff = userRepository.save(new User("Staff Bank", "staff_" + suffix + "@test.org", "+91983" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_BLOODBANK)));
        tokenStaff = jwtTokenProvider.generateAccessToken(staff.getId(), List.of("ROLE_BLOODBANK"));

        unassignedStaff = userRepository.save(new User("Unassigned Staff", "unassigned_" + suffix + "@test.org", "+91986" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_BLOODBANK)));
        tokenUnassignedStaff = jwtTokenProvider.generateAccessToken(unassignedStaff.getId(), List.of("ROLE_BLOODBANK"));

        otherStaff = userRepository.save(new User("Other Staff", "otherstaff_" + suffix + "@test.org", "+91987" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_BLOODBANK)));
        tokenOtherStaff = jwtTokenProvider.generateAccessToken(otherStaff.getId(), List.of("ROLE_BLOODBANK"));

        admin = userRepository.save(new User("Admin", "admin_" + suffix + "@test.org", "+91984" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_ADMIN)));
        tokenAdmin = jwtTokenProvider.generateAccessToken(admin.getId(), List.of("ROLE_ADMIN"));

        outsider = userRepository.save(new User("Outsider", "out_" + suffix + "@test.org", "+91985" + suffix, passwordEncoder.encode("Pass1!"), Set.of(UserRole.ROLE_RECEIVER)));
        tokenOutsider = jwtTokenProvider.generateAccessToken(outsider.getId(), List.of("ROLE_RECEIVER"));

        bloodBank = bloodBankRepository.save(new BloodBank(
                "Central Bank", "LIC-" + suffix, "Street 1", "City", "State", "100001",
                20.0, 80.0, "1234567890", "cb_" + suffix + "@test.org",
                BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN
        ));
        bloodBankAccountRepository.save(new BloodBankAccount(staff.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE));

        otherBank = bloodBankRepository.save(new BloodBank(
                "Other Bank", "LIC-OTH-" + suffix, "Street 2", "City 2", "State 2", "100002",
                21.0, 81.0, "1234567891", "ob_" + suffix + "@test.org",
                BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN
        ));
        bloodBankAccountRepository.save(new BloodBankAccount(otherStaff.getId(), otherBank.getId(), BloodBankAccountStatus.ACTIVE));

        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
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

        fulfillment = new Fulfillment(bloodRequest.getId(), donation.getId(), 1, staff.getId(), "Initial fulfillment");
        fulfillment = fulfillmentRepository.save(fulfillment);
    }

    @Test
    @DisplayName("Security: Donors cannot start, complete, or fail fulfillment (Rule 9)")
    void donorsCannotOperateFulfillment() throws Exception {
        // Start attempt by donor -> 403 Forbidden
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenDonor))
                .andExpect(status().isForbidden());

        // Complete attempt by donor -> 403 Forbidden
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenDonor))
                .andExpect(status().isForbidden());

        // Fail attempt by donor -> 403 Forbidden
        FailFulfillmentRequest failReq = new FailFulfillmentRequest("Fake fail", null);
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/fail")
                        .header("Authorization", "Bearer " + tokenDonor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Receivers cannot start, complete, or fail fulfillment (Rule 9)")
    void receiversCannotOperateFulfillment() throws Exception {
        // Start attempt by receiver -> 403 Forbidden
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenRequester))
                .andExpect(status().isForbidden());

        // Complete attempt by receiver -> 403 Forbidden
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenRequester))
                .andExpect(status().isForbidden());

        // Fail attempt by receiver -> 403 Forbidden
        FailFulfillmentRequest failReq = new FailFulfillmentRequest("Fake fail", null);
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/fail")
                        .header("Authorization", "Bearer " + tokenRequester)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Non-staff cannot access pending fulfillment queue")
    void nonStaffCannotAccessPendingQueue() throws Exception {
        mockMvc.perform(get("/api/v1/fulfillments/pending")
                        .header("Authorization", "Bearer " + tokenDonor))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/fulfillments/pending")
                        .header("Authorization", "Bearer " + tokenRequester))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: IDOR protection on GET /{id}")
    void idorProtectionOnFulfillmentDetails() throws Exception {
        // Outsider cannot access fulfillment -> 403 Forbidden
        mockMvc.perform(get("/api/v1/fulfillments/" + fulfillment.getId())
                        .header("Authorization", "Bearer " + tokenOutsider))
                .andExpect(status().isForbidden());

        // Requester CAN access fulfillment for their request
        mockMvc.perform(get("/api/v1/fulfillments/" + fulfillment.getId())
                        .header("Authorization", "Bearer " + tokenRequester))
                .andExpect(status().isOk());

        // Donor CAN access fulfillment utilizing their donation
        mockMvc.perform(get("/api/v1/fulfillments/" + fulfillment.getId())
                        .header("Authorization", "Bearer " + tokenDonor))
                .andExpect(status().isOk());

        // Staff CAN access fulfillment
        mockMvc.perform(get("/api/v1/fulfillments/" + fulfillment.getId())
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Clinical staff can start and complete fulfillment")
    void staffCanStartAndCompleteFulfillment() throws Exception {
        // 1. Staff starts fulfillment
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 2. Staff completes fulfillment
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    @DisplayName("Security: Unassigned staff cannot operate, create, or view fulfillment queue")
    void unassignedStaffCannotPerformClinicalOperations() throws Exception {
        // 1. Unassigned staff cannot start fulfillment -> 403 Forbidden
        mockMvc.perform(post("/api/v1/fulfillments/" + fulfillment.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenUnassignedStaff))
                .andExpect(status().isForbidden());

        // 2. Unassigned staff cannot view pending queue -> 403 Forbidden
        mockMvc.perform(get("/api/v1/fulfillments/pending")
                        .header("Authorization", "Bearer " + tokenUnassignedStaff))
                .andExpect(status().isForbidden());

        // 3. Unassigned staff cannot create fulfillment -> 403 Forbidden
        CreateFulfillmentRequest createReq = new CreateFulfillmentRequest(bloodRequest.getId(), donation.getId(), 1, "Unassigned test");
        mockMvc.perform(post("/api/v1/fulfillments")
                        .header("Authorization", "Bearer " + tokenUnassignedStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Resource-level scoping prevents staff from another blood bank operating event donation fulfillment")
    void eventDonationFulfillmentRestrictedToHostBloodBank() throws Exception {
        // Create donation camp event hosted by bloodBank (Bank 1)
        org.netra.features.events.entity.DonationEvent event = new org.netra.features.events.entity.DonationEvent();
        event.setBloodBankId(bloodBank.getId());
        event.setTitle("Community Blood Drive");
        event.setDescription("Annual Drive");
        event.setVenueName("Community Hall");
        event.setAddress("Hall Road");
        event.setCity("City");
        event.setState("State");
        event.setPostalCode("100001");
        event.setLatitude(20.0);
        event.setLongitude(80.0);
        event.setStartAt(Instant.now().minusSeconds(7200));
        event.setEndAt(Instant.now().plusSeconds(7200));
        event.setRegistrationOpenAt(Instant.now().minusSeconds(14400));
        event.setRegistrationCloseAt(Instant.now().plusSeconds(3600));
        event.setDonorCapacity(100);
        event.setCreatedBy(staff.getId());
        event = donationEventRepository.save(event);

        // Verified donation collected at Bank 1's event
        Donation eventDonation = new Donation(donor.getId(), DonationSourceType.DONATION_EVENT, null, event.getId(), LocalDate.now(), "Camp notes");
        eventDonation.setVerificationStatus(DonationVerificationStatus.VERIFIED);
        eventDonation = donationRepository.save(eventDonation);

        Fulfillment eventFulfillment = new Fulfillment(bloodRequest.getId(), eventDonation.getId(), 1, staff.getId(), "Event fulfillment");
        eventFulfillment = fulfillmentRepository.save(eventFulfillment);

        // otherStaff (Bank 2) attempts to start fulfillment for Bank 1's event donation -> 403 Forbidden
        mockMvc.perform(post("/api/v1/fulfillments/" + eventFulfillment.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenOtherStaff))
                .andExpect(status().isForbidden());

        // otherStaff (Bank 2) attempts to create fulfillment using Bank 1's event donation -> 403 Forbidden
        CreateFulfillmentRequest crossBankReq = new CreateFulfillmentRequest(bloodRequest.getId(), eventDonation.getId(), 1, "Cross bank claim");
        mockMvc.perform(post("/api/v1/fulfillments")
                        .header("Authorization", "Bearer " + tokenOtherStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossBankReq)))
                .andExpect(status().isForbidden());

        // staff (Bank 1, the event host) CAN start the fulfillment -> 200 OK
        mockMvc.perform(post("/api/v1/fulfillments/" + eventFulfillment.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
