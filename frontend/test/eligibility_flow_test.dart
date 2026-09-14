import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/eligibility/services/eligibility_api_service.dart';
import 'package:netra_app/features/eligibility/state/eligibility_controller.dart';

void main() {
  group('EligibilityController Tests', () {
    late EligibilityController controller;

    setUp(() {
      controller = EligibilityController(apiService: EligibilityApiService());
    });

    test('Initial step is Step 1 of 6 with zero pre-defaulted answers (Rule 32)', () {
      expect(controller.currentStep, 1);
      expect(controller.totalSteps, 6);
      expect(controller.result, isNull);
      // Rule 32 Compliance: Never pre-default health or biological answers
      expect(controller.answers.isEmpty, true);
      expect(controller.getBoolAnswer('CURRENTLY_FEELING_WELL'), isNull);
      expect(controller.getBoolAnswer('PREVIOUS_DONATION'), isNull);
      expect(controller.getBoolAnswer('FEVER_OR_ILLNESS_14D'), isNull);
      expect(controller.getBoolAnswer('CURRENT_MEDICATION'), isNull);
      expect(controller.getBoolAnswer('TATTOO_OR_PIERCING_6M'), isNull);
    });

    test('Step 1 validation fails if required fields are missing', () {
      final canProceed = controller.validateStep(1);
      expect(canProceed, false);
      expect(controller.errors.containsKey('AGE'), true);
      expect(controller.errors.containsKey('WEIGHT_KG'), true);
      expect(controller.errors.containsKey('BIOLOGICAL_SEX'), true);
    });

    test('Step 1 validation succeeds with valid inputs', () {
      controller.setAnswer('AGE', '26');
      controller.setAnswer('WEIGHT_KG', '65');
      controller.setAnswer('BIOLOGICAL_SEX', 'MALE');

      final canProceed = controller.validateStep(1);
      expect(canProceed, true);
      expect(controller.errors.isEmpty, true);
    });

    test('Step 1 rejects invalid age and weight boundaries', () {
      controller.setAnswer('AGE', '5');
      controller.setAnswer('WEIGHT_KG', '20');
      controller.setAnswer('BIOLOGICAL_SEX', 'FEMALE');

      final canProceed = controller.validateStep(1);
      expect(canProceed, false);
      expect(controller.errors['AGE'], contains('valid age'));
      expect(controller.errors['WEIGHT_KG'], contains('valid weight'));
    });

    test('Step 2 validation fails if previous donation is unanswered (Rule 32)', () {
      final canProceed = controller.validateStep(2);
      expect(canProceed, false);
      expect(controller.errors.containsKey('PREVIOUS_DONATION'), true);
    });

    test('Step 2 automatically calculates interval from donation date', () {
      controller.setBoolAnswer('PREVIOUS_DONATION', true);
      final ninetyDaysAgo = DateTime.now().subtract(const Duration(days: 90));
      final dateFormatted =
          "${ninetyDaysAgo.year}-${ninetyDaysAgo.month.toString().padLeft(2, '0')}-${ninetyDaysAgo.day.toString().padLeft(2, '0')}";

      controller.setAnswer('LAST_DONATION_DATE', dateFormatted);
      expect(controller.daysSinceLastDonation, inInclusiveRange(89, 91));
    });

    test('Step 3 validation enforces explicit answers for all health questions (Rule 32)', () {
      controller.setAnswer('BIOLOGICAL_SEX', 'MALE');

      // Unanswered -> Fails
      expect(controller.validateStep(3), false);
      expect(controller.errors.containsKey('CURRENTLY_FEELING_WELL'), true);
      expect(controller.errors.containsKey('FEVER_OR_ILLNESS_14D'), true);
      expect(controller.errors.containsKey('CURRENT_MEDICATION'), true);

      // Answer questions
      controller.setBoolAnswer('CURRENTLY_FEELING_WELL', true);
      controller.setBoolAnswer('FEVER_OR_ILLNESS_14D', false);
      controller.setBoolAnswer('CURRENT_MEDICATION', false);

      expect(controller.validateStep(3), true);
      expect(controller.errors.isEmpty, true);
    });

    test('Step 3 enforces pregnancy/childbirth question for female donors (Rule 32)', () {
      controller.setAnswer('BIOLOGICAL_SEX', 'FEMALE');
      controller.setBoolAnswer('CURRENTLY_FEELING_WELL', true);
      controller.setBoolAnswer('FEVER_OR_ILLNESS_14D', false);
      controller.setBoolAnswer('CURRENT_MEDICATION', false);

      // Missing PREGNANCY_OR_CHILDBIRTH
      expect(controller.validateStep(3), false);
      expect(controller.errors.containsKey('PREGNANCY_OR_CHILDBIRTH'), true);

      controller.setBoolAnswer('PREGNANCY_OR_CHILDBIRTH', false);
      expect(controller.validateStep(3), true);
    });

    test('Step 4 validation enforces explicit answers for safety questions (Rule 32)', () {
      expect(controller.validateStep(4), false);
      expect(controller.errors.containsKey('TATTOO_OR_PIERCING_6M'), true);
      expect(controller.errors.containsKey('MAJOR_SURGERY_12M'), true);
      expect(controller.errors.containsKey('DENTAL_PROCEDURE_72H'), true);
      expect(controller.errors.containsKey('CHRONIC_OR_CARDIAC_CONDITION'), true);

      controller.setBoolAnswer('TATTOO_OR_PIERCING_6M', false);
      controller.setBoolAnswer('MAJOR_SURGERY_12M', false);
      controller.setBoolAnswer('DENTAL_PROCEDURE_72H', false);
      controller.setBoolAnswer('CHRONIC_OR_CARDIAC_CONDITION', false);

      expect(controller.validateStep(4), true);
    });

    test('Step 5 validation enforces explicit answers for readiness questions (Rule 32)', () {
      expect(controller.validateStep(5), false);
      expect(controller.errors.containsKey('SLEEP_HOURS_LAST_NIGHT'), true);
      expect(controller.errors.containsKey('MEAL_WITHIN_4_HOURS'), true);
      expect(controller.errors.containsKey('HYDRATED_TODAY'), true);

      controller.setBoolAnswer('SLEEP_HOURS_LAST_NIGHT', true);
      controller.setBoolAnswer('MEAL_WITHIN_4_HOURS', true);
      controller.setBoolAnswer('HYDRATED_TODAY', true);

      expect(controller.validateStep(5), true);
    });

    test('Step progression moves through each step upon answering explicitly to Step 6', () {
      // Step 1
      controller.setAnswer('AGE', '24');
      controller.setAnswer('WEIGHT_KG', '60');
      controller.setAnswer('BIOLOGICAL_SEX', 'MALE');
      expect(controller.nextStep(), true);
      expect(controller.currentStep, 2);

      // Step 2
      controller.setBoolAnswer('PREVIOUS_DONATION', false);
      expect(controller.nextStep(), true);
      expect(controller.currentStep, 3);

      // Step 3
      controller.setBoolAnswer('CURRENTLY_FEELING_WELL', true);
      controller.setBoolAnswer('FEVER_OR_ILLNESS_14D', false);
      controller.setBoolAnswer('CURRENT_MEDICATION', false);
      expect(controller.nextStep(), true);
      expect(controller.currentStep, 4);

      // Step 4
      controller.setBoolAnswer('TATTOO_OR_PIERCING_6M', false);
      controller.setBoolAnswer('MAJOR_SURGERY_12M', false);
      controller.setBoolAnswer('DENTAL_PROCEDURE_72H', false);
      controller.setBoolAnswer('CHRONIC_OR_CARDIAC_CONDITION', false);
      expect(controller.nextStep(), true);
      expect(controller.currentStep, 5);

      // Step 5
      controller.setBoolAnswer('SLEEP_HOURS_LAST_NIGHT', true);
      controller.setBoolAnswer('MEAL_WITHIN_4_HOURS', true);
      controller.setBoolAnswer('HYDRATED_TODAY', true);
      expect(controller.nextStep(), true);
      expect(controller.currentStep, 6); // Review screen
    });
  });
}
