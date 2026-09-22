/// Lifecycle status for persistent donor matches.
enum DonorMatchStatus {
  matched,
  accepted,
  declined,
  expired,
  cancelled,
  unknown;

  static DonorMatchStatus fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'MATCHED':
        return DonorMatchStatus.matched;
      case 'ACCEPTED':
        return DonorMatchStatus.accepted;
      case 'DECLINED':
        return DonorMatchStatus.declined;
      case 'EXPIRED':
        return DonorMatchStatus.expired;
      case 'CANCELLED':
        return DonorMatchStatus.cancelled;
      default:
        return DonorMatchStatus.unknown;
    }
  }

  String get displayName {
    switch (this) {
      case DonorMatchStatus.matched:
        return 'Pending Response';
      case DonorMatchStatus.accepted:
        return 'Accepted';
      case DonorMatchStatus.declined:
        return 'Declined';
      case DonorMatchStatus.expired:
        return 'Expired';
      case DonorMatchStatus.cancelled:
        return 'Cancelled';
      case DonorMatchStatus.unknown:
        return 'Status unavailable';
    }
  }

  bool get isTerminal =>
      this == DonorMatchStatus.accepted ||
      this == DonorMatchStatus.declined ||
      this == DonorMatchStatus.expired ||
      this == DonorMatchStatus.cancelled;
}

/// Detailed match information presented to the donor.
///
/// Designed with privacy safeguards: exposes only necessary clinical/operational
/// parameters, completely omitting requester personal contact details.
class DonorMatchDetail {
  final String matchId;
  final String bloodRequestId;
  final String bloodGroupRequired;
  final int unitsRequired;
  final String urgency;
  final String hospitalName;
  final String city;
  final String state;
  final double? distanceKm;
  final DateTime? requiredBy;
  final DateTime expiresAt;
  final DonorMatchStatus responseStatus;
  final DateTime createdAt;
  final DateTime? respondedAt;
  final String disclaimer;

  const DonorMatchDetail({
    required this.matchId,
    required this.bloodRequestId,
    required this.bloodGroupRequired,
    required this.unitsRequired,
    required this.urgency,
    required this.hospitalName,
    required this.city,
    required this.state,
    this.distanceKm,
    this.requiredBy,
    required this.expiresAt,
    required this.responseStatus,
    required this.createdAt,
    this.respondedAt,
    required this.disclaimer,
  });

  /// Agrees with backend boundary: now >= expiresAt (or !now.isBefore(expiresAt))
  bool get isExpired => !DateTime.now().isBefore(expiresAt);

  factory DonorMatchDetail.fromJson(Map<String, dynamic> json) {
    final rawExpiresAt = json['expiresAt'];
    if (rawExpiresAt == null || rawExpiresAt is! String) {
      throw const FormatException(
          'Malformed donor match response: expiresAt timestamp is missing.');
    }
    final expiresAt = DateTime.tryParse(rawExpiresAt);
    if (expiresAt == null) {
      throw const FormatException(
          'Malformed donor match response: expiresAt timestamp is invalid.');
    }

    final rawCreatedAt = json['createdAt'];
    if (rawCreatedAt == null || rawCreatedAt is! String) {
      throw const FormatException(
          'Malformed donor match response: createdAt timestamp is missing.');
    }
    final createdAt = DateTime.tryParse(rawCreatedAt);
    if (createdAt == null) {
      throw const FormatException(
          'Malformed donor match response: createdAt timestamp is invalid.');
    }

    final rawStatus = json['responseStatus'] as String?;
    final responseStatus = DonorMatchStatus.fromString(rawStatus);

    return DonorMatchDetail(
      matchId: json['matchId'] as String? ?? '',
      bloodRequestId: json['bloodRequestId'] as String? ?? '',
      bloodGroupRequired: json['bloodGroupRequired'] as String? ?? '',
      unitsRequired: json['unitsRequired'] as int? ?? 1,
      urgency: json['urgency'] as String? ?? 'NORMAL',
      hospitalName:
          json['hospitalName'] as String? ?? 'Authorized Medical Facility',
      city: json['city'] as String? ?? '',
      state: json['state'] as String? ?? '',
      distanceKm: (json['distanceKm'] as num?)?.toDouble(),
      requiredBy: json['requiredBy'] != null
          ? DateTime.tryParse(json['requiredBy'] as String)
          : null,
      expiresAt: expiresAt,
      responseStatus: responseStatus,
      createdAt: createdAt,
      respondedAt: json['respondedAt'] != null
          ? DateTime.tryParse(json['respondedAt'] as String)
          : null,
      disclaimer: json['disclaimer'] as String? ??
          'Accepting a match does not confirm medical eligibility or donation. Final screening is performed by qualified blood-bank staff.',
    );
  }
}

/// Safe match information presented to the requester or administrator.
///
/// Strictly omits donor phone, email, home address, and coordinates.
class RequesterDonorMatch {
  final String matchId;
  final String bloodRequestId;
  final String donorDisplayName;
  final String bloodGroup;
  final String bloodGroupVerificationStatus;
  final String availabilityStatus;
  final double? distanceKm;
  final DonorMatchStatus responseStatus;
  final DateTime createdAt;
  final DateTime updatedAt;
  final DateTime? respondedAt;
  final DateTime expiresAt;

  const RequesterDonorMatch({
    required this.matchId,
    required this.bloodRequestId,
    required this.donorDisplayName,
    required this.bloodGroup,
    required this.bloodGroupVerificationStatus,
    required this.availabilityStatus,
    this.distanceKm,
    required this.responseStatus,
    required this.createdAt,
    required this.updatedAt,
    this.respondedAt,
    required this.expiresAt,
  });

  bool get isVerified =>
      bloodGroupVerificationStatus.toUpperCase() == 'VERIFIED';
  bool get isExpired => !DateTime.now().isBefore(expiresAt);

  factory RequesterDonorMatch.fromJson(Map<String, dynamic> json) {
    final rawExpiresAt = json['expiresAt'];
    if (rawExpiresAt == null || rawExpiresAt is! String) {
      throw const FormatException(
          'Malformed requester match response: expiresAt timestamp is missing.');
    }
    final expiresAt = DateTime.tryParse(rawExpiresAt);
    if (expiresAt == null) {
      throw const FormatException(
          'Malformed requester match response: expiresAt timestamp is invalid.');
    }

    final rawCreatedAt = json['createdAt'];
    if (rawCreatedAt == null || rawCreatedAt is! String) {
      throw const FormatException(
          'Malformed requester match response: createdAt timestamp is missing.');
    }
    final createdAt = DateTime.tryParse(rawCreatedAt);
    if (createdAt == null) {
      throw const FormatException(
          'Malformed requester match response: createdAt timestamp is invalid.');
    }

    final rawUpdatedAt = json['updatedAt'];
    final updatedAt =
        rawUpdatedAt is String ? DateTime.tryParse(rawUpdatedAt) : null;

    final rawStatus = json['responseStatus'] as String?;
    final responseStatus = DonorMatchStatus.fromString(rawStatus);

    return RequesterDonorMatch(
      matchId: json['matchId'] as String? ?? '',
      bloodRequestId: json['bloodRequestId'] as String? ?? '',
      donorDisplayName:
          json['donorDisplayName'] as String? ?? 'Anonymous Donor',
      bloodGroup: json['bloodGroup'] as String? ?? '',
      bloodGroupVerificationStatus:
          json['bloodGroupVerificationStatus'] as String? ?? 'SELF_REPORTED',
      availabilityStatus: json['availabilityStatus'] as String? ?? 'AVAILABLE',
      distanceKm: (json['distanceKm'] as num?)?.toDouble(),
      responseStatus: responseStatus,
      createdAt: createdAt,
      updatedAt: updatedAt ?? createdAt,
      respondedAt: json['respondedAt'] != null
          ? DateTime.tryParse(json['respondedAt'] as String)
          : null,
      expiresAt: expiresAt,
    );
  }
}
