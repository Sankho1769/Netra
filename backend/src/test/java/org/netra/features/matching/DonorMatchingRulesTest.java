package org.netra.features.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.matching.dto.DonorMatchDto;
import org.netra.features.matching.dto.MatchQuality;
import org.netra.features.matching.rules.BloodCompatibilityMatrix;
import org.netra.features.matching.rules.CompatibilityType;
import org.netra.features.matching.service.DonorMatchingService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DonorMatchingRulesTest {

    private BloodCompatibilityMatrix matrix;

    @BeforeEach
    void setUp() {
        matrix = new BloodCompatibilityMatrix();
    }

    @Test
    @DisplayName("Compatibility: O- recipient can receive ONLY from O-")
    void testONegativeRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.O_NEGATIVE);
        assertEquals(Set.of(BloodGroup.O_NEGATIVE), compatible);

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.O_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_POSITIVE, BloodGroup.O_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.AB_NEGATIVE, BloodGroup.O_NEGATIVE));
    }

    @Test
    @DisplayName("Compatibility: O+ recipient can receive from O+ and O-")
    void testOPositiveRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.O_POSITIVE);
        assertEquals(Set.of(BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE), compatible);

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.O_POSITIVE, BloodGroup.O_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_POSITIVE, BloodGroup.O_POSITIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE));
    }

    @Test
    @DisplayName("Compatibility: A- recipient can receive from A- and O-")
    void testANegativeRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.A_NEGATIVE);
        assertEquals(Set.of(BloodGroup.A_NEGATIVE, BloodGroup.O_NEGATIVE), compatible);

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.A_NEGATIVE, BloodGroup.A_NEGATIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.B_NEGATIVE, BloodGroup.A_NEGATIVE));
    }

    @Test
    @DisplayName("Compatibility: A+ recipient can receive from A+, A-, O+, O-")
    void testAPositiveRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.A_POSITIVE);
        assertEquals(Set.of(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE), compatible);

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.A_POSITIVE, BloodGroup.A_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_POSITIVE, BloodGroup.A_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.A_POSITIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.B_POSITIVE, BloodGroup.A_POSITIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.AB_POSITIVE, BloodGroup.A_POSITIVE));
    }

    @Test
    @DisplayName("Compatibility: B- recipient can receive from B- and O-")
    void testBNegativeRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.B_NEGATIVE);
        assertEquals(Set.of(BloodGroup.B_NEGATIVE, BloodGroup.O_NEGATIVE), compatible);

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.B_NEGATIVE, BloodGroup.B_NEGATIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.B_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_NEGATIVE, BloodGroup.B_NEGATIVE));
    }

    @Test
    @DisplayName("Compatibility: B+ recipient can receive from B+, B-, O+, O-")
    void testBPositiveRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.B_POSITIVE);
        assertEquals(Set.of(BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE), compatible);

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.B_POSITIVE, BloodGroup.B_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_POSITIVE, BloodGroup.B_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.B_POSITIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.AB_POSITIVE, BloodGroup.B_POSITIVE));
    }

    @Test
    @DisplayName("Compatibility: AB- recipient can receive from AB-, A-, B-, O-")
    void testABNegativeRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.AB_NEGATIVE);
        assertEquals(Set.of(BloodGroup.AB_NEGATIVE, BloodGroup.A_NEGATIVE, BloodGroup.B_NEGATIVE, BloodGroup.O_NEGATIVE), compatible);

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.AB_NEGATIVE, BloodGroup.AB_NEGATIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_NEGATIVE, BloodGroup.AB_NEGATIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.B_NEGATIVE, BloodGroup.AB_NEGATIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.AB_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.AB_POSITIVE, BloodGroup.AB_NEGATIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_POSITIVE, BloodGroup.AB_NEGATIVE));
    }

    @Test
    @DisplayName("Compatibility: AB+ universal recipient can receive from all 8 blood groups")
    void testABPositiveUniversalRecipient() {
        Set<BloodGroup> compatible = matrix.getCompatibleDonorGroups(BloodGroup.AB_POSITIVE);
        assertEquals(8, compatible.size());
        for (BloodGroup bg : BloodGroup.values()) {
            assertTrue(compatible.contains(bg), "AB+ must be compatible with " + bg);
            assertTrue(matrix.isCompatible(bg, BloodGroup.AB_POSITIVE));
        }

        assertEquals(CompatibilityType.EXACT, matrix.getCompatibilityType(BloodGroup.AB_POSITIVE, BloodGroup.AB_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_NEGATIVE, BloodGroup.AB_POSITIVE));
        assertEquals(CompatibilityType.COMPATIBLE, matrix.getCompatibilityType(BloodGroup.A_POSITIVE, BloodGroup.AB_POSITIVE));
    }

    @Test
    @DisplayName("Compatibility: Null inputs handled gracefully")
    void testNullInputs() {
        assertTrue(matrix.getCompatibleDonorGroups(null).isEmpty());
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(null, BloodGroup.O_POSITIVE));
        assertEquals(CompatibilityType.INCOMPATIBLE, matrix.getCompatibilityType(BloodGroup.O_POSITIVE, null));
        assertFalse(matrix.isCompatible(null, null));
    }

    @Test
    @DisplayName("Privacy: Donor name masking works as expected")
    void testMaskDisplayName() {
        assertEquals("John D.", DonorMatchingService.maskDisplayName("John Doe"));
        assertEquals("Jane S.", DonorMatchingService.maskDisplayName("Jane Smith"));
        assertEquals("Mary W.", DonorMatchingService.maskDisplayName("Mary Jane Watson"));
        assertEquals("Alice", DonorMatchingService.maskDisplayName("Alice"));
        assertEquals("Anonymous Donor", DonorMatchingService.maskDisplayName(null));
        assertEquals("Anonymous Donor", DonorMatchingService.maskDisplayName("   "));
    }

    @Test
    @DisplayName("Ranking: Deterministic sorting orders by Exact compatibility, then Proximity, then deterministic tie-breaker (all VERIFIED)")
    void testRankingComparatorDeterminism() {
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID id3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID id4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID id5 = UUID.fromString("00000000-0000-0000-0000-000000000005");

        // 1: Exact + Verified + 15 km
        DonorMatchDto d1 = new DonorMatchDto(
                id1, "Donor 1", BloodGroup.A_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, 15.0, CompatibilityType.EXACT, MatchQuality.EXCELLENT);

        // 2: Exact + Verified + 5 km (closer than d1)
        DonorMatchDto d2 = new DonorMatchDto(
                id2, "Donor 2", BloodGroup.A_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, 5.0, CompatibilityType.EXACT, MatchQuality.EXCELLENT);

        // 3: Compatible + Verified + 2 km (closer than d2, but only COMPATIBLE not EXACT)
        DonorMatchDto d3 = new DonorMatchDto(
                id3, "Donor 3", BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, 2.0, CompatibilityType.COMPATIBLE, MatchQuality.GOOD);

        // 4: Compatible + Verified + 10 km
        DonorMatchDto d4 = new DonorMatchDto(
                id4, "Donor 4", BloodGroup.O_NEGATIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, 10.0, CompatibilityType.COMPATIBLE, MatchQuality.GOOD);

        // 5: Compatible + Verified + 10 km (same distance as d4 -> deterministic tie-breaker by ID)
        DonorMatchDto d5 = new DonorMatchDto(
                id5, "Donor 5", BloodGroup.O_NEGATIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, 10.0, CompatibilityType.COMPATIBLE, MatchQuality.GOOD);

        List<DonorMatchDto> list = new ArrayList<>(List.of(d1, d4, d5, d3, d2));

        Comparator<DonorMatchDto> rankingComparator = Comparator
                .comparingInt((DonorMatchDto m) -> m.getCompatibilityType() == CompatibilityType.EXACT ? 0 : 1)
                .thenComparingDouble(DonorMatchDto::getDistanceKm)
                .thenComparing(m -> m.getMatchId().toString());

        list.sort(rankingComparator);

        // Expected order:
        // 1st: d2 (Exact, Verified, 5.0 km)
        // 2nd: d1 (Exact, Verified, 15.0 km)
        // 3rd: d3 (Compatible, Verified, 2.0 km)
        // 4th: d4 (Compatible, Verified, 10.0 km, id4 < id5)
        // 5th: d5 (Compatible, Verified, 10.0 km, id5 > id4)
        assertEquals(d2.getMatchId(), list.get(0).getMatchId());
        assertEquals(d1.getMatchId(), list.get(1).getMatchId());
        assertEquals(d3.getMatchId(), list.get(2).getMatchId());
        assertEquals(d4.getMatchId(), list.get(3).getMatchId());
        assertEquals(d5.getMatchId(), list.get(4).getMatchId());
    }
}
