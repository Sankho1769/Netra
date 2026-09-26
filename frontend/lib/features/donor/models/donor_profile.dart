class DonorProfile {
  final String id;
  final String bloodGroup;
  final String bloodGroupVerificationStatus;
  final String availabilityStatus;
  final String donorStatus;
  final DateTime? lastDonationDate;
  final String? biologicalSex;
  final String? verifiedBy;
  final DateTime? verifiedAt;
  final String? verificationNotes;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const DonorProfile({
    required this.id,
    required this.bloodGroup,
    required this.bloodGroupVerificationStatus,
    required this.availabilityStatus,
    required this.donorStatus,
    this.lastDonationDate,
    this.biologicalSex,
    this.verifiedBy,
    this.verifiedAt,
    this.verificationNotes,
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
      availabilityStatus: json['availabilityStatus'] as String? ?? 'AVAILABLE',
      donorStatus: json['donorStatus'] as String? ?? 'ACTIVE',
      lastDonationDate: json['lastDonationDate'] != null
          ? DateTime.tryParse(json['lastDonationDate'].toString())
          : null,
      biologicalSex: json['biologicalSex'] as String?,
      verifiedBy: json['verifiedBy'] as String?,
      verifiedAt: json['verifiedAt'] != null
          ? DateTime.tryParse(json['verifiedAt'].toString())
          : null,
      verificationNotes: json['verificationNotes'] as String?,
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
      if (biologicalSex != null) 'biologicalSex': biologicalSex,
      if (verifiedBy != null) 'verifiedBy': verifiedBy,
      if (verifiedAt != null) 'verifiedAt': verifiedAt!.toIso8601String(),
      if (verificationNotes != null) 'verificationNotes': verificationNotes,
      if (createdAt != null) 'createdAt': createdAt!.toIso8601String(),
      if (updatedAt != null) 'updatedAt': updatedAt!.toIso8601String(),
    };
  }
}

class CreateDonorProfileRequest {
  final String bloodGroup;
  final String? availabilityStatus;
  final String? biologicalSex;

  const CreateDonorProfileRequest({
    required this.bloodGroup,
    this.availabilityStatus,
    this.biologicalSex,
  });

  Map<String, dynamic> toJson() {
    return {
      'bloodGroup': bloodGroup,
      if (availabilityStatus != null) 'availabilityStatus': availabilityStatus,
      if (biologicalSex != null) 'biologicalSex': biologicalSex,
    };
  }
}

class UpdateDonorProfileRequest {
  final String? bloodGroup;
  final String? availabilityStatus;
  final String? biologicalSex;

  const UpdateDonorProfileRequest({
    this.bloodGroup,
    this.availabilityStatus,
    this.biologicalSex,
  });

  Map<String, dynamic> toJson() {
    return {
      if (bloodGroup != null) 'bloodGroup': bloodGroup,
      if (availabilityStatus != null) 'availabilityStatus': availabilityStatus,
      if (biologicalSex != null) 'biologicalSex': biologicalSex,
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
