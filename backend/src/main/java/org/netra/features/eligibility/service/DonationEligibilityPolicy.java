package org.netra.features.eligibility.service;

import org.netra.core.exception.InvalidAnswerException;
import org.netra.features.eligibility.dto.DeferralDetailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Authoritative donation eligibility policy service.
 * Single source of truth across Eligibility and Matching for:
 * - Sex-specific donation recovery intervals (Male: 90 days, Female: 120 days per NBTC / CDSCO standards)
 * - First-time vs repeat donor age eligibility boundaries:
 *   * Minimum age: 18 years (deferral code: AGE_BELOW_MINIMUM)
 *   * First-time donor maximum age: 60 years (deferral code: FIRST_TIME_DONOR_AGE_LIMIT)
 *   * Repeat donor maximum age: 65 years (deferral code: AGE_ABOVE_MAXIMUM)
 */
@Service
public class DonationEligibilityPolicy {

    private static final Logger log = LoggerFactory.getLogger(DonationEligibilityPolicy.class);

    public static final int MALE_INTERVAL_DAYS = 90;
    public static final int FEMALE_INTERVAL_DAYS = 120;
    public static final int DEFAULT_INTERVAL_DAYS = 90;

    public static final int MINIMUM_DONOR_AGE = 18;
    public static final int FIRST_TIME_DONOR_MAX_AGE = 60;
    public static final int REPEAT_DONOR_MAX_AGE = 65;

    public static final String CODE_AGE_BELOW_MINIMUM = "AGE_BELOW_MINIMUM";
    public static final String CODE_AGE_ABOVE_MAXIMUM = "AGE_ABOVE_MAXIMUM";
    public static final String CODE_FIRST_TIME_AGE_LIMIT = "FIRST_TIME_DONOR_AGE_LIMIT";
    public static final String CODE_DONATION_INTERVAL_DEFICIT = "DONATION_INTERVAL_DEFICIT";

    /**
     * Determines required interval in days based on biological sex.
     * Male -> 90 days, Female -> 120 days, Other/Unspecified -> 90 days.
     */
    public int getRequiredIntervalDays(String biologicalSex) {
        if ("FEMALE".equalsIgnoreCase(biologicalSex)) {
            return FEMALE_INTERVAL_DAYS;
        }
        return MALE_INTERVAL_DAYS;
    }

    /**
     * Checks if donor meets the recovery interval requirement since their last donation.
     *
     * @param biologicalSex donor's biological sex (MALE, FEMALE, or null/OTHER)
     * @param lastDonationDate date of previous donation (null if never donated)
     * @param referenceDate reference date to evaluate against (defaults to today if null)
     * @return true if eligible (or never donated), false if within recovery interval
     */
    public boolean isIntervalEligible(String biologicalSex, LocalDate lastDonationDate, LocalDate referenceDate) {
        if (lastDonationDate == null) {
            return true;
        }
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        if (lastDonationDate.isAfter(ref)) {
            throw new InvalidAnswerException("Last donation date cannot be in the future: " + lastDonationDate);
        }
        long daysSinceDonation = ChronoUnit.DAYS.between(lastDonationDate, ref);
        int requiredInterval = getRequiredIntervalDays(biologicalSex);
        return daysSinceDonation >= requiredInterval;
    }

    /**
     * Calculates the estimated next eligible date based on sex-specific interval.
     */
    public LocalDate calculateNextEligibleDate(String biologicalSex, LocalDate lastDonationDate) {
        if (lastDonationDate == null) {
            return null;
        }
        return lastDonationDate.plusDays(getRequiredIntervalDays(biologicalSex));
    }

    /**
     * Checks donation interval deficit and returns DeferralDetailDto if deferred.
     */
    public Optional<DeferralDetailDto> checkIntervalEligibility(
            String biologicalSex, LocalDate lastDonationDate, LocalDate referenceDate) {
        if (lastDonationDate == null) {
            return Optional.empty();
        }
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        if (lastDonationDate.isAfter(ref)) {
            throw new InvalidAnswerException("Last donation date cannot be in the future: " + lastDonationDate);
        }

        long daysSince = ChronoUnit.DAYS.between(lastDonationDate, ref);
        int requiredInterval = getRequiredIntervalDays(biologicalSex);

        if (daysSince < requiredInterval) {
            LocalDate nextEligible = lastDonationDate.plusDays(requiredInterval);
            String sexLabel = "FEMALE".equalsIgnoreCase(biologicalSex) ? "Female" : "Male";
            return Optional.of(new DeferralDetailDto(
                    CODE_DONATION_INTERVAL_DEFICIT,
                    String.format("%s donors must wait a minimum of %d days between whole-blood donations.",
                            sexLabel, requiredInterval),
                    String.format("Your estimated next eligible date is %s. Set a reminder in the app.", nextEligible)
            ));
        }
        return Optional.empty();
    }

    /**
     * Evaluates age eligibility against NBTC / CDSCO criteria:
     * - Under 18: deferred (AGE_BELOW_MINIMUM)
     * - Over 65: deferred (AGE_ABOVE_MAXIMUM)
     * - 61-65: first-time donors deferred (FIRST_TIME_DONOR_AGE_LIMIT); repeat donors eligible
     * - 18-60: eligible
     *
     * @param age donor's chronological age in years
     * @param hasDonatedBefore true if donor has donated whole blood previously
     * @param previousDonationAnswered true if user provided an answer to the PREVIOUS_DONATION question
     * @return Optional containing DeferralDetailDto if deferred, or empty if eligible
     */
    public Optional<DeferralDetailDto> checkAgeEligibility(
            Integer age, boolean hasDonatedBefore, boolean previousDonationAnswered) {
        if (age == null) {
            return Optional.empty();
        }

        if (age < MINIMUM_DONOR_AGE) {
            return Optional.of(new DeferralDetailDto(
                    CODE_AGE_BELOW_MINIMUM,
                    "Donors must be at least 18 years old to voluntarily donate whole blood.",
                    "We look forward to welcoming you once you turn 18."
            ));
        }

        if (age > REPEAT_DONOR_MAX_AGE) {
            return Optional.of(new DeferralDetailDto(
                    CODE_AGE_ABOVE_MAXIMUM,
                    "Voluntary whole-blood donation is accepted up to 65 years of age per national guidelines.",
                    "Please speak with the blood centre physician regarding eligibility for apheresis or replacement donation."
            ));
        }

        if (age > FIRST_TIME_DONOR_MAX_AGE) {
            // Age is between 61 and 65
            if (previousDonationAnswered && !hasDonatedBefore) {
                return Optional.of(new DeferralDetailDto(
                        CODE_FIRST_TIME_AGE_LIMIT,
                        "First-time whole-blood donation is accepted up to 60 years of age per national guidelines (NBTC/CDSCO). Repeat donors may donate up to 65 years.",
                        "If you have previously donated blood, please update your questionnaire. Otherwise, please consult a blood centre physician."
                ));
            }
        }

        return Optional.empty();
    }
}
