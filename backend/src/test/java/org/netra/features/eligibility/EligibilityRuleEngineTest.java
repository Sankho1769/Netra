package org.netra.features.eligibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.InvalidAnswerException;
import org.netra.features.eligibility.dto.EligibilityResultResponse;
import org.netra.features.eligibility.entity.ResultType;
import org.netra.features.eligibility.service.EligibilityRuleEngine;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityRuleEngineTest {

    private EligibilityRuleEngine engine;
    private final LocalDate refDate = LocalDate.of(2026, 9, 15);
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        engine = new EligibilityRuleEngine();
    }

    private Map<String, String> createEligibleMaleAnswers() {
        Map<String, String> answers = new HashMap<>();
        answers.put("AGE", "25");
        answers.put("WEIGHT_KG", "68");
        answers.put("BIOLOGICAL_SEX", "MALE");
        answers.put("PREVIOUS_DONATION", "true");
        answers.put("LAST_DONATION_DATE", "2026-05-01"); // > 130 days ago
        answers.put("CURRENTLY_FEELING_WELL", "true");
        answers.put("FEVER_OR_ILLNESS_14D", "false");
        answers.put("CURRENT_MEDICATION", "false");
        answers.put("PREGNANCY_OR_CHILDBIRTH", "false");
        answers.put("TATTOO_OR_PIERCING_6M", "false");
        answers.put("MAJOR_SURGERY_12M", "false");
        answers.put("DENTAL_PROCEDURE_72H", "false");
        answers.put("CHRONIC_OR_CARDIAC_CONDITION", "false");
        answers.put("SLEEP_HOURS_LAST_NIGHT", "true");
        answers.put("MEAL_WITHIN_4_HOURS", "true");
        answers.put("HYDRATED_TODAY", "true");
        return answers;
    }

    @Test
    @DisplayName("Scenario 1: Valid eligible male donor meets all criteria")
    void testValidEligibleMale() {
        Map<String, String> answers = createEligibleMaleAnswers();
        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);

        assertEquals(ResultType.LIKELY_ELIGIBLE, result.getResult());
        assertTrue(result.getDeferralReasons().isEmpty());
        assertNotNull(result.getDisclaimer());
        assertTrue(result.getDisclaimer().contains("Pre-screening result only"));
    }

    @Test
    @DisplayName("Scenario 2: Valid eligible female donor with > 120 days interval")
    void testValidEligibleFemale() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("BIOLOGICAL_SEX", "FEMALE");
        answers.put("LAST_DONATION_DATE", "2026-04-10"); // > 150 days ago

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.LIKELY_ELIGIBLE, result.getResult());
    }

    @Test
    @DisplayName("Scenario 3: Age under 18 results in temporary deferral")
    void testAgeUnder18() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("AGE", "17");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertTrue(result.getDeferralReasons().stream().anyMatch(d -> "AGE_BELOW_MINIMUM".equals(d.getCode())));
    }

    @Test
    @DisplayName("Scenario 4: Age over 65 results in temporary deferral")
    void testAgeOver65() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("AGE", "67");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertTrue(result.getDeferralReasons().stream().anyMatch(d -> "AGE_ABOVE_MAXIMUM".equals(d.getCode())));
    }

    @Test
    @DisplayName("Scenario 5: Weight under 45 kg results in temporary deferral")
    void testWeightUnder45Kg() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("WEIGHT_KG", "43");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertTrue(result.getDeferralReasons().stream().anyMatch(d -> "WEIGHT_BELOW_MINIMUM".equals(d.getCode())));
    }

    @Test
    @DisplayName("Scenario 6: Male donor with < 90 days interval receives safe calculated next date")
    void testMaleIntervalDeficit() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("LAST_DONATION_DATE", "2026-08-01"); // 45 days ago

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertEquals(LocalDate.of(2026, 10, 30), result.getEstimatedNextEligibleDate());
        assertTrue(result.getDeferralReasons().stream().anyMatch(d -> "DONATION_INTERVAL_DEFICIT".equals(d.getCode())));
    }

    @Test
    @DisplayName("Scenario 7: Female donor with < 120 days interval receives safe calculated next date")
    void testFemaleIntervalDeficit() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("BIOLOGICAL_SEX", "FEMALE");
        answers.put("LAST_DONATION_DATE", "2026-07-01"); // 76 days ago

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertEquals(LocalDate.of(2026, 10, 29), result.getEstimatedNextEligibleDate());
    }

    @Test
    @DisplayName("Scenario 8: Recent fever/illness triggers 14-day deferral")
    void testRecentFever() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("FEVER_OR_ILLNESS_14D", "true");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertEquals(refDate.plusDays(14), result.getEstimatedNextEligibleDate());
    }

    @Test
    @DisplayName("Scenario 9: Prescription medication triggers MEDICAL_REVIEW_REQUIRED")
    void testPrescriptionMedication() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("CURRENT_MEDICATION", "true");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.MEDICAL_REVIEW_REQUIRED, result.getResult());
        assertTrue(result.getMessage().contains("Certain prescription medications"));
    }

    @Test
    @DisplayName("Scenario 10: Chronic/cardiac condition triggers MEDICAL_REVIEW_REQUIRED")
    void testChronicCardiacCondition() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("CHRONIC_OR_CARDIAC_CONDITION", "true");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.MEDICAL_REVIEW_REQUIRED, result.getResult());
    }

    @Test
    @DisplayName("Scenario 11: Tattoo or piercing within 6 months triggers temporary deferral")
    void testTattooWithin6Months() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("TATTOO_OR_PIERCING_6M", "true");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertTrue(result.getDeferralReasons().stream().anyMatch(d -> "TATTOO_OR_PIERCING_6M".equals(d.getCode())));
    }

    @Test
    @DisplayName("Scenario 12: Surgery within recovery window triggers temporary deferral")
    void testSurgeryRecovery() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("MAJOR_SURGERY_12M", "true");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
    }

    @Test
    @DisplayName("Scenario 13: Dental procedure within 72h triggers temporary deferral")
    void testDentalProcedure72H() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("DENTAL_PROCEDURE_72H", "true");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result.getResult());
        assertEquals(refDate.plusDays(3), result.getEstimatedNextEligibleDate());
    }

    @Test
    @DisplayName("Scenario 14: Missing required fields yields INSUFFICIENT_INFORMATION")
    void testMissingRequiredFields() {
        Map<String, String> answers = new HashMap<>();
        answers.put("AGE", "25");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.INSUFFICIENT_INFORMATION, result.getResult());
        assertTrue(result.getMissingFields().contains("WEIGHT_KG"));
    }

    @Test
    @DisplayName("Scenario 15: Sleep or meal deficit triggers pre-donation preparation advisory")
    void testSleepOrMealDeficit() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("SLEEP_HOURS_LAST_NIGHT", "false");

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.INSUFFICIENT_INFORMATION, result.getResult());
        assertTrue(result.getTitle().contains("Pre-donation preparation"));
    }

    @Test
    @DisplayName("Scenario 16: Impossible age (< 10) throws InvalidAnswerException")
    void testImpossibleAgeUnder10() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("AGE", "5");

        assertThrows(InvalidAnswerException.class, () -> 
            engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate)
        );
    }

    @Test
    @DisplayName("Scenario 17: Impossible age (> 120) throws InvalidAnswerException")
    void testImpossibleAgeOver120() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("AGE", "135");

        assertThrows(InvalidAnswerException.class, () -> 
            engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate)
        );
    }

    @Test
    @DisplayName("Scenario 18: Invalid weight (< 30 kg or > 300 kg) throws InvalidAnswerException")
    void testInvalidWeight() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("WEIGHT_KG", "15");

        assertThrows(InvalidAnswerException.class, () -> 
            engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate)
        );
    }

    @Test
    @DisplayName("Scenario 19: Future donation date throws InvalidAnswerException")
    void testFutureDonationDate() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("LAST_DONATION_DATE", "2027-01-01");

        assertThrows(InvalidAnswerException.class, () -> 
            engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate)
        );
    }

    @Test
    @DisplayName("Scenario 20: Medical disclaimer is unconditionally present on all outcomes")
    void testDisclaimerUnconditionallyPresent() {
        Map<String, String> answers = createEligibleMaleAnswers();
        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(EligibilityRuleEngine.DISCLAIMER_TEXT, result.getDisclaimer());
    }

    @Test
    @DisplayName("Scenario 21 (Rule 32): Missing biological sex cannot silently default to male")
    void testRule32MissingBiologicalSexYieldsInsufficientInformation() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.remove("BIOLOGICAL_SEX"); // Unanswered

        EligibilityResultResponse result = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.INSUFFICIENT_INFORMATION, result.getResult());
        assertTrue(result.getMissingFields().contains("BIOLOGICAL_SEX"));
    }

    @Test
    @DisplayName("Scenario 22: First-time donor age limit (<=60 eligible, 61 deferred with FIRST_TIME_DONOR_AGE_LIMIT)")
    void testFirstTimeDonorAgeLimitRule() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("PREVIOUS_DONATION", "false");
        answers.remove("LAST_DONATION_DATE");

        // Age 60 first-time donor -> LIKELY_ELIGIBLE
        answers.put("AGE", "60");
        EligibilityResultResponse result60 = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.LIKELY_ELIGIBLE, result60.getResult());

        // Age 61 first-time donor -> TEMPORARY_DEFERRAL with FIRST_TIME_DONOR_AGE_LIMIT
        answers.put("AGE", "61");
        EligibilityResultResponse result61 = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result61.getResult());
        assertTrue(result61.getDeferralReasons().stream()
                .anyMatch(d -> "FIRST_TIME_DONOR_AGE_LIMIT".equals(d.getCode())));
    }

    @Test
    @DisplayName("Scenario 23: Repeat donor age limit (<=65 eligible, 66 deferred with AGE_ABOVE_MAXIMUM)")
    void testRepeatDonorAgeLimitRule() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("PREVIOUS_DONATION", "true");
        answers.put("LAST_DONATION_DATE", "2026-01-01");

        // Age 65 repeat donor -> LIKELY_ELIGIBLE
        answers.put("AGE", "65");
        EligibilityResultResponse result65 = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.LIKELY_ELIGIBLE, result65.getResult());

        // Age 66 repeat donor -> TEMPORARY_DEFERRAL with AGE_ABOVE_MAXIMUM
        answers.put("AGE", "66");
        EligibilityResultResponse result66 = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, result66.getResult());
        assertTrue(result66.getDeferralReasons().stream()
                .anyMatch(d -> "AGE_ABOVE_MAXIMUM".equals(d.getCode())));
    }

    @Test
    @DisplayName("Scenario 24: Authoritative sex-specific interval (Male: 90 days, Female: 120 days)")
    void testSexSpecificDonationInterval() {
        Map<String, String> answers = createEligibleMaleAnswers();
        answers.put("PREVIOUS_DONATION", "true");
        // Donated 100 days ago
        answers.put("LAST_DONATION_DATE", refDate.minusDays(100).toString());

        // Male: 100 >= 90 -> LIKELY_ELIGIBLE
        answers.put("BIOLOGICAL_SEX", "MALE");
        EligibilityResultResponse maleResult = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.LIKELY_ELIGIBLE, maleResult.getResult());

        // Female: 100 < 120 -> TEMPORARY_DEFERRAL
        answers.put("BIOLOGICAL_SEX", "FEMALE");
        EligibilityResultResponse femaleResult = engine.evaluate(sessionId, "INDIA-NBTC-2026-01", answers, refDate);
        assertEquals(ResultType.TEMPORARY_DEFERRAL, femaleResult.getResult());
        assertTrue(femaleResult.getDeferralReasons().stream()
                .anyMatch(d -> "DONATION_INTERVAL_DEFICIT".equals(d.getCode()) && d.getMessage().contains("120 days")));
    }
}
