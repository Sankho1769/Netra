package org.netra.features.eligibility.service;

import org.netra.core.exception.InvalidAnswerException;
import org.netra.features.eligibility.dto.DeferralDetailDto;
import org.netra.features.eligibility.dto.EligibilityResultResponse;
import org.netra.features.eligibility.dto.NextActionDto;
import org.netra.features.eligibility.entity.ResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class EligibilityRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(EligibilityRuleEngine.class);

    public static final String DISCLAIMER_TEXT = 
            "Pre-screening result only. Final eligibility is determined by the blood bank/qualified medical staff after physical examination and required tests.";

    public static final String ACTIVE_RULE_VERSION = "INDIA-NBTC-2026-01";

    private final DonationEligibilityPolicy policy;

    public EligibilityRuleEngine() {
        this(new DonationEligibilityPolicy());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EligibilityRuleEngine(DonationEligibilityPolicy policy) {
        this.policy = policy != null ? policy : new DonationEligibilityPolicy();
    }

    public EligibilityResultResponse evaluate(UUID sessionId, String ruleVersion, Map<String, String> answers, LocalDate referenceDate) {
        if (ruleVersion == null || ruleVersion.isBlank()) {
            ruleVersion = ACTIVE_RULE_VERSION;
        }
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        EligibilityResultResponse response = new EligibilityResultResponse();
        response.setSessionId(sessionId);
        response.setRuleVersion(ruleVersion);
        response.setDisclaimer(DISCLAIMER_TEXT);

        List<DeferralDetailDto> deferrals = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        boolean medicalReviewRequired = false;
        String medicalReviewReason = null;
        LocalDate calculatedNextDate = null;

        // Parse previous donation answer upfront for age rule evaluation
        String prevDonationStr = answers.get("PREVIOUS_DONATION");
        boolean previousDonationAnswered = prevDonationStr != null && !prevDonationStr.isBlank();
        boolean hasDonatedBefore = previousDonationAnswered && Boolean.parseBoolean(prevDonationStr.trim());

        // 1. Basic Information Validation & Evaluation
        Integer age = parseAge(answers.get("AGE"));
        if (age == null) {
            missingFields.add("AGE");
        } else {
            policy.checkAgeEligibility(age, hasDonatedBefore, previousDonationAnswered)
                    .ifPresent(deferrals::add);
        }

        Double weightKg = parseWeight(answers.get("WEIGHT_KG"));
        if (weightKg == null) {
            missingFields.add("WEIGHT_KG");
        } else {
            if (weightKg < 45.0) {
                deferrals.add(new DeferralDetailDto(
                        "WEIGHT_BELOW_MINIMUM",
                        "A minimum body weight of 45 kg is required for whole blood donation.",
                        "Eat a balanced, nutritious diet. You can donate once your weight reaches 45 kg or above."
                ));
            }
        }

        // Rule 32: Never assign a default biological sex!
        String sex = answers.get("BIOLOGICAL_SEX");
        if (sex == null || sex.isBlank()) {
            missingFields.add("BIOLOGICAL_SEX");
        }

        // 2. Recent Donation Interval Evaluation
        if (!previousDonationAnswered) {
            missingFields.add("PREVIOUS_DONATION");
        } else if (hasDonatedBefore) {
            String lastDateStr = answers.get("LAST_DONATION_DATE");
            if (lastDateStr == null || lastDateStr.isBlank()) {
                missingFields.add("LAST_DONATION_DATE");
            } else if (sex != null && !sex.isBlank()) {
                LocalDate lastDonationDate = parseDate(lastDateStr, referenceDate);
                Optional<DeferralDetailDto> intervalDeferral = policy.checkIntervalEligibility(sex, lastDonationDate, referenceDate);
                if (intervalDeferral.isPresent()) {
                    calculatedNextDate = policy.calculateNextEligibleDate(sex, lastDonationDate);
                    deferrals.add(intervalDeferral.get());
                }
            }
        }

        // 3. Current Health Evaluation
        String feelingWellStr = answers.get("CURRENTLY_FEELING_WELL");
        if (feelingWellStr == null || feelingWellStr.isBlank()) {
            missingFields.add("CURRENTLY_FEELING_WELL");
        } else if (!Boolean.parseBoolean(feelingWellStr)) {
            deferrals.add(new DeferralDetailDto(
                    "NOT_FEELING_WELL_TODAY",
                    "Donors should feel active, well, and symptom-free on the day of donation.",
                    "Please rest and return when you are feeling 100% healthy."
            ));
        }

        String feverStr = answers.get("FEVER_OR_ILLNESS_14D");
        if (feverStr == null || feverStr.isBlank()) {
            missingFields.add("FEVER_OR_ILLNESS_14D");
        } else if (Boolean.parseBoolean(feverStr)) {
            LocalDate feverEligibleDate = referenceDate.plusDays(14);
            if (calculatedNextDate == null || feverEligibleDate.isAfter(calculatedNextDate)) {
                calculatedNextDate = feverEligibleDate;
            }
            deferrals.add(new DeferralDetailDto(
                    "FEVER_OR_INFECTION_14D",
                    "A 14-day symptom-free deferral is required following fever or acute illness.",
                    "Please allow your immune system to fully recover before donating."
            ));
        }

        String medicationStr = answers.get("CURRENT_MEDICATION");
        if (medicationStr == null || medicationStr.isBlank()) {
            missingFields.add("CURRENT_MEDICATION");
        } else if (Boolean.parseBoolean(medicationStr)) {
            medicalReviewRequired = true;
            medicalReviewReason = "Certain prescription medications (such as antibiotics or blood thinners) require clinical review by a blood bank medical officer.";
        }

        String pregnancyStr = answers.get("PREGNANCY_OR_CHILDBIRTH");
        if (pregnancyStr != null && Boolean.parseBoolean(pregnancyStr)) {
            deferrals.add(new DeferralDetailDto(
                    "PREGNANCY_OR_LACTATION",
                    "Donation is deferred during pregnancy, lactation, and for 12 months post-delivery to protect maternal iron stores.",
                    "We look forward to welcoming you after this period."
            ));
        }

        // 4. Donation Safety Questions
        String tattooStr = answers.get("TATTOO_OR_PIERCING_6M");
        if (tattooStr == null || tattooStr.isBlank()) {
            missingFields.add("TATTOO_OR_PIERCING_6M");
        } else if (Boolean.parseBoolean(tattooStr)) {
            deferrals.add(new DeferralDetailDto(
                    "TATTOO_OR_PIERCING_6M",
                    "A 6-month deferral period is required after a tattoo, body piercing, or acupuncture.",
                    "This safety window protects against blood-borne virus transmission."
            ));
        }

        String surgeryStr = answers.get("MAJOR_SURGERY_12M");
        if (surgeryStr == null || surgeryStr.isBlank()) {
            missingFields.add("MAJOR_SURGERY_12M");
        } else if (Boolean.parseBoolean(surgeryStr)) {
            deferrals.add(new DeferralDetailDto(
                    "SURGERY_RECOVERY_PERIOD",
                    "A 12-month deferral is required following major surgery (6 months for minor surgery).",
                    "Ensures your body and blood count have completely healed."
            ));
        }

        String dentalStr = answers.get("DENTAL_PROCEDURE_72H");
        if (dentalStr == null || dentalStr.isBlank()) {
            missingFields.add("DENTAL_PROCEDURE_72H");
        } else if (Boolean.parseBoolean(dentalStr)) {
            LocalDate dentalNextDate = referenceDate.plusDays(3);
            if (calculatedNextDate == null || dentalNextDate.isAfter(calculatedNextDate)) {
                calculatedNextDate = dentalNextDate;
            }
            deferrals.add(new DeferralDetailDto(
                    "DENTAL_PROCEDURE_72H",
                    "Please wait 72 hours after tooth extractions or surgical dental procedures.",
                    "Guards against transient bacteremia."
            ));
        }

        String chronicStr = answers.get("CHRONIC_OR_CARDIAC_CONDITION");
        if (chronicStr == null || chronicStr.isBlank()) {
            missingFields.add("CHRONIC_OR_CARDIAC_CONDITION");
        } else if (Boolean.parseBoolean(chronicStr)) {
            medicalReviewRequired = true;
            medicalReviewReason = "Conditions involving cardiovascular health, seizures, bleeding disorders, or certain infections require medical review before donating.";
        }

        // 5. Pre-Check Guidance (Sleep & Meal)
        String sleepStr = answers.get("SLEEP_HOURS_LAST_NIGHT");
        String mealStr = answers.get("MEAL_WITHIN_4_HOURS");
        boolean preCheckNeedsAttention = false;

        if (sleepStr != null && !sleepStr.isBlank()) {
            String s = sleepStr.trim();
            if (s.equalsIgnoreCase("false")) {
                preCheckNeedsAttention = true;
            } else if (!s.equalsIgnoreCase("true")) {
                try {
                    double hours = Double.parseDouble(s);
                    if (hours < 4.0) {
                        preCheckNeedsAttention = true;
                    }
                } catch (NumberFormatException ignored) {
                    if (!Boolean.parseBoolean(s)) {
                        preCheckNeedsAttention = true;
                    }
                }
            }
        }

        if (mealStr != null && !mealStr.isBlank()) {
            if (!Boolean.parseBoolean(mealStr.trim())) {
                preCheckNeedsAttention = true;
            }
        }

        // 6. Deterministic Precedence: MEDICAL_REVIEW > DEFERRAL > INSUFFICIENT > LIKELY_ELIGIBLE
        if (medicalReviewRequired) {
            response.setResult(ResultType.MEDICAL_REVIEW_REQUIRED);
            response.setTitle("We can't determine your eligibility from the app alone");
            response.setMessage(medicalReviewReason != null ? medicalReviewReason :
                    "Please speak with the blood bank or qualified medical staff before donating.");
            response.setDeferralReasons(deferrals);
            response.setNextActions(List.of(
                    new NextActionDto("FIND_BLOOD_BANKS", "Find Nearby Blood Banks", "/blood-banks"),
                    new NextActionDto("CONSULT_STAFF", "Contact Blood Bank Doctor", "/contact-doctor")
            ));
        } else if (!deferrals.isEmpty()) {
            response.setResult(ResultType.TEMPORARY_DEFERRAL);
            response.setTitle("You may need to wait before donating");
            response.setMessage("Based on your answers, you currently have one or more temporary deferral criteria.");
            response.setEstimatedNextEligibleDate(calculatedNextDate);
            response.setDeferralReasons(deferrals);
            List<NextActionDto> actions = new ArrayList<>();
            if (calculatedNextDate != null) {
                actions.add(new NextActionDto("SET_REMINDER", "Set Reminder for Next Eligible Date", "/reminder"));
            }
            actions.add(new NextActionDto("FIND_BLOOD_BANKS", "Find Blood Banks", "/blood-banks"));
            response.setNextActions(actions);
        } else if (!missingFields.isEmpty()) {
            response.setResult(ResultType.INSUFFICIENT_INFORMATION);
            response.setTitle("More information needed");
            response.setMessage("Some required pre-screening questions have not been answered.");
            response.setMissingFields(missingFields);
            response.setNextActions(List.of(
                    new NextActionDto("COMPLETE_SURVEY", "Complete Questionnaire", "/eligibility/flow")
            ));
        } else if (preCheckNeedsAttention) {
            response.setResult(ResultType.INSUFFICIENT_INFORMATION);
            response.setTitle("Pre-donation preparation needed");
            response.setMessage("For your comfort and safety, donors should have at least 4-6 hours of sound sleep and eat a light meal/snack within 4 hours of donating.");
            response.setNextActions(List.of(
                    new NextActionDto("PRE_DONATION_GUIDE", "View Pre-Donation Tips", "/tips"),
                    new NextActionDto("FIND_BLOOD_BANKS", "Find Nearby Blood Banks", "/blood-banks")
            ));
        } else {
            response.setResult(ResultType.LIKELY_ELIGIBLE);
            response.setTitle("You appear eligible to donate blood");
            response.setMessage("Based on your answers, you meet the preliminary criteria for whole-blood donation.");
            response.setNextActions(List.of(
                    new NextActionDto("FIND_BLOOD_BANKS", "Find Nearby Blood Banks", "/blood-banks"),
                    new NextActionDto("FIND_EVENTS", "Find Donation Events", "/events"),
                    new NextActionDto("REGISTER_DONATION", "Register for Donation", "/register")
            ));
        }

        return response;
    }

    private Integer parseAge(String ageStr) {
        if (ageStr == null || ageStr.isBlank()) return null;
        try {
            int age = Integer.parseInt(ageStr.trim());
            if (age < 10 || age > 120) {
                throw new InvalidAnswerException("Provided age is outside valid biological range: " + age);
            }
            return age;
        } catch (NumberFormatException e) {
            throw new InvalidAnswerException("Invalid age format. Age must be a whole number.");
        }
    }

    private Double parseWeight(String weightStr) {
        if (weightStr == null || weightStr.isBlank()) return null;
        try {
            double weight = Double.parseDouble(weightStr.trim());
            if (weight < 30.0 || weight > 300.0) {
                throw new InvalidAnswerException("Provided weight is outside valid clinical range (30-300 kg): " + weight);
            }
            return weight;
        } catch (NumberFormatException e) {
            throw new InvalidAnswerException("Invalid weight format. Weight must be a numeric value in kg.");
        }
    }

    private LocalDate parseDate(String dateStr, LocalDate referenceDate) {
        try {
            LocalDate parsedDate = LocalDate.parse(dateStr.trim());
            if (parsedDate.isAfter(referenceDate)) {
                throw new InvalidAnswerException("Last donation date cannot be in the future: " + dateStr);
            }
            return parsedDate;
        } catch (DateTimeParseException e) {
            throw new InvalidAnswerException("Invalid date format. Please use YYYY-MM-DD.");
        }
    }
}
