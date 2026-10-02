package org.netra.features.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.events.dto.RegisterParticipantRequest;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventRegistration;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.repository.DonationEventRegistrationRepository;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DonationEventParticipantRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private DonationEventRepository donationEventRepository;

    @Autowired
    private DonationEventRegistrationRepository registrationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User createDonor(String name, String email) {
        User user = new User(name, email, "+9198" + String.format("%08d", Math.abs(email.hashCode()) % 100_000_000L),
                "hashed", Set.of(UserRole.ROLE_DONOR));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        return userRepository.save(user);
    }

    private DonationEvent createPublishedEvent(User organizer) {
        BloodBank bank = new BloodBank(
                "Camp Test Blood Bank " + UUID.randomUUID(),
                "REG-" + UUID.randomUUID().toString().substring(0, 8),
                "Camp Address",
                "Mumbai",
                "Maharashtra",
                "400001",
                19.0760,
                72.8777,
                "+919800000000",
                "bank@netra.org",
                BloodBankVerificationStatus.VERIFIED,
                BloodBankOperatingStatus.OPEN
        );
        bank = bloodBankRepository.save(bank);

        Instant now = Instant.now();
        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Annual Blood Camp " + UUID.randomUUID());
        event.setDescription("Camp Description");
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setVenueName("Community Hall");
        event.setAddress("Main Rd");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400001");
        event.setLatitude(19.0760);
        event.setLongitude(72.8777);
        event.setStartAt(now.plus(2, ChronoUnit.DAYS));
        event.setEndAt(now.plus(3, ChronoUnit.DAYS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(1, ChronoUnit.DAYS));
        event.setDonorCapacity(50);
        event.setCreatedBy(organizer.getId());
        return donationEventRepository.save(event);
    }

    @Test
    @DisplayName("Participant Form 1: Underage participant rejected with validation error")
    void testUnderageParticipantRejected() throws Exception {
        User donor = createDonor("Young Donor", "young." + UUID.randomUUID() + "@netra.org");
        DonationEvent event = createPublishedEvent(donor);
        String token = jwtTokenProvider.generateAccessToken(donor.getId(), Set.of("ROLE_DONOR"));

        RegisterParticipantRequest underageReq = new RegisterParticipantRequest(
                "Young Donor",
                LocalDate.now().minusYears(17), // 17 years old
                donor.getPhone(),
                donor.getEmail(),
                "O_POSITIVE",
                "123 Street",
                "Mumbai",
                "Parent Name",
                "+919811111111",
                true
        );

        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(underageReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Participant must be at least 18 years old to register for a donation camp.")));
    }

    @Test
    @DisplayName("Participant Form 2: Missing consent rejected")
    void testMissingConsentRejected() throws Exception {
        User donor = createDonor("Consent Donor", "noconsent." + UUID.randomUUID() + "@netra.org");
        DonationEvent event = createPublishedEvent(donor);
        String token = jwtTokenProvider.generateAccessToken(donor.getId(), Set.of("ROLE_DONOR"));

        RegisterParticipantRequest noConsentReq = new RegisterParticipantRequest(
                "Consent Donor",
                LocalDate.now().minusYears(25),
                donor.getPhone(),
                donor.getEmail(),
                "A_POSITIVE",
                "456 Avenue",
                "Mumbai",
                "Emergency Person",
                "+919822222222",
                false
        );

        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noConsentReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Participant Form 3: Valid participant form registered and persisted in database")
    void testValidParticipantFormRegisteredSuccessfully() throws Exception {
        User donor = createDonor("Eligible Donor", "eligible." + UUID.randomUUID() + "@netra.org");
        DonationEvent event = createPublishedEvent(donor);
        String token = jwtTokenProvider.generateAccessToken(donor.getId(), Set.of("ROLE_DONOR"));

        RegisterParticipantRequest validReq = new RegisterParticipantRequest(
                "Eligible Donor",
                LocalDate.of(1995, 5, 15),
                donor.getPhone(),
                donor.getEmail(),
                "B_POSITIVE",
                "789 Central Road",
                "Mumbai",
                "Guardian Name",
                "+919833333333",
                true
        );

        mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantName", is("Eligible Donor")))
                .andExpect(jsonPath("$.participantBloodGroup", is("B_POSITIVE")))
                .andExpect(jsonPath("$.consentConfirmed", is(true)));

        // Verify database persistence
        DonationEventRegistration reg = registrationRepository.findByEventIdAndDonorUserId(event.getId(), donor.getId()).orElseThrow();
        assertEquals("Eligible Donor", reg.getParticipantName());
        assertEquals(LocalDate.of(1995, 5, 15), reg.getParticipantDob());
        assertEquals("B_POSITIVE", reg.getParticipantBloodGroup());
        assertEquals("Mumbai", reg.getParticipantCity());
        assertEquals("Guardian Name", reg.getEmergencyContactName());
        assertEquals("+919833333333", reg.getEmergencyContactPhone());
        assertTrue(reg.isConsentConfirmed());
        assertNotNull(reg.getConsentTimestamp());
        assertEquals(DonationEventRegistrationStatus.REGISTERED, reg.getStatus());
    }
}
