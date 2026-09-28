package org.netra.features.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.metrics.dto.CommunityImpactDto;
import org.netra.features.metrics.service.CommunityMetricsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityMetricsServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private BloodRequestRepository bloodRequestRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    private CommunityMetricsService communityMetricsService;

    @BeforeEach
    void setUp() {
        communityMetricsService = new CommunityMetricsService(donationRepository, bloodRequestRepository, donorProfileRepository);
    }

    @Test
    @DisplayName("getCommunityImpact: returns clean empty state notice when zero verified donations exist")
    void getCommunityImpact_ZeroDataNotice() {
        when(donationRepository.countByVerificationStatus(DonationVerificationStatus.VERIFIED)).thenReturn(0L);
        when(bloodRequestRepository.countByStatus(BloodRequestStatus.FULFILLED)).thenReturn(0L);
        when(donorProfileRepository.count()).thenReturn(10L);

        CommunityImpactDto dto = communityMetricsService.getCommunityImpact();

        assertNotNull(dto);
        assertFalse(dto.isHasData());
        assertEquals(0L, dto.getTotalVerifiedDonations());
        assertEquals(0L, dto.getTotalUnitsCollected());
        assertEquals(0L, dto.getFulfilledRequestsCount());
        assertEquals(10L, dto.getActiveDonorsCount());
        assertEquals("Community impact data will appear here once verified donations are recorded.", dto.getNotice());
    }

    @Test
    @DisplayName("getCommunityImpact: calculates metrics accurately when verified donations exist")
    void getCommunityImpact_WithData() {
        when(donationRepository.countByVerificationStatus(DonationVerificationStatus.VERIFIED)).thenReturn(42L);
        when(bloodRequestRepository.countByStatus(BloodRequestStatus.FULFILLED)).thenReturn(35L);
        when(donorProfileRepository.count()).thenReturn(150L);

        CommunityImpactDto dto = communityMetricsService.getCommunityImpact();

        assertNotNull(dto);
        assertTrue(dto.isHasData());
        assertEquals(42L, dto.getTotalVerifiedDonations());
        assertEquals(42L, dto.getTotalUnitsCollected());
        assertEquals(35L, dto.getFulfilledRequestsCount());
        assertEquals(150L, dto.getActiveDonorsCount());
        assertNull(dto.getNotice());
    }
}
