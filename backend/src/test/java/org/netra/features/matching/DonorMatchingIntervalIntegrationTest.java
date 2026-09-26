package org.netra.features.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donor.entity.*;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.eligibility.entity.EligibilitySession;
import org.netra.features.eligibility.entity.ResultType;
import org.netra.features.eligibility.entity.SessionStatus;
import org.netra.features.eligibility.repository.EligibilitySessionRepository;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DonorMatchingIntervalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EligibilitySessionRepository eligibilitySessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

    private User requester;
    private String requesterToken;

    private static final double REQ_LAT = 18.9400;
    private static final double REQ_LNG = 72.8350;

    @BeforeEach
    void setUp() {
        eligibilitySessionRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();

        requester = createTestUser("req.integ", UserRole.ROLE_RECEIVER);
        requesterToken = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));
    }

    private User createTestUser(String namePrefix, UserRole role) {
        User user = new User(
                namePrefix + " FullName",
                namePrefix.toLowerCase() + "." + UUID.randomUUID() + "@netra.org",
                "+919800000000",
                passwordEncoder.encode("Pass123!"),
                Set.of(role)
        );
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private BloodRequest createBloodRequest() {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("KEM Hospital");
        req.setHospitalAddress("Parel");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400012");
        req.setLatitude(REQ_LAT);
        req.setLongitude(REQ_LNG);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());
        return bloodRequestRepository.save(req);
    }

    private DonorProfile createDonorProfile(User user, LocalDate lastDonationDate, String biologicalSex) {
        DonorProfile dp = new DonorProfile(user.getId(), BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        dp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        dp.setDonorStatus(DonorStatus.ACTIVE);
        dp.setLastDonationDate(lastDonationDate);
        dp.setBiologicalSex(biologicalSex);
        dp.setLatitude(18.9500);
        dp.setLongitude(72.8400);
        return donorProfileRepository.save(dp);
    }

    private EligibilitySession createEligibilitySession(User user, ResultType result, LocalDate estimatedEligibleDate) {
        EligibilitySession session = new EligibilitySession(user.getId(), "v1.0", Instant.now().plus(24, ChronoUnit.HOURS));
        session.setStatus(SessionStatus.COMPLETED);
        session.setResult(result);
        session.setEstimatedEligibleDate(estimatedEligibleDate);
        session.setCompletedAt(Instant.now());
        return eligibilitySessionRepository.save(session);
    }

    @Test
    @DisplayName("Matching Interval: Female donor donating 100 days ago is EXCLUDED (female interval is 120 days)")
    void testMatch_FemaleDonor_100DaysAgo_Excluded() throws Exception {
        BloodRequest req = createBloodRequest();

        User femaleDonor = createTestUser("FemaleDonor100", UserRole.ROLE_DONOR);
        createDonorProfile(femaleDonor, LocalDate.now().minusDays(100), "FEMALE");

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    @DisplayName("Matching Interval: Female donor donating 125 days ago is INCLUDED (>= 120 days)")
    void testMatch_FemaleDonor_125DaysAgo_Included() throws Exception {
        BloodRequest req = createBloodRequest();

        User femaleDonor = createTestUser("FemaleDonor125", UserRole.ROLE_DONOR);
        createDonorProfile(femaleDonor, LocalDate.now().minusDays(125), "FEMALE");

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1));
    }

    @Test
    @DisplayName("Matching Interval: Male donor donating 95 days ago is INCLUDED (male interval is 90 days)")
    void testMatch_MaleDonor_95DaysAgo_Included() throws Exception {
        BloodRequest req = createBloodRequest();

        User maleDonor = createTestUser("MaleDonor95", UserRole.ROLE_DONOR);
        createDonorProfile(maleDonor, LocalDate.now().minusDays(95), "MALE");

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1));
    }

    @Test
    @DisplayName("Matching Interval: Male donor donating 85 days ago is EXCLUDED (< 90 days)")
    void testMatch_MaleDonor_85DaysAgo_Excluded() throws Exception {
        BloodRequest req = createBloodRequest();

        User maleDonor = createTestUser("MaleDonor85", UserRole.ROLE_DONOR);
        createDonorProfile(maleDonor, LocalDate.now().minusDays(85), "MALE");

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    @DisplayName("Matching Pre-Screening: Temporary deferral with null estimatedEligibleDate is EXCLUDED")
    void testMatch_TemporaryDeferral_NullEstimatedEligibleDate_Excluded() throws Exception {
        BloodRequest req = createBloodRequest();

        User donor = createTestUser("IndefiniteDeferral", UserRole.ROLE_DONOR);
        createDonorProfile(donor, null, "MALE");
        createEligibilitySession(donor, ResultType.TEMPORARY_DEFERRAL, null);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    @DisplayName("Matching Pre-Screening: Temporary deferral whose estimatedEligibleDate has passed is INCLUDED")
    void testMatch_TemporaryDeferral_PastEstimatedEligibleDate_Included() throws Exception {
        BloodRequest req = createBloodRequest();

        User donor = createTestUser("ExpiredDeferral", UserRole.ROLE_DONOR);
        createDonorProfile(donor, null, "MALE");
        createEligibilitySession(donor, ResultType.TEMPORARY_DEFERRAL, LocalDate.now().minusDays(5));

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1));
    }

    @Test
    @DisplayName("Matching Pre-Screening: MEDICAL_REVIEW_REQUIRED donor is EXCLUDED from candidates")
    void testMatch_MedicalReviewRequired_Excluded() throws Exception {
        BloodRequest req = createBloodRequest();

        User donor = createTestUser("MedicalReviewDonor", UserRole.ROLE_DONOR);
        createDonorProfile(donor, null, "MALE");
        createEligibilitySession(donor, ResultType.MEDICAL_REVIEW_REQUIRED, null);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    @DisplayName("Matching Pre-Screening: IN_PROGRESS eligibility session does not bypass prior COMPLETED deferral")
    void testMatch_InProgressSessionDoesNotBypassCompletedDeferral() throws Exception {
        BloodRequest req = createBloodRequest();

        User donor = createTestUser("InProgressBypassAttempt", UserRole.ROLE_DONOR);
        createDonorProfile(donor, null, "MALE");

        // 1. Earlier completed session that deferred the donor to a future date (+30 days)
        EligibilitySession completedDeferral = new EligibilitySession(donor.getId(), "v1.0", Instant.now().plus(24, ChronoUnit.HOURS));
        completedDeferral.setStatus(SessionStatus.COMPLETED);
        completedDeferral.setResult(ResultType.TEMPORARY_DEFERRAL);
        completedDeferral.setEstimatedEligibleDate(LocalDate.now().plusDays(30));
        completedDeferral.setCompletedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        eligibilitySessionRepository.save(completedDeferral);

        // 2. Newer session that was started but is currently IN_PROGRESS (no result yet)
        EligibilitySession inProgressSession = new EligibilitySession(donor.getId(), "v1.0", Instant.now().plus(24, ChronoUnit.HOURS));
        inProgressSession.setStatus(SessionStatus.IN_PROGRESS);
        inProgressSession.setResult(null);
        eligibilitySessionRepository.save(inProgressSession);

        // The matching service must check the COMPLETED session and properly EXCLUDE the donor
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }
}
