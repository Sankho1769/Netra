/// Privacy-first location models for NETRA.
/// Medical privacy: NETRA uses approximate location for discovery.
/// Precise coordinates are never stored permanently.

class Coordinates {
  final double latitude;
  final double longitude;
  final double? accuracy;

  const Coordinates({
    required this.latitude,
    required this.longitude,
    this.accuracy,
  });

  Map<String, dynamic> toJson() => {
        'latitude': latitude,
        'longitude': longitude,
        if (accuracy != null) 'accuracy': accuracy,
      };

  factory Coordinates.fromJson(Map<String, dynamic> json) => Coordinates(
        latitude: (json['latitude'] as num).toDouble(),
        longitude: (json['longitude'] as num).toDouble(),
        accuracy: (json['accuracy'] as num?)?.toDouble(),
      );

  @override
  String toString() => 'Coordinates($latitude, $longitude)';
}

enum LocationPermissionStatus {
  granted,
  denied,
  permanentlyDenied,
  restricted,
  unknown,
}

class ApproximateLocation {
  final String? address;
  final String? city;
  final String? district;
  final String? state;
  final String? postalCode;
  final Coordinates? coordinates;
  final bool isApproximate;

  const ApproximateLocation({
    this.address,
    this.city,
    this.district,
    this.state,
    this.postalCode,
    this.coordinates,
    this.isApproximate = true,
  });

  double? get latitude => coordinates?.latitude;
  double? get longitude => coordinates?.longitude;

  String get displayName {
    final parts = [city, district, state]
        .where((p) => p != null && p.isNotEmpty)
        .toList();
    if (parts.isEmpty) {
      return postalCode ?? 'Current Area';
    }
    return parts.join(', ');
  }

  Map<String, dynamic> toJson() => {
        if (address != null) 'address': address,
        if (city != null) 'city': city,
        if (district != null) 'district': district,
        if (state != null) 'state': state,
        if (postalCode != null) 'postalCode': postalCode,
        'isApproximate': isApproximate,
        if (coordinates != null) 'coordinates': coordinates!.toJson(),
      };
}

enum LocationFailureReason {
  serviceDisabled,
  permissionDenied,
  permissionPermanentlyDenied,
  timeout,
  unavailable,
}

class LocationResult {
  final ApproximateLocation? location;
  final LocationFailureReason? failureReason;
  final String? errorMessage;

  const LocationResult.success(ApproximateLocation this.location)
      : failureReason = null,
        errorMessage = null;

  const LocationResult.failure(LocationFailureReason this.failureReason, [this.errorMessage])
      : location = null;

  bool get isSuccess => location != null;
  bool get isPermissionDenied => failureReason == LocationFailureReason.permissionDenied;
  bool get isPermissionPermanentlyDenied => failureReason == LocationFailureReason.permissionPermanentlyDenied;
  bool get isServiceDisabled => failureReason == LocationFailureReason.serviceDisabled;
}
