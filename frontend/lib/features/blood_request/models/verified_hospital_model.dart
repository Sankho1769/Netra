class VerifiedHospitalModel {
  final String? id;
  final String name;
  final String address;
  final String city;
  final String state;
  final String? postalCode;
  final String? placeId;
  final bool hasBloodBank;
  final String verificationStatus;
  final double? latitude;
  final double? longitude;
  final String? phone;

  VerifiedHospitalModel({
    this.id,
    required this.name,
    required this.address,
    required this.city,
    required this.state,
    this.postalCode,
    this.placeId,
    required this.hasBloodBank,
    required this.verificationStatus,
    this.latitude,
    this.longitude,
    this.phone,
  });

  factory VerifiedHospitalModel.fromJson(Map<String, dynamic> json) {
    return VerifiedHospitalModel(
      id: json['id'] as String?,
      name: json['name'] as String? ?? json['hospitalName'] as String? ?? '',
      address: json['address'] as String? ??
          json['hospitalAddress'] as String? ??
          '',
      city: json['city'] as String? ?? '',
      state: json['state'] as String? ?? '',
      postalCode: json['postalCode'] as String?,
      placeId: json['placeId'] as String?,
      hasBloodBank: json['hasBloodBank'] as bool? ?? false,
      verificationStatus: json['verificationStatus'] as String? ?? 'VERIFIED',
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      phone: json['phone'] as String?,
    );
  }

  bool get isVerified => verificationStatus.toUpperCase() == 'VERIFIED';
  String get hospitalName => name;
  String get hospitalAddress => address;
}
