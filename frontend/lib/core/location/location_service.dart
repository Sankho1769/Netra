import 'dart:async';
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

      return LocationResult.success(
        ApproximateLocation(
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
    try {
      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 10),
        ),
      );

      final double rawLat = position.latitude;
      final double rawLng = position.longitude;
      final double lat =
          approximateOnly ? (rawLat * 100).round() / 100 : rawLat;
      final double lng =
          approximateOnly ? (rawLng * 100).round() / 100 : rawLng;

      return LocationResult.success(
        ApproximateLocation(
          coordinates: Coordinates(
            latitude: lat,
            longitude: lng,
            accuracy: position.accuracy,
          ),
          isApproximate: approximateOnly,
        ),
      );
    } on TimeoutException {
      return const LocationResult.failure(
        LocationFailureReason.timeout,
        'GPS location request timed out. Please try again or select a verified hospital.',
      );
    } catch (_) {
      return const LocationResult.failure(
        LocationFailureReason.unavailable,
        'Could not obtain location from device GPS. Please select a verified hospital.',
      );
    }
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
