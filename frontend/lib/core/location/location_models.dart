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
  final String? city;
  final String? district;
  final String? state;
  final String? postalCode;
  final Coordinates? coordinates;
  final bool isApproximate;

  const ApproximateLocation({
    this.city,
    this.district,
    this.state,
    this.postalCode,
    this.coordinates,
    this.isApproximate = true,
  });

  String get displayName {
    final parts = [city, district, state].where((p) => p != null && p.isNotEmpty).toList();
    if (parts.isEmpty) {
      return postalCode ?? 'Current Area';
    }
    return parts.join(', ');
  }

  Map<String, dynamic> toJson() => {
        if (city != null) 'city': city,
        if (district != null) 'district': district,
        if (state != null) 'state': state,
        if (postalCode != null) 'postalCode': postalCode,
        'isApproximate': isApproximate,
        if (coordinates != null) 'coordinates': coordinates!.toJson(),
      };
}
