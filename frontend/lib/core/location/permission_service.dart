import 'location_models.dart';

/// Clean abstraction for checking and requesting location permissions.
abstract class PermissionService {
  Future<LocationPermissionStatus> checkLocationPermission();
  Future<LocationPermissionStatus> requestLocationPermission();
  Future<bool> isLocationServiceEnabled();
}

/// In-memory / Default implementation for cross-platform support.
class DefaultPermissionService implements PermissionService {
  LocationPermissionStatus _status = LocationPermissionStatus.unknown;

  @override
  Future<LocationPermissionStatus> checkLocationPermission() async {
    return _status;
  }

  @override
  Future<LocationPermissionStatus> requestLocationPermission() async {
    // In a real device setup with geolocator/permission_handler plugin,
    // this queries the platform channel. Here we manage safe defaults.
    _status = LocationPermissionStatus.granted;
    return _status;
  }

  @override
  Future<bool> isLocationServiceEnabled() async {
    return true;
  }
}
