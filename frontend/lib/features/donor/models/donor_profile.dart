class DonorProfile {
  final String id;
  final String bloodGroup;
  final String bloodGroupVerificationStatus;
  final String availabilityStatus;
  final String donorStatus;
  final DateTime? lastDonationDate;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const DonorProfile({
    required this.id,
    required this.bloodGroup,
    required this.bloodGroupVerificationStatus,
    required this.availabilityStatus,
    required this.donorStatus,
    this.lastDonationDate,
    this.createdAt,
    this.updatedAt,
  });

  bool get isVerified => bloodGroupVerificationStatus == 'VERIFIED';
  bool get isAvailable => availabilityStatus == 'AVAILABLE';

  factory DonorProfile.fromJson(Map<String, dynamic> json) {
    return DonorProfile(
      id: json['id'] as String,
      bloodGroup: json['bloodGroup'] as String? ?? '',
      bloodGroupVerificationStatus:
          json['bloodGroupVerificationStatus'] as String? ?? 'SELF_REPORTED',
      availabilityStatus:
          json['availabilityStatus'] as String? ?? 'AVAILABLE',
      donorStatus: json['donorStatus'] as String? ?? 'ACTIVE',
      lastDonationDate: json['lastDonationDate'] != null
          ? DateTime.tryParse(json['lastDonationDate'].toString())
          : null,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'bloodGroup': bloodGroup,
      'bloodGroupVerificationStatus': bloodGroupVerificationStatus,
      'availabilityStatus': availabilityStatus,
      'donorStatus': donorStatus,
      if (lastDonationDate != null)
        'lastDonationDate':
            "${lastDonationDate!.year.toString().padLeft(4, '0')}-${lastDonationDate!.month.toString().padLeft(2, '0')}-${lastDonationDate!.day.toString().padLeft(2, '0')}",
      if (createdAt != null) 'createdAt': createdAt!.toIso8601String(),
      if (updatedAt != null) 'updatedAt': updatedAt!.toIso8601String(),
    };
  }
}

class CreateDonorProfileRequest {
  final String bloodGroup;
  final String? availabilityStatus;

  const CreateDonorProfileRequest({
    required this.bloodGroup,
    this.availabilityStatus,
  });

  Map<String, dynamic> toJson() {
    return {
      'bloodGroup': bloodGroup,
      if (availabilityStatus != null) 'availabilityStatus': availabilityStatus,
    };
  }
}

class UpdateDonorProfileRequest {
  final String? bloodGroup;
  final String? availabilityStatus;

  const UpdateDonorProfileRequest({
    this.bloodGroup,
    this.availabilityStatus,
  });

  Map<String, dynamic> toJson() {
    return {
      if (bloodGroup != null) 'bloodGroup': bloodGroup,
      if (availabilityStatus != null) 'availabilityStatus': availabilityStatus,
    };
  }
}

abstract class BloodGroupConstants {
  static const List<String> supported = [
    'A+',
    'A-',
    'B+',
    'B-',
    'O+',
    'O-',
    'AB+',
    'AB-',
  ];
}
