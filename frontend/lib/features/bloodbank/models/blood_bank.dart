import 'blood_inventory.dart';

enum BloodBankVerificationStatus {
  pending,
  verified,
  suspended,
  rejected;

  static BloodBankVerificationStatus fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'VERIFIED':
        return BloodBankVerificationStatus.verified;
      case 'SUSPENDED':
        return BloodBankVerificationStatus.suspended;
      case 'REJECTED':
        return BloodBankVerificationStatus.rejected;
      case 'PENDING':
      default:
        return BloodBankVerificationStatus.pending;
    }
  }

  String get displayName {
    switch (this) {
      case BloodBankVerificationStatus.verified:
        return 'Verified Centre';
      case BloodBankVerificationStatus.suspended:
        return 'Suspended';
      case BloodBankVerificationStatus.rejected:
        return 'Rejected';
      case BloodBankVerificationStatus.pending:
        return 'Verification Pending';
    }
  }
}

enum BloodBankOperatingStatus {
  open,
  closed,
  temporarilyUnavailable;

  static BloodBankOperatingStatus fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'CLOSED':
        return BloodBankOperatingStatus.closed;
      case 'TEMPORARILY_UNAVAILABLE':
        return BloodBankOperatingStatus.temporarilyUnavailable;
      case 'OPEN':
      default:
        return BloodBankOperatingStatus.open;
    }
  }

  String get displayName {
    switch (this) {
      case BloodBankOperatingStatus.open:
        return 'Open';
      case BloodBankOperatingStatus.closed:
        return 'Closed';
      case BloodBankOperatingStatus.temporarilyUnavailable:
        return 'Temporarily Unavailable';
    }
  }
}

class BloodBankSummary {
  final String id;
  final String name;
  final String? registrationNumber;
  final String address;
  final String city;
  final String state;
  final String postalCode;
  final String phone;
  final String? email;
  final BloodBankVerificationStatus verificationStatus;
  final BloodBankOperatingStatus operatingStatus;
  final double? distanceKm;

  BloodBankSummary({
    required this.id,
    required this.name,
    this.registrationNumber,
    required this.address,
    required this.city,
    required this.state,
    required this.postalCode,
    required this.phone,
    this.email,
    required this.verificationStatus,
    required this.operatingStatus,
    this.distanceKm,
  });

  factory BloodBankSummary.fromJson(Map<String, dynamic> json) {
    return BloodBankSummary(
      id: json['id'] as String,
      name: json['name'] as String,
      registrationNumber: json['registrationNumber'] as String?,
      address: json['address'] as String,
      city: json['city'] as String,
      state: json['state'] as String,
      postalCode: json['postalCode'] as String,
      phone: json['phone'] as String,
      email: json['email'] as String?,
      verificationStatus: BloodBankVerificationStatus.fromString(json['verificationStatus'] as String?),
      operatingStatus: BloodBankOperatingStatus.fromString(json['operatingStatus'] as String?),
      distanceKm: json['distanceKm'] != null ? (json['distanceKm'] as num).toDouble() : null,
    );
  }

  String get formattedDistance {
    if (distanceKm == null) return '';
    return '${distanceKm!.toStringAsFixed(1)} km away';
  }
}

class BloodBankDetail {
  final String id;
  final String name;
  final String? registrationNumber;
  final String address;
  final String city;
  final String state;
  final String postalCode;
  final double latitude;
  final double longitude;
  final String phone;
  final String? email;
  final BloodBankVerificationStatus verificationStatus;
  final BloodBankOperatingStatus operatingStatus;
  final double? distanceKm;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final List<BloodInventoryItem> inventory;

  BloodBankDetail({
    required this.id,
    required this.name,
    this.registrationNumber,
    required this.address,
    required this.city,
    required this.state,
    required this.postalCode,
    required this.latitude,
    required this.longitude,
    required this.phone,
    this.email,
    required this.verificationStatus,
    required this.operatingStatus,
    this.distanceKm,
    this.createdAt,
    this.updatedAt,
    this.inventory = const [],
  });

  factory BloodBankDetail.fromJson(Map<String, dynamic> json) {
    final invList = (json['inventory'] as List<dynamic>?)
            ?.map((e) => BloodInventoryItem.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [];

    return BloodBankDetail(
      id: json['id'] as String,
      name: json['name'] as String,
      registrationNumber: json['registrationNumber'] as String?,
      address: json['address'] as String,
      city: json['city'] as String,
      state: json['state'] as String,
      postalCode: json['postalCode'] as String,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      phone: json['phone'] as String,
      email: json['email'] as String?,
      verificationStatus: BloodBankVerificationStatus.fromString(json['verificationStatus'] as String?),
      operatingStatus: BloodBankOperatingStatus.fromString(json['operatingStatus'] as String?),
      distanceKm: json['distanceKm'] != null ? (json['distanceKm'] as num).toDouble() : null,
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'] as String) : null,
      updatedAt: json['updatedAt'] != null ? DateTime.tryParse(json['updatedAt'] as String) : null,
      inventory: invList,
    );
  }

  String get formattedDistance {
    if (distanceKm == null) return '';
    return '${distanceKm!.toStringAsFixed(1)} km away';
  }
}
