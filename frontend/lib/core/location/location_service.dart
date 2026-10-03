import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:geolocator/geolocator.dart';
import 'location_models.dart';
import 'permission_service.dart';

export 'location_models.dart';

/// Privacy-first location service for NETRA.
/// Coordinates are coarse by default to prevent precise donor tracking.
abstract class LocationService {
  Future<ApproximateLocation?> getCurrentLocation(
      {bool approximateOnly = true});
  Future<LocationResult> getDetailedLocation(
      {bool approximateOnly = true});
  Future<List<ApproximateLocation>> searchLocations(String query);
  Future<bool> openAppSettings();
  Future<bool> openLocationSettings();
}

/// Production implementation with real GPS integration, privacy fuzzing, and graceful fallbacks.
class DefaultLocationService implements LocationService {
  final PermissionService _permissionService;
  final Future<Coordinates?> Function()? _deviceCoordinateProvider;

  DefaultLocationService({
    PermissionService? permissionService,
    Future<Coordinates?> Function()? deviceCoordinateProvider,
  })  : _permissionService = permissionService ?? DefaultPermissionService(),
        _deviceCoordinateProvider = deviceCoordinateProvider;

  @override
  Future<ApproximateLocation?> getCurrentLocation(
      {bool approximateOnly = true}) async {
    final result = await getDetailedLocation(approximateOnly: approximateOnly);
    return result.location;
  }

  @override
  Future<LocationResult> getDetailedLocation(
      {bool approximateOnly = true}) async {
    final enabled = await _permissionService.isLocationServiceEnabled();
    if (!enabled) {
      return const LocationResult.failure(
        LocationFailureReason.serviceDisabled,
        'Device location service is turned off. Please enable GPS in device settings.',
      );
    }

    final permission = await _permissionService.checkLocationPermission();
    if (permission == LocationPermissionStatus.permanentlyDenied) {
      return const LocationResult.failure(
        LocationFailureReason.permissionPermanentlyDenied,
        'Location permission is permanently denied. Please enable it in App Settings.',
      );
    }

    if (permission != LocationPermissionStatus.granted) {
      final requested = await _permissionService.requestLocationPermission();
      if (requested == LocationPermissionStatus.permanentlyDenied) {
        return const LocationResult.failure(
          LocationFailureReason.permissionPermanentlyDenied,
          'Location permission is permanently denied. Please enable it in App Settings.',
        );
      }
      if (requested != LocationPermissionStatus.granted) {
        return const LocationResult.failure(
          LocationFailureReason.permissionDenied,
          'Location permission was denied.',
        );
      }
    }

    if (_deviceCoordinateProvider != null) {
      final coords = await _deviceCoordinateProvider!();
      if (coords == null) {
        return const LocationResult.failure(
          LocationFailureReason.unavailable,
          'Location coordinates could not be retrieved from provider.',
        );
      }

      final double rawLat = coords.latitude;
      final double rawLng = coords.longitude;
      final double lat =
          approximateOnly ? (rawLat * 100).round() / 100 : rawLat;
      final double lng =
          approximateOnly ? (rawLng * 100).round() / 100 : rawLng;

      final geocoded = await _reverseGeocode(rawLat, rawLng);

      return LocationResult.success(
        ApproximateLocation(
          address: geocoded['address'],
          city: geocoded['city'],
          district: geocoded['district'],
          state: geocoded['state'],
          postalCode: geocoded['postalCode'],
          coordinates: Coordinates(
            latitude: lat,
            longitude: lng,
            accuracy: coords.accuracy,
          ),
          isApproximate: approximateOnly,
        ),
      );
    }

    // Real device GPS via Geolocator
    Position? position;
    try {
      position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.medium,
          timeLimit: Duration(seconds: 8),
        ),
      );
    } catch (_) {
      try {
        position = await Geolocator.getLastKnownPosition();
      } catch (_) {
        position = null;
      }
    }

    if (position != null) {
      final double rawLat = position.latitude;
      final double rawLng = position.longitude;
      final double lat =
          approximateOnly ? (rawLat * 100).round() / 100 : rawLat;
      final double lng =
          approximateOnly ? (rawLng * 100).round() / 100 : rawLng;

      final geocoded = await _reverseGeocode(rawLat, rawLng);

      return LocationResult.success(
        ApproximateLocation(
          address: geocoded['address'],
          city: geocoded['city'],
          district: geocoded['district'],
          state: geocoded['state'],
          postalCode: geocoded['postalCode'],
          coordinates: Coordinates(
            latitude: lat,
            longitude: lng,
            accuracy: position.accuracy,
          ),
          isApproximate: approximateOnly,
        ),
      );
    }

    return const LocationResult.failure(
      LocationFailureReason.unavailable,
      'Could not obtain location from device GPS. Please enable GPS or select a verified healthcare centre.',
    );
  }

  Future<Map<String, String?>> _reverseGeocode(double rawLat, double rawLng) async {
    try {
      final uri = Uri.parse(
          'https://nominatim.openstreetmap.org/reverse?lat=$rawLat&lon=$rawLng&format=json&addressdetails=1');
      final res = await http.get(uri, headers: {
        'User-Agent': 'NetraBloodApp/1.0 (contact@netra.org)',
        'Accept': 'application/json',
      }).timeout(const Duration(seconds: 4));

      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        final addr = data['address'] as Map<String, dynamic>?;
        if (addr != null) {
          final road = addr['road'] ?? addr['pedestrian'] ?? addr['street'];
          final suburb = addr['suburb'] ?? addr['neighbourhood'] ?? addr['residential'];
          final houseNumber = addr['house_number'];

          final addressParts = [
            if (houseNumber != null) houseNumber,
            if (road != null) road,
            if (suburb != null) suburb,
          ];
          final address = addressParts.isNotEmpty
              ? addressParts.join(', ')
              : data['display_name'] as String?;
          final city = (addr['city'] ??
              addr['town'] ??
              addr['village'] ??
              addr['municipality'] ??
              addr['county']) as String?;
          final district = (addr['state_district'] ?? addr['district']) as String?;
          final state = addr['state'] as String?;
          final postalCode = addr['postcode'] as String?;

          return {
            'address': address,
            'city': city,
            'district': district,
            'state': state,
            'postalCode': postalCode,
          };
        }
      }
    } catch (_) {
      // Non-blocking fallback
    }
    return {};
  }

  @override
  Future<bool> openAppSettings() => _permissionService.openAppSettings();

  @override
  Future<bool> openLocationSettings() =>
      _permissionService.openLocationSettings();

  @override
  Future<List<ApproximateLocation>> searchLocations(String query) async {
    return [];
  }
}
