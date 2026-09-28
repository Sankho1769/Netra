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

    @Test
    @DisplayName("verifyHospital: resolves multi-region hospital (Kolkata) via external provider and caches it")
    void verifyHospital_ExternalProviderMultiRegion_Kolkata() {
        org.netra.features.hospital.provider.HospitalPlacesProvider placesProvider =
                mock(org.netra.features.hospital.provider.HospitalPlacesProvider.class);
        HospitalVerificationService serviceWithProvider =
                new HospitalVerificationService(verifiedHospitalRepository, bloodBankRepository, placesProvider);

        when(verifiedHospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        when(verifiedHospitalRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(bloodBankRepository.findAll()).thenReturn(Collections.emptyList());

        VerifiedHospitalDto providerDto = new VerifiedHospitalDto(
                null, "SSKM Hospital", "244 AJC Bose Road", "Kolkata", "West Bengal",
                "700020", "OSM-123456", true, "VERIFIED", null, 22.5398, 88.3426,
                "EXTERNAL_PROVIDER", "HOSPITAL"
        );

        when(placesProvider.searchHealthcarePlaces("SSKM Hospital", "Kolkata"))
                .thenReturn(List.of(providerDto));
        when(verifiedHospitalRepository.save(any(VerifiedHospital.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerifyHospitalRequest request = new VerifyHospitalRequest(
                "SSKM Hospital", "AJC Bose Road", "Kolkata", "West Bengal", null
        );

        HospitalVerificationResultDto result = serviceWithProvider.verifyHospital(request);

        assertNotNull(result);
        assertTrue(result.isVerified());
        assertEquals("VERIFIED", result.getVerificationStatus());
        assertEquals("SSKM Hospital", result.getHospitalName());
        assertEquals("Kolkata", result.getCity());
        assertEquals("EXTERNAL_PROVIDER", result.getSource());
        verify(verifiedHospitalRepository).save(any(VerifiedHospital.class));
    }

    @Test
    @DisplayName("verifyHospital: gracefully handles external provider failure and returns UNVERIFIED")
    void verifyHospital_ProviderFailureGracefulFallback() {
        org.netra.features.hospital.provider.HospitalPlacesProvider placesProvider =
                mock(org.netra.features.hospital.provider.HospitalPlacesProvider.class);
        HospitalVerificationService serviceWithProvider =
                new HospitalVerificationService(verifiedHospitalRepository, bloodBankRepository, placesProvider);

        when(verifiedHospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        when(verifiedHospitalRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(bloodBankRepository.findAll()).thenReturn(Collections.emptyList());
        when(placesProvider.searchHealthcarePlaces(anyString(), anyString()))
                .thenThrow(new RuntimeException("External service timeout"));

        VerifyHospitalRequest request = new VerifyHospitalRequest(
                "Unknown Rural Health Center", "Village Road", "Purulia", "West Bengal", null
        );

        HospitalVerificationResultDto result = serviceWithProvider.verifyHospital(request);

        assertNotNull(result);
        assertFalse(result.isVerified());
        assertEquals("UNVERIFIED", result.getVerificationStatus());
    }

    @Test
    @DisplayName("searchHospitals: combines internal registry and dynamic external provider results")
    void searchHospitals_CombinesRegistryAndProvider() {
        org.netra.features.hospital.provider.HospitalPlacesProvider placesProvider =
                mock(org.netra.features.hospital.provider.HospitalPlacesProvider.class);
        HospitalVerificationService serviceWithProvider =
                new HospitalVerificationService(verifiedHospitalRepository, bloodBankRepository, placesProvider);

        VerifiedHospital internalHosp = new VerifiedHospital(
                "AIIMS New Delhi", "Ansari Nagar", "New Delhi", "Delhi", "110029",
                28.5672, 77.2100, "PLACE-DEL-AIIMS", true, "VERIFIED", null,
                "INTERNAL_REGISTRY", "HOSPITAL"
        );
        when(verifiedHospitalRepository.searchHospitals("AIIMS", null))
                .thenReturn(List.of(internalHosp));
        when(bloodBankRepository.findAll()).thenReturn(Collections.emptyList());

        VerifiedHospitalDto extHosp = new VerifiedHospitalDto(
                null, "AIIMS Kalyani", "NH-34 Connector", "Kalyani", "West Bengal",
                "741245", "OSM-789012", true, "VERIFIED", null, 22.9750, 88.4344,
                "EXTERNAL_PROVIDER", "HOSPITAL"
        );
        when(placesProvider.searchHealthcarePlaces("AIIMS", null))
                .thenReturn(List.of(extHosp));

        List<VerifiedHospitalDto> results = serviceWithProvider.searchHospitals("AIIMS", null);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(h -> h.getCity().equals("New Delhi")));
        assertTrue(results.stream().anyMatch(h -> h.getCity().equals("Kalyani")));
    }
}
