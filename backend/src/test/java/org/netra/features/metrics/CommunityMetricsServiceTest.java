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
        assertEquals("Community impact data will appear here once verified activity is recorded.", dto.getNotice());
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

    @Test
    @DisplayName("getTimeSeries: returns empty state notice when zero verified records exist in time window")
    void getTimeSeries_EmptyData() {
        when(bloodRequestRepository.findByCreatedAtGreaterThanEqual(any())).thenReturn(java.util.Collections.emptyList());
        when(bloodRequestRepository.findByFulfilledAtGreaterThanEqual(any())).thenReturn(java.util.Collections.emptyList());
        when(donationRepository.findByDonationDateGreaterThanEqualAndVerificationStatus(any(), eq(DonationVerificationStatus.VERIFIED)))
                .thenReturn(java.util.Collections.emptyList());
        when(donorProfileRepository.count()).thenReturn(5L);

        org.netra.features.metrics.dto.CommunityTimeSeriesDto dto = communityMetricsService.getTimeSeries(7);

        assertNotNull(dto);
        assertEquals(7, dto.getDays());
        assertEquals(7, dto.getDataPoints().size());
        assertFalse(dto.isHasData());
        assertEquals(0L, dto.getTotalRequestsReceived());
        assertEquals(0L, dto.getTotalDonations());
        assertEquals(0L, dto.getTotalUnitsCollected());
        assertEquals("Community impact data will appear here once verified activity is recorded.", dto.getEmptyStateMessage());
    }

    @Test
    @DisplayName("getTimeSeries: calculates buckets and totals accurately when records exist")
    void getTimeSeries_WithData() {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        java.time.Instant nowInstant = java.time.Instant.now();

        org.netra.features.bloodrequest.entity.BloodRequest r1 = new org.netra.features.bloodrequest.entity.BloodRequest();
        r1.setCreatedAt(nowInstant);
        r1.setUrgency(org.netra.features.bloodrequest.entity.BloodRequestUrgency.CRITICAL);

        org.netra.features.bloodrequest.entity.BloodRequest r2 = new org.netra.features.bloodrequest.entity.BloodRequest();
        r2.setCreatedAt(nowInstant);
        r2.setFulfilledAt(nowInstant);
        r2.setUrgency(org.netra.features.bloodrequest.entity.BloodRequestUrgency.NORMAL);

        org.netra.features.donation.entity.Donation d1 = new org.netra.features.donation.entity.Donation();
        d1.setDonationDate(today);

        when(bloodRequestRepository.findByCreatedAtGreaterThanEqual(any())).thenReturn(java.util.List.of(r1, r2));
        when(bloodRequestRepository.findByFulfilledAtGreaterThanEqual(any())).thenReturn(java.util.List.of(r2));
        when(donationRepository.findByDonationDateGreaterThanEqualAndVerificationStatus(any(), eq(DonationVerificationStatus.VERIFIED)))
                .thenReturn(java.util.List.of(d1));
        when(donorProfileRepository.count()).thenReturn(25L);

        org.netra.features.metrics.dto.CommunityTimeSeriesDto dto = communityMetricsService.getTimeSeries(30);

        assertNotNull(dto);
        assertEquals(30, dto.getDays());
        assertEquals(30, dto.getDataPoints().size());
        assertTrue(dto.isHasData());
        assertEquals(2L, dto.getTotalRequestsReceived());
        assertEquals(1L, dto.getTotalRequestsFulfilled());
        assertEquals(1L, dto.getTotalDonations());
        assertEquals(1L, dto.getTotalUnitsCollected());
        assertEquals(1L, dto.getTotalEmergencyRequests());
        assertEquals(25L, dto.getActiveDonors());
        assertNull(dto.getEmptyStateMessage());
    }
}
