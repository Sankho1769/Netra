enum DonationSourceType {
  bloodRequest,
  donationEvent,
  unknown;

  static DonationSourceType fromString(String? value) {
    if (value == null) return DonationSourceType.unknown;
    switch (value.toUpperCase()) {
      case 'BLOOD_REQUEST':
        return DonationSourceType.bloodRequest;
      case 'DONATION_EVENT':
        return DonationSourceType.donationEvent;
      default:
        return DonationSourceType.unknown;
    }
  }

  String toServerString() {
    switch (this) {
      case DonationSourceType.bloodRequest:
        return 'BLOOD_REQUEST';
      case DonationSourceType.donationEvent:
        return 'DONATION_EVENT';
      case DonationSourceType.unknown:
        return 'UNKNOWN';
    }
  }

  String get label {
    switch (this) {
      case DonationSourceType.bloodRequest:
        return 'Direct Blood Request';
      case DonationSourceType.donationEvent:
        return 'Donation Camp';
      case DonationSourceType.unknown:
        return 'Unknown Source';
    }
  }
}

enum DonationVerificationStatus {
  pendingVerification,
  verified,
  rejected,
  cancelled,
  unknown;

  static DonationVerificationStatus fromString(String? value) {
    if (value == null) return DonationVerificationStatus.unknown;
    switch (value.toUpperCase()) {
      case 'PENDING_VERIFICATION':
        return DonationVerificationStatus.pendingVerification;
      case 'VERIFIED':
        return DonationVerificationStatus.verified;
      case 'REJECTED':
        return DonationVerificationStatus.rejected;
      case 'CANCELLED':
        return DonationVerificationStatus.cancelled;
      default:
        return DonationVerificationStatus.unknown;
    }
  }

  String toServerString() {
    switch (this) {
      case DonationVerificationStatus.pendingVerification:
        return 'PENDING_VERIFICATION';
      case DonationVerificationStatus.verified:
        return 'VERIFIED';
      case DonationVerificationStatus.rejected:
        return 'REJECTED';
      case DonationVerificationStatus.cancelled:
        return 'CANCELLED';
      case DonationVerificationStatus.unknown:
        return 'UNKNOWN';
    }
  }

  String get label {
    switch (this) {
      case DonationVerificationStatus.pendingVerification:
        return 'Pending Verification';
      case DonationVerificationStatus.verified:
        return 'Verified';
      case DonationVerificationStatus.rejected:
        return 'Rejected';
      case DonationVerificationStatus.cancelled:
        return 'Cancelled';
      case DonationVerificationStatus.unknown:
        return 'Unknown';
    }
  }

  bool get isTerminal =>
      this == DonationVerificationStatus.verified ||
      this == DonationVerificationStatus.rejected ||
      this == DonationVerificationStatus.cancelled;
}

class DonationModel {
  final String id;
  final String donorUserId;
  final String? donorName;
  final DonationSourceType sourceType;
  final String? bloodRequestId;
  final String? donationEventId;
  final String? referenceTitle;
  final String? referenceLocation;
  final DateTime donationDate;
  final DonationVerificationStatus verificationStatus;
  final DateTime? verifiedAt;
  final String? verifiedByUserId;
  final String? rejectionReason;
  final String? notes;
  final DateTime createdAt;
  final DateTime updatedAt;

  const DonationModel({
    required this.id,
    required this.donorUserId,
    this.donorName,
    required this.sourceType,
    this.bloodRequestId,
    this.donationEventId,
    this.referenceTitle,
    this.referenceLocation,
    required this.donationDate,
    required this.verificationStatus,
    this.verifiedAt,
    this.verifiedByUserId,
    this.rejectionReason,
    this.notes,
    required this.createdAt,
    required this.updatedAt,
  });

  factory DonationModel.fromJson(Map<String, dynamic> json) {
    final rawDate = json['donationDate'] as String?;
    final rawCreatedAt = json['createdAt'] as String?;
    final rawUpdatedAt = json['updatedAt'] as String?;
    final rawVerifiedAt = json['verifiedAt'] as String?;

    return DonationModel(
      id: json['id'] as String? ?? '',
      donorUserId: json['donorUserId'] as String? ?? '',
      donorName: json['donorName'] as String?,
      sourceType: DonationSourceType.fromString(json['sourceType'] as String?),
      bloodRequestId: json['bloodRequestId'] as String?,
      donationEventId: json['donationEventId'] as String?,
      referenceTitle: json['referenceTitle'] as String?,
      referenceLocation: json['referenceLocation'] as String?,
      donationDate: rawDate != null ? DateTime.parse(rawDate) : DateTime.now(),
      verificationStatus: DonationVerificationStatus.fromString(
          json['verificationStatus'] as String?),
      verifiedAt: rawVerifiedAt != null ? DateTime.parse(rawVerifiedAt) : null,
      verifiedByUserId: json['verifiedByUserId'] as String?,
      rejectionReason: json['rejectionReason'] as String?,
      notes: json['notes'] as String?,
      createdAt:
          rawCreatedAt != null ? DateTime.parse(rawCreatedAt) : DateTime.now(),
      updatedAt:
          rawUpdatedAt != null ? DateTime.parse(rawUpdatedAt) : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'donorUserId': donorUserId,
      if (donorName != null) 'donorName': donorName,
      'sourceType': sourceType.toServerString(),
      if (bloodRequestId != null) 'bloodRequestId': bloodRequestId,
      if (donationEventId != null) 'donationEventId': donationEventId,
      if (referenceTitle != null) 'referenceTitle': referenceTitle,
      if (referenceLocation != null) 'referenceLocation': referenceLocation,
      'donationDate': donationDate.toIso8601String().split('T').first,
      'verificationStatus': verificationStatus.toServerString(),
      if (verifiedAt != null) 'verifiedAt': verifiedAt!.toIso8601String(),
      if (verifiedByUserId != null) 'verifiedByUserId': verifiedByUserId,
      if (rejectionReason != null) 'rejectionReason': rejectionReason,
      if (notes != null) 'notes': notes,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }
}

class CreateDonationClaimDto {
  final DonationSourceType sourceType;
  final String? bloodRequestId;
  final String? donationEventId;
  final DateTime donationDate;
  final String? notes;

  const CreateDonationClaimDto({
    required this.sourceType,
    this.bloodRequestId,
    this.donationEventId,
    required this.donationDate,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'sourceType': sourceType.toServerString(),
      if (bloodRequestId != null) 'bloodRequestId': bloodRequestId,
      if (donationEventId != null) 'donationEventId': donationEventId,
      'donationDate': donationDate.toIso8601String().split('T').first,
      if (notes != null && notes!.trim().isNotEmpty) 'notes': notes!.trim(),
    };
  }
}
