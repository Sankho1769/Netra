package org.netra.features.eligibility.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.InvalidAnswerException;
import org.netra.features.eligibility.dto.DeferralDetailDto;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DonationEligibilityPolicyTest {

    private DonationEligibilityPolicy policy;
    private final LocalDate refDate = LocalDate.of(2026, 9, 25);

    @BeforeEach
    void setUp() {
        policy = new DonationEligibilityPolicy();
    }

    @Test
    @DisplayName("Interval: Male donor requires 90 days interval")
    void testMaleIntervalRequirements() {
        assertEquals(90, policy.getRequiredIntervalDays("MALE"));

        // 89 days ago: ineligible
        assertFalse(policy.isIntervalEligible("MALE", refDate.minusDays(89), refDate));
        Optional<DeferralDetailDto> deferral89 = policy.checkIntervalEligibility("MALE", refDate.minusDays(89), refDate);
        assertTrue(deferral89.isPresent());
        assertEquals("DONATION_INTERVAL_DEFICIT", deferral89.get().getCode());
        assertTrue(deferral89.get().getMessage().contains("90 days"));

        // 90 days ago: eligible
        assertTrue(policy.isIntervalEligible("MALE", refDate.minusDays(90), refDate));
        assertTrue(policy.checkIntervalEligibility("MALE", refDate.minusDays(90), refDate).isEmpty());

        // 120 days ago: eligible
        assertTrue(policy.isIntervalEligible("MALE", refDate.minusDays(120), refDate));
    }

    @Test
    @DisplayName("Interval: Female donor requires 120 days interval")
    void testFemaleIntervalRequirements() {
        assertEquals(120, policy.getRequiredIntervalDays("FEMALE"));

        // 119 days ago: ineligible
        assertFalse(policy.isIntervalEligible("FEMALE", refDate.minusDays(119), refDate));
        Optional<DeferralDetailDto> deferral119 = policy.checkIntervalEligibility("FEMALE", refDate.minusDays(119), refDate);
        assertTrue(deferral119.isPresent());
        assertEquals("DONATION_INTERVAL_DEFICIT", deferral119.get().getCode());
        assertTrue(deferral119.get().getMessage().contains("120 days"));

        // 120 days ago: eligible
        assertTrue(policy.isIntervalEligible("FEMALE", refDate.minusDays(120), refDate));
        assertTrue(policy.checkIntervalEligibility("FEMALE", refDate.minusDays(120), refDate).isEmpty());
    }

    @Test
    @DisplayName("Interval: Unspecified or OTHER sex defaults to 90 days")
    void testDefaultInterval() {
        assertEquals(90, policy.getRequiredIntervalDays(null));
        assertEquals(90, policy.getRequiredIntervalDays("OTHER"));
        assertTrue(policy.isIntervalEligible(null, refDate.minusDays(90), refDate));
        assertFalse(policy.isIntervalEligible("OTHER", refDate.minusDays(89), refDate));
    }

    @Test
    @DisplayName("Interval: Null last donation date represents first-time donor (eligible)")
    void testNullLastDonationDate() {
        assertTrue(policy.isIntervalEligible("FEMALE", null, refDate));
        assertTrue(policy.isIntervalEligible("MALE", null, refDate));
        assertNull(policy.calculateNextEligibleDate("FEMALE", null));
        assertTrue(policy.checkIntervalEligibility("FEMALE", null, refDate).isEmpty());
    }

    @Test
    @DisplayName("Interval: Future donation date throws InvalidAnswerException")
    void testFutureDonationDateThrows() {
        LocalDate futureDate = refDate.plusDays(1);
        assertThrows(InvalidAnswerException.class, () ->
                policy.isIntervalEligible("MALE", futureDate, refDate));
        assertThrows(InvalidAnswerException.class, () ->
                policy.checkIntervalEligibility("MALE", futureDate, refDate));
    }

    @Test
    @DisplayName("Interval: calculateNextEligibleDate returns correct date")
    void testCalculateNextEligibleDate() {
        LocalDate lastDonation = LocalDate.of(2026, 6, 1);
        assertEquals(LocalDate.of(2026, 8, 30), policy.calculateNextEligibleDate("MALE", lastDonation));
        assertEquals(LocalDate.of(2026, 9, 29), policy.calculateNextEligibleDate("FEMALE", lastDonation));
    }

    @Test
    @DisplayName("Age: Minimum age 18 enforced for both first-time and repeat donors")
    void testMinimumAge() {
        Optional<DeferralDetailDto> d1 = policy.checkAgeEligibility(17, false, true);
        assertTrue(d1.isPresent());
        assertEquals("AGE_BELOW_MINIMUM", d1.get().getCode());

        Optional<DeferralDetailDto> d2 = policy.checkAgeEligibility(17, true, true);
        assertTrue(d2.isPresent());
        assertEquals("AGE_BELOW_MINIMUM", d2.get().getCode());

        Optional<DeferralDetailDto> d3 = policy.checkAgeEligibility(18, false, true);
        assertTrue(d3.isEmpty(), "Age 18 first-time donor must be eligible");

        Optional<DeferralDetailDto> d4 = policy.checkAgeEligibility(18, true, true);
        assertTrue(d4.isEmpty(), "Age 18 repeat donor must be eligible");
    }

    @Test
    @DisplayName("Age: First-time donor maximum age is 60 (NBTC/CDSCO rule)")
    void testFirstTimeDonorMaxAge() {
        // Age 60 first-time donor: eligible
        Optional<DeferralDetailDto> at60 = policy.checkAgeEligibility(60, false, true);
        assertTrue(at60.isEmpty(), "Age 60 first-time donor is eligible");

        // Age 61 first-time donor: deferred with FIRST_TIME_DONOR_AGE_LIMIT
        Optional<DeferralDetailDto> at61 = policy.checkAgeEligibility(61, false, true);
        assertTrue(at61.isPresent());
        assertEquals("FIRST_TIME_DONOR_AGE_LIMIT", at61.get().getCode());
        assertTrue(at61.get().getMessage().contains("60 years"));

        // Age 65 first-time donor: deferred with FIRST_TIME_DONOR_AGE_LIMIT
        Optional<DeferralDetailDto> at65 = policy.checkAgeEligibility(65, false, true);
        assertTrue(at65.isPresent());
        assertEquals("FIRST_TIME_DONOR_AGE_LIMIT", at65.get().getCode());
    }

    @Test
    @DisplayName("Age: Repeat donor maximum age is 65 (NBTC/CDSCO rule)")
    void testRepeatDonorMaxAge() {
        // Age 61 repeat donor: eligible
        Optional<DeferralDetailDto> at61 = policy.checkAgeEligibility(61, true, true);
        assertTrue(at61.isEmpty(), "Age 61 repeat donor is eligible");

        // Age 65 repeat donor: eligible
        Optional<DeferralDetailDto> at65 = policy.checkAgeEligibility(65, true, true);
        assertTrue(at65.isEmpty(), "Age 65 repeat donor is eligible");

        // Age 66 repeat donor: deferred with AGE_ABOVE_MAXIMUM
        Optional<DeferralDetailDto> at66 = policy.checkAgeEligibility(66, true, true);
        assertTrue(at66.isPresent());
        assertEquals("AGE_ABOVE_MAXIMUM", at66.get().getCode());

        // Age 66 first-time donor: deferred with AGE_ABOVE_MAXIMUM
        Optional<DeferralDetailDto> at66FirstTime = policy.checkAgeEligibility(66, false, true);
        assertTrue(at66FirstTime.isPresent());
        assertEquals("AGE_ABOVE_MAXIMUM", at66FirstTime.get().getCode());
    }
}
