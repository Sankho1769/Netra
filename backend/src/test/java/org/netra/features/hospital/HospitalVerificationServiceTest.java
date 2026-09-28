package org.netra.features.hospital;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.hospital.dto.HospitalVerificationResultDto;
import org.netra.features.hospital.dto.VerifiedHospitalDto;
import org.netra.features.hospital.dto.VerifyHospitalRequest;
import org.netra.features.hospital.entity.VerifiedHospital;
import org.netra.features.hospital.repository.VerifiedHospitalRepository;
import org.netra.features.hospital.service.HospitalVerificationService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HospitalVerificationServiceTest {

    @Mock
    private VerifiedHospitalRepository verifiedHospitalRepository;

    @Mock
    private BloodBankRepository bloodBankRepository;

    private HospitalVerificationService hospitalVerificationService;

    @BeforeEach
    void setUp() {
        hospitalVerificationService = new HospitalVerificationService(verifiedHospitalRepository, bloodBankRepository);
    }

    @Test
    @DisplayName("verifyHospital: rejects blacklisted or fraudulent hospital names")
    void verifyHospital_BlacklistedRejected() {
        VerifyHospitalRequest request = new VerifyHospitalRequest("Fake Clinic Center", "Street 1", "Mumbai", "Maharashtra", null);

        HospitalVerificationResultDto result = hospitalVerificationService.verifyHospital(request);

        assertNotNull(result);
        assertEquals("REJECTED", result.getVerificationStatus());
        assertFalse(result.isVerified());
        assertTrue(result.getMessage().contains("flag indicates non-existent or abusive"));
    }

    @Test
    @DisplayName("verifyHospital: successfully verifies via authoritative placeId")
    void verifyHospital_ByPlaceId() {
        VerifiedHospital vh = new VerifiedHospital(
                "KEM Hospital", "Acharya Donde Marg, Parel", "Mumbai", "Maharashtra", "400012",
                18.9998, 72.8427, "ChIJk68F3d3P5zsRg4n38bI4F0c", true, "VERIFIED", "+91 22 2410 7000"
        );

        when(verifiedHospitalRepository.findByPlaceId("ChIJk68F3d3P5zsRg4n38bI4F0c"))
                .thenReturn(Optional.of(vh));

        VerifyHospitalRequest request = new VerifyHospitalRequest(
                "KEM Hospital", "Parel", "Mumbai", "Maharashtra", "ChIJk68F3d3P5zsRg4n38bI4F0c"
        );

        HospitalVerificationResultDto result = hospitalVerificationService.verifyHospital(request);

        assertNotNull(result);
        assertTrue(result.isVerified());
        assertEquals("VERIFIED", result.getVerificationStatus());
        assertEquals("KEM Hospital", result.getHospitalName());
        assertEquals(18.9998, result.getLatitude());
        assertEquals(72.8427, result.getLongitude());
        assertTrue(result.getHasBloodBank());
    }

    @Test
    @DisplayName("verifyHospital: matches by hospital name and city")
    void verifyHospital_ByNameAndCity() {
        VerifiedHospital vh = new VerifiedHospital(
                "AIIMS New Delhi", "Sri Aurobindo Marg, Ansari Nagar", "New Delhi", "Delhi", "110029",
                28.5672, 77.2100, "ChIJjQk-UjHhDDkRWdD_8Jb8Q5s", true, "VERIFIED", "+91 11 2658 8500"
        );

        when(verifiedHospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase("AIIMS New Delhi", "New Delhi"))
                .thenReturn(Optional.of(vh));

        VerifyHospitalRequest request = new VerifyHospitalRequest(
                "AIIMS New Delhi", "Ansari Nagar", "New Delhi", "Delhi", null
        );

        HospitalVerificationResultDto result = hospitalVerificationService.verifyHospital(request);

        assertNotNull(result);
        assertTrue(result.isVerified());
        assertEquals("AIIMS New Delhi", result.getHospitalName());
        assertEquals(28.5672, result.getLatitude());
    }

    @Test
    @DisplayName("verifyHospital: falls back to UNVERIFIED for unknown places")
    void verifyHospital_UnknownPlaceUnverified() {
        when(verifiedHospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        when(verifiedHospitalRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(bloodBankRepository.findAll()).thenReturn(Collections.emptyList());

        VerifyHospitalRequest request = new VerifyHospitalRequest(
                "Community Polyclinic", "Main Road", "Jaipur", "Rajasthan", null
        );

        HospitalVerificationResultDto result = hospitalVerificationService.verifyHospital(request);

        assertNotNull(result);
        assertFalse(result.isVerified());
        assertEquals("UNVERIFIED", result.getVerificationStatus());
    }
}
