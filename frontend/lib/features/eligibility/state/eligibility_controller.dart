import 'package:flutter/material.dart';
import '../../models/eligibility_models.dart';
import '../services/eligibility_api_service.dart';

class EligibilityController extends ChangeNotifier {
  final EligibilityApiService apiService;

  int _currentStep = 1;
  final int totalSteps = 6;
  String? _sessionId;
  String _ruleVersion = 'INDIA-NBTC-2026-01';

  final Map<String, String> _answers = {};
  final Map<String, String> _errors = {};

  bool _isLoading = false;
  String? _errorMessage;
  EligibilityResult? _result;

  EligibilityController({required this.apiService}) {
    // Clinical Safety (Rule 32): Never pre-populate or default clinical answers.
    // Unanswered questions remain null until explicitly answered by the donor.
  }

  int get currentStep => _currentStep;
  String? get sessionId => _sessionId;
  Map<String, String> get answers => Map.unmodifiable(_answers);
  Map<String, String> get errors => Map.unmodifiable(_errors);
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  EligibilityResult? get result => _result;
  String get ruleVersion => _ruleVersion;

  void setAnswer(String key, String value) {
    _answers[key] = value;
    _errors.remove(key);
    notifyListeners();
  }

  String? getAnswer(String key) => _answers[key];

  bool? getBoolAnswer(String key) {
    final val = _answers[key];
    if (val == null) return null;
    return val.toLowerCase() == 'true';
  }

  void setBoolAnswer(String key, bool? value) {
    if (value == null) {
      _answers.remove(key);
    } else {
      _answers[key] = value.toString();
    }
    _errors.remove(key);
    notifyListeners();
  }

  bool isAnswered(String key) => _answers[key] != null && _answers[key]!.isNotEmpty;

  int? get daysSinceLastDonation {
    final dateStr = _answers['LAST_DONATION_DATE'];
    if (dateStr == null || dateStr.isEmpty) return null;
    try {
      final lastDate = DateTime.parse(dateStr);
      return DateTime.now().difference(lastDate).inDays;
    } catch (_) {
      return null;
    }
  }

  bool validateStep(int step) {
    _errors.clear();
    switch (step) {
      case 1:
        // Basic Info
        final ageStr = _answers['AGE'];
        if (ageStr == null || ageStr.trim().isEmpty) {
          _errors['AGE'] = 'Please enter your age.';
        } else {
          final age = int.tryParse(ageStr.trim());
          if (age == null || age < 10 || age > 120) {
            _errors['AGE'] = 'Please enter a valid age (10-120 years).';
          }
        }

        final weightStr = _answers['WEIGHT_KG'];
        if (weightStr == null || weightStr.trim().isEmpty) {
          _errors['WEIGHT_KG'] = 'Please enter your weight in kg.';
        } else {
          final weight = double.tryParse(weightStr.trim());
          if (weight == null || weight < 30 || weight > 300) {
            _errors['WEIGHT_KG'] = 'Please enter a valid weight (30-300 kg).';
          }
        }

        if (_answers['BIOLOGICAL_SEX'] == null || _answers['BIOLOGICAL_SEX']!.isEmpty) {
          _errors['BIOLOGICAL_SEX'] = 'Please select biological sex for clinical intervals.';
        }
        break;

      case 2:
        // Recent Donation
        if (_answers['PREVIOUS_DONATION'] == null) {
          _errors['PREVIOUS_DONATION'] = 'Please indicate whether you have donated blood before.';
        } else if (_answers['PREVIOUS_DONATION'] == 'true') {
          final lastDate = _answers['LAST_DONATION_DATE'];
          if (lastDate == null || lastDate.isEmpty) {
            _errors['LAST_DONATION_DATE'] = 'Please select the date of your last donation.';
          } else {
            try {
              final parsed = DateTime.parse(lastDate);
              if (parsed.isAfter(DateTime.now())) {
                _errors['LAST_DONATION_DATE'] = 'Last donation date cannot be in the future.';
              }
            } catch (_) {
              _errors['LAST_DONATION_DATE'] = 'Invalid date format.';
            }
          }
        }
        break;

      case 3:
        // Current Health
        if (_answers['CURRENTLY_FEELING_WELL'] == null) {
          _errors['CURRENTLY_FEELING_WELL'] = 'Please answer whether you are currently feeling well.';
        }
        if (_answers['FEVER_OR_ILLNESS_14D'] == null) {
          _errors['FEVER_OR_ILLNESS_14D'] = 'Please answer whether you had a fever or illness in the last 14 days.';
        }
        if (_answers['CURRENT_MEDICATION'] == null) {
          _errors['CURRENT_MEDICATION'] = 'Please answer whether you are currently taking prescription medications.';
        }
        final sex = _answers['BIOLOGICAL_SEX'];
        if ((sex == 'FEMALE' || sex == 'OTHER') && _answers['PREGNANCY_OR_CHILDBIRTH'] == null) {
          _errors['PREGNANCY_OR_CHILDBIRTH'] = 'Please answer the pregnancy and childbirth question.';
        }
        break;

      case 4:
        // Donation Safety
        if (_answers['TATTOO_OR_PIERCING_6M'] == null) {
          _errors['TATTOO_OR_PIERCING_6M'] = 'Please answer whether you had a tattoo or piercing in the last 6 months.';
        }
        if (_answers['MAJOR_SURGERY_12M'] == null) {
          _errors['MAJOR_SURGERY_12M'] = 'Please answer whether you had major surgery in the last 12 months.';
        }
        if (_answers['DENTAL_PROCEDURE_72H'] == null) {
          _errors['DENTAL_PROCEDURE_72H'] = 'Please answer whether you had a dental procedure in the last 72 hours.';
        }
        if (_answers['CHRONIC_OR_CARDIAC_CONDITION'] == null) {
          _errors['CHRONIC_OR_CARDIAC_CONDITION'] = 'Please answer whether you have a history of heart or chronic conditions.';
        }
        break;

      case 5:
        // Pre-Donation Check
        if (_answers['SLEEP_HOURS_LAST_NIGHT'] == null) {
          _errors['SLEEP_HOURS_LAST_NIGHT'] = 'Please answer whether you had 4 to 6 hours of sleep last night.';
        }
        if (_answers['MEAL_WITHIN_4_HOURS'] == null) {
          _errors['MEAL_WITHIN_4_HOURS'] = 'Please answer whether you had a meal or snack within the last 4 hours.';
        }
        if (_answers['HYDRATED_TODAY'] == null) {
          _errors['HYDRATED_TODAY'] = 'Please answer whether you have had plenty of fluids today.';
        }
        break;
    }

    notifyListeners();
    return _errors.isEmpty;
  }

  bool nextStep() {
    if (!validateStep(_currentStep)) {
      return false;
    }
    if (_currentStep < totalSteps) {
      _currentStep++;
      notifyListeners();
      return true;
    }
    return false;
  }

  void previousStep() {
    if (_currentStep > 1) {
      _currentStep--;
      _errors.clear();
      notifyListeners();
    }
  }

  void jumpToStep(int step) {
    if (step >= 1 && step <= totalSteps) {
      _currentStep = step;
      _errors.clear();
      notifyListeners();
    }
  }

  Future<void> submitAndEvaluate(BuildContext context) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      // 1. Create or ensure session
      _sessionId ??= await apiService.createSession();

      // 2. Submit all answers
      await apiService.submitAnswers(_sessionId!, _answers);

      // 3. Request server-side evaluation (Anti-tampering: result strictly computed by backend)
      final evalResult = await apiService.checkEligibility(_sessionId!);
      _result = evalResult;
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _errorMessage = e.toString();
      notifyListeners();
    }
  }
}
