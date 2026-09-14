package org.netra;

import org.netra.features.eligibility.entity.*;
import org.netra.features.eligibility.repository.EligibilityQuestionRepository;
import org.netra.features.eligibility.repository.EligibilityRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.List;

@SpringBootApplication
public class NetraApplication {

    private static final Logger log = LoggerFactory.getLogger(NetraApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NetraApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDefaultData(EligibilityQuestionRepository questionRepo, EligibilityRuleRepository ruleRepo) {
        return args -> {
            String version = "INDIA-NBTC-2026-01";
            if (!questionRepo.existsByVersionAndActive(version, true)) {
                log.info("Seeding baseline eligibility questions for version: {}", version);

                List<EligibilityQuestion> questions = List.of(
                        createQ(version, "AGE", 1, QuestionCategory.BASIC, "How old are you?",
                                "Donors must be between 18 and 65 years old per national blood transfusion guidelines.",
                                QuestionType.NUMBER, null, "{\"min\": 10, \"max\": 120, \"required\": true}", 1),

                        createQ(version, "WEIGHT_KG", 1, QuestionCategory.BASIC, "What is your current body weight (in kg)?",
                                "A donor must weigh at least 45 kg to safely donate 350 ml of whole blood, or 55 kg for 450 ml.",
                                QuestionType.NUMBER, null, "{\"min\": 30, \"max\": 250, \"required\": true}", 2),

                        createQ(version, "BIOLOGICAL_SEX", 1, QuestionCategory.BASIC, "Biological sex (for clinical interval & hemoglobin criteria)",
                                "Recovery intervals between whole blood donations differ: 90 days for men, 120 days for women.",
                                QuestionType.SINGLE_CHOICE,
                                "[{\"key\": \"MALE\", \"label\": \"Male\"}, {\"key\": \"FEMALE\", \"label\": \"Female\"}, {\"key\": \"OTHER\", \"label\": \"Other / Prefer not to say\"}]",
                                "{\"required\": true}", 3),

                        createQ(version, "PREVIOUS_DONATION", 2, QuestionCategory.RECENT_DONATION, "Have you donated blood before?",
                                "Helps determine required donation recovery intervals and first-time donor guidance.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 4),

                        createQ(version, "LAST_DONATION_DATE", 2, QuestionCategory.RECENT_DONATION, "Date of your most recent whole-blood donation",
                                "We automatically calculate your safe interval so your red blood cells and iron stores replenish fully.",
                                QuestionType.DATE, null, "{\"required\": false}", 5),

                        createQ(version, "CURRENTLY_FEELING_WELL", 3, QuestionCategory.CURRENT_HEALTH, "Are you currently feeling well, active, and in good health?",
                                "Donors should be symptom-free on the day of donation to ensure safety for both donor and recipient.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 6),

                        createQ(version, "FEVER_OR_ILLNESS_14D", 3, QuestionCategory.CURRENT_HEALTH, "Have you had a fever, viral illness, cough, or infection in the past 14 days?",
                                "A 14-day post-recovery deferral prevents the transmission of infectious agents.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 7),

                        createQ(version, "CURRENT_MEDICATION", 3, QuestionCategory.CURRENT_HEALTH, "Are you currently taking antibiotics, blood thinners, or prescription medication for an infection?",
                                "Certain medications remain in the bloodstream and could affect vulnerable transfusion recipients.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 8),

                        createQ(version, "PREGNANCY_OR_CHILDBIRTH", 3, QuestionCategory.CURRENT_HEALTH, "Are you currently pregnant, breast-feeding, or have delivered in the past 12 months?",
                                "Protects maternal iron levels; guidelines specify deferral during pregnancy and 12 months post-delivery.",
                                QuestionType.BOOLEAN, null, "{\"required\": false}", 9),

                        createQ(version, "TATTOO_OR_PIERCING_6M", 4, QuestionCategory.DONATION_SAFETY, "Have you had a tattoo, body piercing, or acupuncture done in the past 6 months?",
                                "Skin-piercing procedures carry a temporary deferral window to guard against blood-borne virus transmission.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 10),

                        createQ(version, "MAJOR_SURGERY_12M", 4, QuestionCategory.DONATION_SAFETY, "Have you had major surgery in the past 12 months, or minor surgery in the past 6 months?",
                                "Ensures sufficient healing time and restoration of normal blood volume.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 11),

                        createQ(version, "DENTAL_PROCEDURE_72H", 4, QuestionCategory.DONATION_SAFETY, "Have you had a tooth extraction or dental surgery in the past 72 hours?",
                                "Minor bacteremia can occur immediately following invasive dental procedures.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 12),

                        createQ(version, "CHRONIC_OR_CARDIAC_CONDITION", 4, QuestionCategory.DONATION_SAFETY, "Do you have a history of heart disease, epilepsy/seizures, bleeding disorders, or hepatitis/HIV?",
                                "Certain chronic conditions require direct evaluation by a qualified medical officer to ensure donor safety.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 13),

                        createQ(version, "SLEEP_HOURS_LAST_NIGHT", 5, QuestionCategory.PRE_CHECK, "Did you have at least 4 to 6 hours of sound sleep last night?",
                                "Adequate sleep reduces the risk of dizziness or vasovagal reactions during or after donation.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 14),

                        createQ(version, "MEAL_WITHIN_4_HOURS", 5, QuestionCategory.PRE_CHECK, "Have you had a light meal or snack within the last 4 hours?",
                                "Donating blood on an empty stomach significantly increases the chance of feeling faint.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 15),

                        createQ(version, "HYDRATED_TODAY", 5, QuestionCategory.PRE_CHECK, "Have you had plenty of water/fluids today?",
                                "Hydration keeps blood volume stable and makes vein access smoother.",
                                QuestionType.BOOLEAN, null, "{\"required\": true}", 16)
                );
                questionRepo.saveAll(questions);
                log.info("Seeded {} questions successfully.", questions.size());
            }

            if (ruleRepo.findActiveRulesByVersion(version, Instant.now()).isEmpty()) {
                log.info("Seeding baseline eligibility rules for version: {}", version);
                List<EligibilityRule> rules = List.of(
                        createRule(version, "RULE_AGE_MIN", "Donor must be at least 18 years old",
                                "NBTC Guidelines Sec 3.1 & Schedule F Part XII-B", "BASIC", "AGE < 18",
                                ResultType.TEMPORARY_DEFERRAL, null,
                                "Donors must be at least 18 years of age to voluntarily donate whole blood.", RuleSeverity.HIGH),

                        createRule(version, "RULE_AGE_MAX", "Donor upper age limit is 65 years",
                                "NBTC Guidelines Sec 3.1 & Schedule F Part XII-B", "BASIC", "AGE > 65",
                                ResultType.TEMPORARY_DEFERRAL, null,
                                "Standard voluntary whole-blood donation is recommended up to 65 years of age.", RuleSeverity.HIGH),

                        createRule(version, "RULE_WEIGHT_MIN", "Minimum donor weight is 45 kg",
                                "NBTC Guidelines Sec 3.2", "BASIC", "WEIGHT_KG < 45",
                                ResultType.TEMPORARY_DEFERRAL, null,
                                "A body weight of at least 45 kg is required to safely donate 350 ml of whole blood.", RuleSeverity.HIGH),

                        createRule(version, "RULE_INTERVAL_MALE", "Male donation interval 90 days",
                                "NBTC Guidelines Sec 3.3", "RECENT_DONATION", "BIOLOGICAL_SEX == 'MALE' && DAYS < 90",
                                ResultType.TEMPORARY_DEFERRAL, 90,
                                "Male donors must maintain a minimum 90-day (3 month) interval between whole-blood donations.", RuleSeverity.MEDIUM),

                        createRule(version, "RULE_INTERVAL_FEMALE", "Female donation interval 120 days",
                                "NBTC Guidelines Sec 3.3", "RECENT_DONATION", "BIOLOGICAL_SEX == 'FEMALE' && DAYS < 120",
                                ResultType.TEMPORARY_DEFERRAL, 120,
                                "Female donors must maintain a minimum 120-day (4 month) interval between whole-blood donations.", RuleSeverity.MEDIUM),

                        createRule(version, "RULE_FEVER_ILLNESS", "Fever or acute illness within 14 days",
                                "NBTC Guidelines Sec 4.2", "CURRENT_HEALTH", "FEVER_OR_ILLNESS_14D == true",
                                ResultType.TEMPORARY_DEFERRAL, 14,
                                "A 14-day symptom-free deferral is required following acute infections or fever.", RuleSeverity.MEDIUM),

                        createRule(version, "RULE_MEDICATION", "Active antibiotic / prescription medication",
                                "NBTC Guidelines Sec 4.4", "CURRENT_HEALTH", "CURRENT_MEDICATION == true",
                                ResultType.MEDICAL_REVIEW_REQUIRED, null,
                                "Certain medications require clinical review by the blood centre doctor before donation.", RuleSeverity.HIGH),

                        createRule(version, "RULE_CHRONIC_CARDIAC", "Heart condition, epilepsy, bleeding disorder, or blood-borne infection history",
                                "NBTC Guidelines Sec 5.4 & WHO Guidelines", "DONATION_SAFETY", "CHRONIC_OR_CARDIAC_CONDITION == true",
                                ResultType.MEDICAL_REVIEW_REQUIRED, null,
                                "Donors with a history of heart disease, seizures, bleeding disorders, or certain infections require medical clearance.", RuleSeverity.CRITICAL)
                );
                ruleRepo.saveAll(rules);
                log.info("Seeded {} rules successfully.", rules.size());
            }
        };
    }

    private EligibilityQuestion createQ(String version, String key, int step, QuestionCategory cat, String text, String help, QuestionType type, String options, String validation, int order) {
        EligibilityQuestion q = new EligibilityQuestion();
        q.setVersion(version);
        q.setQuestionKey(key);
        q.setStepNumber(step);
        q.setCategory(cat);
        q.setQuestionText(text);
        q.setHelpText(help);
        q.setQuestionType(type);
        q.setOptionsJson(options);
        q.setValidationJson(validation);
        q.setSortOrder(order);
        q.setActive(true);
        return q;
    }

    private EligibilityRule createRule(String version, String ruleId, String desc, String source, String cat, String cond, ResultType result, Integer deferralDays, String explanation, RuleSeverity severity) {
        EligibilityRule r = new EligibilityRule();
        r.setVersion(version);
        r.setRuleId(ruleId);
        r.setDescription(desc);
        r.setSource(source);
        r.setCategory(cat);
        r.setConditionExpression(cond);
        r.setResultType(result);
        r.setDeferralDurationDays(deferralDays);
        r.setExplanation(explanation);
        r.setSeverity(severity);
        r.setEffectiveFrom(Instant.parse("2026-01-01T00:00:00Z"));
        r.setActive(true);
        return r;
    }
}
