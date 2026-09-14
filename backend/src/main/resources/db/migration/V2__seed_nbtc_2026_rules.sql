-- ==========================================================
-- NETRA Platform: Seed Questions and Rules
-- Rule Version: INDIA-NBTC-2026-01
-- Authority: National Blood Transfusion Council (NBTC) / Drugs and Cosmetics Rules (Schedule F, Part XII-B)
-- Effective From: 2026-01-01
-- ==========================================================

-- Seed Questions (5 Sections, Step 6 is Review in UI)
INSERT INTO eligibility_questions (id, version, question_key, step_number, category, question_text, help_text, question_type, options_json, validation_json, sort_order, active)
VALUES
-- Section 1: Basic Information
('10000000-0000-0000-0000-000000000001', 'INDIA-NBTC-2026-01', 'AGE', 1, 'BASIC', 
 'How old are you?', 
 'Donors must be between 18 and 65 years old per national blood transfusion guidelines.', 
 'NUMBER', NULL, '{"min": 10, "max": 120, "required": true}', 1, TRUE),

('10000000-0000-0000-0000-000000000002', 'INDIA-NBTC-2026-01', 'WEIGHT_KG', 1, 'BASIC', 
 'What is your current body weight (in kg)?', 
 'A donor must weigh at least 45 kg to safely donate 350 ml of whole blood, or 55 kg for 450 ml.', 
 'NUMBER', NULL, '{"min": 30, "max": 250, "required": true}', 2, TRUE),

('10000000-0000-0000-0000-000000000003', 'INDIA-NBTC-2026-01', 'BIOLOGICAL_SEX', 1, 'BASIC', 
 'Biological sex (for clinical interval & hemoglobin criteria)', 
 'Recovery intervals between whole blood donations differ: 90 days for men, 120 days for women.', 
 'SINGLE_CHOICE', '[{"key": "MALE", "label": "Male"}, {"key": "FEMALE", "label": "Female"}, {"key": "OTHER", "label": "Other / Prefer not to say"}]', '{"required": true}', 3, TRUE),

-- Section 2: Recent Donation
('10000000-0000-0000-0000-000000000004', 'INDIA-NBTC-2026-01', 'PREVIOUS_DONATION', 2, 'RECENT_DONATION', 
 'Have you donated blood before?', 
 'Helps determine required donation recovery intervals and first-time donor guidance.', 
 'BOOLEAN', NULL, '{"required": true}', 4, TRUE),

('10000000-0000-0000-0000-000000000005', 'INDIA-NBTC-2026-01', 'LAST_DONATION_DATE', 2, 'RECENT_DONATION', 
 'Date of your most recent whole-blood donation', 
 'We automatically calculate your safe interval so your red blood cells and iron stores replenish fully.', 
 'DATE', NULL, '{"required": false}', 5, TRUE),

-- Section 3: Current Health
('10000000-0000-0000-0000-000000000006', 'INDIA-NBTC-2026-01', 'CURRENTLY_FEELING_WELL', 3, 'CURRENT_HEALTH', 
 'Are you currently feeling well, active, and in good health?', 
 'Donors should be symptom-free on the day of donation to ensure safety for both donor and recipient.', 
 'BOOLEAN', NULL, '{"required": true}', 6, TRUE),

('10000000-0000-0000-0000-000000000007', 'INDIA-NBTC-2026-01', 'FEVER_OR_ILLNESS_14D', 3, 'CURRENT_HEALTH', 
 'Have you had a fever, viral illness, cough, or infection in the past 14 days?', 
 'A 14-day post-recovery deferral prevents the transmission of infectious agents.', 
 'BOOLEAN', NULL, '{"required": true}', 7, TRUE),

('10000000-0000-0000-0000-000000000008', 'INDIA-NBTC-2026-01', 'CURRENT_MEDICATION', 3, 'CURRENT_HEALTH', 
 'Are you currently taking antibiotics, blood thinners, or prescription medication for an infection?', 
 'Certain medications remain in the bloodstream and could affect vulnerable transfusion recipients.', 
 'BOOLEAN', NULL, '{"required": true}', 8, TRUE),

('10000000-0000-0000-0000-000000000009', 'INDIA-NBTC-2026-01', 'PREGNANCY_OR_CHILDBIRTH', 3, 'CURRENT_HEALTH', 
 'Are you currently pregnant, breast-feeding, or have delivered in the past 12 months?', 
 'Protects maternal iron levels; guidelines specify deferral during pregnancy and 12 months post-delivery.', 
 'BOOLEAN', NULL, '{"required": false}', 9, TRUE),

-- Section 4: Donation Safety Questions
('10000000-0000-0000-0000-000000000010', 'INDIA-NBTC-2026-01', 'TATTOO_OR_PIERCING_6M', 4, 'DONATION_SAFETY', 
 'Have you had a tattoo, body piercing, or acupuncture done in the past 6 months?', 
 'Skin-piercing procedures carry a temporary deferral window to guard against blood-borne virus transmission.', 
 'BOOLEAN', NULL, '{"required": true}', 10, TRUE),

('10000000-0000-0000-0000-000000000011', 'INDIA-NBTC-2026-01', 'MAJOR_SURGERY_12M', 4, 'DONATION_SAFETY', 
 'Have you had major surgery in the past 12 months, or minor surgery in the past 6 months?', 
 'Ensures sufficient healing time and restoration of normal blood volume.', 
 'BOOLEAN', NULL, '{"required": true}', 11, TRUE),

('10000000-0000-0000-0000-000000000012', 'INDIA-NBTC-2026-01', 'DENTAL_PROCEDURE_72H', 4, 'DONATION_SAFETY', 
 'Have you had a tooth extraction or dental surgery in the past 72 hours?', 
 'Minor bacteremia can occur immediately following invasive dental procedures.', 
 'BOOLEAN', NULL, '{"required": true}', 12, TRUE),

('10000000-0000-0000-0000-000000000013', 'INDIA-NBTC-2026-01', 'CHRONIC_OR_CARDIAC_CONDITION', 4, 'DONATION_SAFETY', 
 'Do you have a history of heart disease, epilepsy/seizures, bleeding disorders, or hepatitis/HIV?', 
 'Certain chronic conditions require direct evaluation by a qualified medical officer to ensure donor safety.', 
 'BOOLEAN', NULL, '{"required": true}', 13, TRUE),

-- Section 5: Basic Pre-Donation Check
('10000000-0000-0000-0000-000000000014', 'INDIA-NBTC-2026-01', 'SLEEP_HOURS_LAST_NIGHT', 5, 'PRE_CHECK', 
 'Did you have at least 4 to 6 hours of sound sleep last night?', 
 'Adequate sleep reduces the risk of dizziness or vasovagal reactions during or after donation.', 
 'BOOLEAN', NULL, '{"required": true}', 14, TRUE),

('10000000-0000-0000-0000-000000000015', 'INDIA-NBTC-2026-01', 'MEAL_WITHIN_4_HOURS', 5, 'PRE_CHECK', 
 'Have you had a light meal or snack within the last 4 hours?', 
 'Donating blood on an empty stomach significantly increases the chance of feeling faint.', 
 'BOOLEAN', NULL, '{"required": true}', 15, TRUE),

('10000000-0000-0000-0000-000000000016', 'INDIA-NBTC-2026-01', 'HYDRATED_TODAY', 5, 'PRE_CHECK', 
 'Have you had plenty of water/fluids today?', 
 'Hydration keeps blood volume stable and makes vein access smoother.', 
 'BOOLEAN', NULL, '{"required": true}', 16, TRUE)
ON CONFLICT (version, question_key) DO NOTHING;

-- Seed Rules
INSERT INTO eligibility_rules (id, rule_id, version, description, source, category, condition_expression, result_type, deferral_duration_days, explanation, severity, effective_from, active)
VALUES
('20000000-0000-0000-0000-000000000001', 'RULE_AGE_MIN', 'INDIA-NBTC-2026-01', 
 'Donor must be at least 18 years old', 
 'NBTC Guidelines Sec 3.1 & Drugs and Cosmetics Rules Sch F Part XII-B', 
 'BASIC', 'AGE < 18', 'TEMPORARY_DEFERRAL', NULL, 
 'Donors must be at least 18 years of age to voluntarily donate whole blood.', 
 'HIGH', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000002', 'RULE_AGE_MAX', 'INDIA-NBTC-2026-01', 
 'Donor upper age limit is 65 years', 
 'NBTC Guidelines Sec 3.1 & Drugs and Cosmetics Rules Sch F Part XII-B', 
 'BASIC', 'AGE > 65', 'TEMPORARY_DEFERRAL', NULL, 
 'Standard voluntary whole-blood donation is recommended up to 65 years of age.', 
 'HIGH', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000003', 'RULE_WEIGHT_MIN', 'INDIA-NBTC-2026-01', 
 'Minimum donor weight is 45 kg', 
 'NBTC Guidelines Sec 3.2', 
 'BASIC', 'WEIGHT_KG < 45', 'TEMPORARY_DEFERRAL', NULL, 
 'A body weight of at least 45 kg is required to safely donate 350 ml of whole blood.', 
 'HIGH', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000004', 'RULE_INTERVAL_MALE', 'INDIA-NBTC-2026-01', 
 'Male donation interval 90 days', 
 'NBTC Guidelines Sec 3.3', 
 'RECENT_DONATION', 'BIOLOGICAL_SEX == "MALE" && DAYS_SINCE_LAST_DONATION < 90', 'TEMPORARY_DEFERRAL', 90, 
 'Male donors must maintain a minimum 90-day (3 month) interval between whole-blood donations.', 
 'MEDIUM', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000005', 'RULE_INTERVAL_FEMALE', 'INDIA-NBTC-2026-01', 
 'Female donation interval 120 days', 
 'NBTC Guidelines Sec 3.3', 
 'RECENT_DONATION', 'BIOLOGICAL_SEX == "FEMALE" && DAYS_SINCE_LAST_DONATION < 120', 'TEMPORARY_DEFERRAL', 120, 
 'Female donors must maintain a minimum 120-day (4 month) interval between whole-blood donations.', 
 'MEDIUM', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000006', 'RULE_FEVER_ILLNESS', 'INDIA-NBTC-2026-01', 
 'Fever or acute illness within 14 days', 
 'NBTC Guidelines Sec 4.2', 
 'CURRENT_HEALTH', 'FEVER_OR_ILLNESS_14D == true', 'TEMPORARY_DEFERRAL', 14, 
 'A 14-day symptom-free deferral is required following acute infections or fever.', 
 'MEDIUM', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000007', 'RULE_MEDICATION', 'INDIA-NBTC-2026-01', 
 'Active antibiotic / prescription medication', 
 'NBTC Guidelines Sec 4.4', 
 'CURRENT_HEALTH', 'CURRENT_MEDICATION == true', 'MEDICAL_REVIEW_REQUIRED', NULL, 
 'Certain medications require clinical review by the blood centre doctor before donation.', 
 'HIGH', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000008', 'RULE_PREGNANCY', 'INDIA-NBTC-2026-01', 
 'Pregnancy and lactation deferral', 
 'NBTC Guidelines Sec 4.5', 
 'CURRENT_HEALTH', 'PREGNANCY_OR_CHILDBIRTH == true', 'TEMPORARY_DEFERRAL', 365, 
 'Donors who are pregnant or nursing must defer until 12 months after delivery.', 
 'HIGH', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000009', 'RULE_TATTOO_PIERCING', 'INDIA-NBTC-2026-01', 
 'Tattoo, body piercing, or acupuncture within 6 months', 
 'NBTC Guidelines Sec 5.1 & WHO Donor Selection Criteria', 
 'DONATION_SAFETY', 'TATTOO_OR_PIERCING_6M == true', 'TEMPORARY_DEFERRAL', 180, 
 'A 6-month deferral window ensures safety following tattooing, ear/body piercing, or acupuncture.', 
 'HIGH', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000010', 'RULE_SURGERY', 'INDIA-NBTC-2026-01', 
 'Major or minor surgery within recovery window', 
 'NBTC Guidelines Sec 5.2', 
 'DONATION_SAFETY', 'MAJOR_SURGERY_12M == true', 'TEMPORARY_DEFERRAL', 365, 
 'Major surgery requires a 12-month deferral window; minor surgery requires a 6-month window.', 
 'HIGH', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000011', 'RULE_DENTAL', 'INDIA-NBTC-2026-01', 
 'Dental extraction or surgery within 72 hours', 
 'NBTC Guidelines Sec 5.3', 
 'DONATION_SAFETY', 'DENTAL_PROCEDURE_72H == true', 'TEMPORARY_DEFERRAL', 3, 
 'Please wait 72 hours following tooth extraction or dental surgery before donating.', 
 'MEDIUM', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000012', 'RULE_CHRONIC_CARDIAC', 'INDIA-NBTC-2026-01', 
 'Heart condition, epilepsy, bleeding disorder, or blood-borne infection history', 
 'NBTC Guidelines Sec 5.4 & WHO Guidelines', 
 'DONATION_SAFETY', 'CHRONIC_OR_CARDIAC_CONDITION == true', 'MEDICAL_REVIEW_REQUIRED', NULL, 
 'Donors with a history of heart disease, seizures, bleeding disorders, or certain infections require medical clearance.', 
 'CRITICAL', '2026-01-01 00:00:00+00', TRUE),

('20000000-0000-0000-0000-000000000013', 'RULE_PRE_CHECK_FAIL', 'INDIA-NBTC-2026-01', 
 'Pre-donation sleep or meal deficit', 
 'NBTC Guidelines Sec 2.1 & Pre-donation counseling', 
 'PRE_CHECK', 'SLEEP_HOURS_LAST_NIGHT == false || MEAL_WITHIN_4_HOURS == false', 'INSUFFICIENT_INFORMATION', NULL, 
 'To ensure you feel comfortable, please obtain at least 4-6 hours of sleep and eat a light meal before visiting the blood centre.', 
 'LOW', '2026-01-01 00:00:00+00', TRUE)
ON CONFLICT (version, rule_id) DO NOTHING;
