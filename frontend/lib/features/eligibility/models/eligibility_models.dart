enum ResultType {
  LIKELY_ELIGIBLE,
  TEMPORARY_DEFERRAL,
  MEDICAL_REVIEW_REQUIRED,
  INSUFFICIENT_INFORMATION,
}

class QuestionOption {
  final String key;
  final String label;

  QuestionOption({required this.key, required this.label});

  factory QuestionOption.fromJson(Map<String, dynamic> json) {
    return QuestionOption(
      key: json['key'] ?? '',
      label: json['label'] ?? '',
    );
  }
}

class EligibilityQuestion {
  final String questionKey;
  final int stepNumber;
  final String category;
  final String text;
  final String? helpText;
  final String type;
  final List<QuestionOption> options;
  final String? validation;

  EligibilityQuestion({
    required this.questionKey,
    required this.stepNumber,
    required this.category,
    required this.text,
    this.helpText,
    required this.type,
    this.options = const [],
    this.validation,
  });

  factory EligibilityQuestion.fromJson(Map<String, dynamic> json) {
    return EligibilityQuestion(
      questionKey: json['questionKey'] ?? '',
      stepNumber: json['stepNumber'] ?? 1,
      category: json['category'] ?? '',
      text: json['text'] ?? '',
      helpText: json['helpText'],
      type: json['type'] ?? 'BOOLEAN',
      options: (json['options'] as List<dynamic>?)
              ?.map((o) => QuestionOption.fromJson(o))
              .toList() ??
          [],
      validation: json['validation'],
    );
  }
}

class DeferralReason {
  final String code;
  final String displayText;
  final String recommendedAction;

  DeferralReason({
    required this.code,
    required this.displayText,
    required this.recommendedAction,
  });

  factory DeferralReason.fromJson(Map<String, dynamic> json) {
    return DeferralReason(
      code: json['code'] ?? '',
      displayText: json['displayText'] ?? '',
      recommendedAction: json['recommendedAction'] ?? '',
    );
  }
}

class NextAction {
  final String actionType;
  final String label;
  final String? route;

  NextAction({
    required this.actionType,
    required this.label,
    this.route,
  });

  factory NextAction.fromJson(Map<String, dynamic> json) {
    return NextAction(
      actionType: json['actionType'] ?? '',
      label: json['label'] ?? '',
      route: json['route'],
    );
  }
}

class EligibilityResult {
  final String sessionId;
  final String ruleVersion;
  final ResultType result;
  final String title;
  final String message;
  final String disclaimer;
  final DateTime? estimatedNextEligibleDate;
  final List<DeferralReason> deferralReasons;
  final List<String> missingFields;
  final List<NextAction> nextActions;

  EligibilityResult({
    required this.sessionId,
    required this.ruleVersion,
    required this.result,
    required this.title,
    required this.message,
    required this.disclaimer,
    this.estimatedNextEligibleDate,
    this.deferralReasons = const [],
    this.missingFields = const [],
    this.nextActions = const [],
  });

  factory EligibilityResult.fromJson(Map<String, dynamic> json) {
    ResultType parsedResult;
    switch (json['result']) {
      case 'LIKELY_ELIGIBLE':
        parsedResult = ResultType.LIKELY_ELIGIBLE;
        break;
      case 'TEMPORARY_DEFERRAL':
        parsedResult = ResultType.TEMPORARY_DEFERRAL;
        break;
      case 'MEDICAL_REVIEW_REQUIRED':
        parsedResult = ResultType.MEDICAL_REVIEW_REQUIRED;
        break;
      default:
        parsedResult = ResultType.INSUFFICIENT_INFORMATION;
    }

    DateTime? nextDate;
    if (json['estimatedNextEligibleDate'] != null) {
      try {
        nextDate = DateTime.parse(json['estimatedNextEligibleDate']);
      } catch (_) {}
    }

    return EligibilityResult(
      sessionId: json['sessionId'] ?? '',
      ruleVersion: json['ruleVersion'] ?? '',
      result: parsedResult,
      title: json['title'] ?? '',
      message: json['message'] ?? '',
      disclaimer: json['disclaimer'] ??
          'Pre-screening result only. Final eligibility is determined by the blood bank/qualified medical staff after physical examination and required tests.',
      estimatedNextEligibleDate: nextDate,
      deferralReasons: (json['deferralReasons'] as List<dynamic>?)
              ?.map((r) => DeferralReason.fromJson(r))
              .toList() ??
          [],
      missingFields: (json['missingFields'] as List<dynamic>?)
              ?.map((f) => f.toString())
              .toList() ??
          [],
      nextActions: (json['nextActions'] as List<dynamic>?)
              ?.map((a) => NextAction.fromJson(a))
              .toList() ??
          [],
    );
  }
}
