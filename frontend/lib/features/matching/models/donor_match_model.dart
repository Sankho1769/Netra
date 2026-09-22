/// Blood transfusion compatibility classification.
enum CompatibilityType {
  exact,
  compatible,
  incompatible;

  static CompatibilityType fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'EXACT':
        return CompatibilityType.exact;
      case 'COMPATIBLE':
        return CompatibilityType.compatible;
      default:
        return CompatibilityType.incompatible;
    }
  }

  String get displayName {
    switch (this) {
      case CompatibilityType.exact:
        return 'Exact Match';
      case CompatibilityType.compatible:
        return 'Compatible';
      case CompatibilityType.incompatible:
        return 'Incompatible';
    }
  }
}

/// Match quality indicator based on compatibility, verification, and proximity.
enum MatchQuality {
  excellent,
  good,
  fair;

  static MatchQuality fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'EXCELLENT':
        return MatchQuality.excellent;
      case 'GOOD':
        return MatchQuality.good;
      default:
        return MatchQuality.fair;
    }
  }

  String get displayName {
    switch (this) {
      case MatchQuality.excellent:
        return 'Excellent';
      case MatchQuality.good:
        return 'Good';
      case MatchQuality.fair:
        return 'Fair';
    }
  }
}

/// Privacy-safe donor candidate matched to a blood request.
class DonorMatch {
  /// Authorized internal donor-selection reference. Not a secret.
  final String candidateReference;
  final String donorDisplayName;
  final String bloodGroup;
  final String bloodGroupVerificationStatus;
  final String availabilityStatus;
  final double distanceKm;
  final CompatibilityType compatibilityType;
  final MatchQuality matchQuality;

  const DonorMatch({
    required this.candidateReference,
    required this.donorDisplayName,
    required this.bloodGroup,
    required this.bloodGroupVerificationStatus,
    required this.availabilityStatus,
    required this.distanceKm,
    required this.compatibilityType,
    required this.matchQuality,
  });

  bool get isVerified =>
      bloodGroupVerificationStatus.toUpperCase() == 'VERIFIED';

  factory DonorMatch.fromJson(Map<String, dynamic> json) {
    return DonorMatch(
      candidateReference: json['candidateReference'] as String? ?? '',
      donorDisplayName:
          json['donorDisplayName'] as String? ?? 'Anonymous Donor',
      bloodGroup: json['bloodGroup'] as String? ?? '',
      bloodGroupVerificationStatus:
          json['bloodGroupVerificationStatus'] as String? ?? 'SELF_REPORTED',
      availabilityStatus: json['availabilityStatus'] as String? ?? 'AVAILABLE',
      distanceKm: (json['distanceKm'] as num?)?.toDouble() ?? 0.0,
      compatibilityType:
          CompatibilityType.fromString(json['compatibilityType'] as String?),
      matchQuality: MatchQuality.fromString(json['matchQuality'] as String?),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'candidateReference': candidateReference,
      'donorDisplayName': donorDisplayName,
      'bloodGroup': bloodGroup,
      'bloodGroupVerificationStatus': bloodGroupVerificationStatus,
      'availabilityStatus': availabilityStatus,
      'distanceKm': distanceKm,
      'compatibilityType': compatibilityType.name.toUpperCase(),
      'matchQuality': matchQuality.name.toUpperCase(),
    };
  }
}

/// Enveloping response returned by the donor matching engine.
class DonorMatchResponse {
  final String requestId;
  final String bloodGroupRequired;
  final String urgency;
  final double searchRadiusKm;
  final int candidateCount;
  final List<DonorMatch> matches;
  final String disclaimer;

  const DonorMatchResponse({
    required this.requestId,
    required this.bloodGroupRequired,
    required this.urgency,
    required this.searchRadiusKm,
    required this.candidateCount,
    required this.matches,
    required this.disclaimer,
  });

  factory DonorMatchResponse.fromJson(Map<String, dynamic> json) {
    final rawMatches = json['matches'] as List<dynamic>? ?? [];
    return DonorMatchResponse(
      requestId: json['requestId'] as String? ?? '',
      bloodGroupRequired: json['bloodGroupRequired'] as String? ?? '',
      urgency: json['urgency'] as String? ?? 'NORMAL',
      searchRadiusKm: (json['searchRadiusKm'] as num?)?.toDouble() ?? 25.0,
      candidateCount: json['candidateCount'] as int? ?? 0,
      matches: rawMatches
          .map((e) => DonorMatch.fromJson(e as Map<String, dynamic>))
          .toList(),
      disclaimer: json['disclaimer'] as String? ?? '',
    );
  }
}
