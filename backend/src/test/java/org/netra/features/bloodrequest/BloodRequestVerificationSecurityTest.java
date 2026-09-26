package org.netra.features.bloodrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.dto.VerifyBloodRequestDto;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.entity.BloodRequestVerificationStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BloodRequestVerificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User requester;
    private User bloodBankUser;
    private User adminUser;
    private User unauthorizedReceiver;
    private User bankAndRequesterUser;

    @BeforeEach
    void setUp() {
        bloodRequestRepository.deleteAll();

        requester = createTestUser("requester", UserRole.ROLE_RECEIVER);
        bloodBankUser = createTestUser("bloodbank", UserRole.ROLE_BLOODBANK);
        adminUser = createTestUser("admin", UserRole.ROLE_ADMIN);
        unauthorizedReceiver = createTestUser("unauth", UserRole.ROLE_RECEIVER);
        bankAndRequesterUser = createTestUser("dualrole", UserRole.ROLE_RECEIVER, UserRole.ROLE_BLOODBANK);
    }

    private User createTestUser(String prefix, UserRole... roles) {
        User user = new User(
                prefix + " User",
                prefix.toLowerCase() + "." + UUID.randomUUID() + "@netra.org",
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

    private BloodRequest createSampleBloodRequest(User owner, BloodRequestStatus status) {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(owner.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.URGENT);
        req.setStatus(status);
        req.setHospitalName("City General Hospital");
        req.setHospitalAddress("456 Care Lane");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9400);
        req.setLongitude(72.8350);
        req.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        req.setDescription("Medical emergency blood need");
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());
        return bloodRequestRepository.save(req);
    }

    @Test
    @DisplayName("Verification: ROLE_BLOODBANK can verify OPEN blood request with audit trail")
    void testVerifyRequest_AsBloodBank_Success() throws Exception {
        BloodRequest request = createSampleBloodRequest(requester, BloodRequestStatus.OPEN);

        VerifyBloodRequestDto dto = new VerifyBloodRequestDto();
        dto.setDecision(BloodRequestVerificationStatus.VERIFIED);
        dto.setReason("Hospital admission slip verified and authenticated.");

        mockMvc.perform(post("/api/v1/blood-requests/" + request.getId() + "/verify")
                        .header("Authorization", "Bearer " + getAccessToken(bloodBankUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId().toString()))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedBy").value(bloodBankUser.getId().toString()))
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty())
                .andExpect(jsonPath("$.verificationNotes").value("Hospital admission slip verified and authenticated."));

        BloodRequest updated = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(BloodRequestVerificationStatus.VERIFIED, updated.getVerificationStatus());
        assertEquals(bloodBankUser.getId(), updated.getVerifiedBy());
        assertNotNull(updated.getVerifiedAt());
        assertEquals("Hospital admission slip verified and authenticated.", updated.getVerificationNotes());
    }

    @Test
    @DisplayName("Verification: ROLE_ADMIN can reject blood request with audit notes")
    void testVerifyRequest_AsAdmin_Success() throws Exception {
        BloodRequest request = createSampleBloodRequest(requester, BloodRequestStatus.OPEN);

        VerifyBloodRequestDto dto = new VerifyBloodRequestDto();
        dto.setDecision(BloodRequestVerificationStatus.REJECTED);
        dto.setReason("Hospital documentation is unverified or forged.");

        mockMvc.perform(post("/api/v1/blood-requests/" + request.getId() + "/verify")
                        .header("Authorization", "Bearer " + getAccessToken(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId().toString()))
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.verifiedBy").value(adminUser.getId().toString()))
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty())
                .andExpect(jsonPath("$.verificationNotes").value("Hospital documentation is unverified or forged."));

        BloodRequest updated = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(BloodRequestVerificationStatus.REJECTED, updated.getVerificationStatus());
    }

    @Test
    @DisplayName("Verification: Unauthorized role (ROLE_RECEIVER) is rejected with 403 Forbidden")
    void testVerifyRequest_AsUnauthorizedReceiver_Forbidden() throws Exception {
        BloodRequest request = createSampleBloodRequest(requester, BloodRequestStatus.OPEN);

        VerifyBloodRequestDto dto = new VerifyBloodRequestDto();
        dto.setDecision(BloodRequestVerificationStatus.VERIFIED);
        dto.setReason("Attempt unauthorized verification.");

        mockMvc.perform(post("/api/v1/blood-requests/" + request.getId() + "/verify")
                        .header("Authorization", "Bearer " + getAccessToken(unauthorizedReceiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verification: Unauthenticated caller is rejected with 401/403")
    void testVerifyRequest_Unauthenticated_Rejected() throws Exception {
        BloodRequest request = createSampleBloodRequest(requester, BloodRequestStatus.OPEN);

        VerifyBloodRequestDto dto = new VerifyBloodRequestDto();
        dto.setDecision(BloodRequestVerificationStatus.VERIFIED);
        dto.setReason("Attempt anonymous verification.");

        mockMvc.perform(post("/api/v1/blood-requests/" + request.getId() + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anti-fraud: Requester cannot verify their own blood request even if possessing ROLE_BLOODBANK")
    void testVerifyRequest_AntiFraud_RequesterCannotVerifyOwnRequest() throws Exception {
        BloodRequest ownRequest = createSampleBloodRequest(bankAndRequesterUser, BloodRequestStatus.OPEN);

        VerifyBloodRequestDto dto = new VerifyBloodRequestDto();
        dto.setDecision(BloodRequestVerificationStatus.VERIFIED);
        dto.setReason("Self-verifying own request.");

        mockMvc.perform(post("/api/v1/blood-requests/" + ownRequest.getId() + "/verify")
                        .header("Authorization", "Bearer " + getAccessToken(bankAndRequesterUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Requesters are not permitted to verify their own blood requests")));
    }

    @Test
    @DisplayName("Verification: Verifying non-OPEN blood request (e.g. CANCELLED) is rejected with 400 Bad Request")
    void testVerifyRequest_NonOpenStatus_Rejected() throws Exception {
        BloodRequest request = createSampleBloodRequest(requester, BloodRequestStatus.CANCELLED);

        VerifyBloodRequestDto dto = new VerifyBloodRequestDto();
        dto.setDecision(BloodRequestVerificationStatus.VERIFIED);
        dto.setReason("Verifying cancelled request.");

        mockMvc.perform(post("/api/v1/blood-requests/" + request.getId() + "/verify")
                        .header("Authorization", "Bearer " + getAccessToken(bloodBankUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only OPEN blood requests can be verified")));
    }

    @Test
    @DisplayName("Verification: UNVERIFIED decision payload is rejected with 400 Bad Request")
    void testVerifyRequest_InvalidDecision_Rejected() throws Exception {
        BloodRequest request = createSampleBloodRequest(requester, BloodRequestStatus.OPEN);

        VerifyBloodRequestDto dto = new VerifyBloodRequestDto();
        dto.setDecision(BloodRequestVerificationStatus.UNVERIFIED);
        dto.setReason("Setting back to unverified.");

        mockMvc.perform(post("/api/v1/blood-requests/" + request.getId() + "/verify")
                        .header("Authorization", "Bearer " + getAccessToken(bloodBankUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("A valid verification decision")));
    }
}
