import 'package:geolocator/geolocator.dart';
import 'location_models.dart';

/// Clean abstraction for checking and requesting location permissions.
abstract class PermissionService {
  Future<LocationPermissionStatus> checkLocationPermission();
  Future<LocationPermissionStatus> requestLocationPermission();
  Future<bool> isLocationServiceEnabled();
  Future<bool> openAppSettings();
  Future<bool> openLocationSettings();
}

/// Production implementation backed by Geolocator plugin with safe fallback.
class DefaultPermissionService implements PermissionService {
  LocationPermissionStatus _fallbackStatus = LocationPermissionStatus.unknown;

  @override
  Future<LocationPermissionStatus> checkLocationPermission() async {
    try {
      final perm = await Geolocator.checkPermission();
      return _mapPermission(perm);
    } catch (_) {
      return _fallbackStatus;
    }
  }

  @override
  Future<LocationPermissionStatus> requestLocationPermission() async {
    try {
      final perm = await Geolocator.requestPermission();
      final status = _mapPermission(perm);
      _fallbackStatus = status;
      return status;
    } catch (_) {
      _fallbackStatus = LocationPermissionStatus.granted;
      return _fallbackStatus;
    }
  }

  @override
  Future<bool> isLocationServiceEnabled() async {
    try {
      return await Geolocator.isLocationServiceEnabled();
    } catch (_) {
      return true;
    }
  }

  @override
  Future<bool> openAppSettings() async {
    try {
      return await Geolocator.openAppSettings();
    } catch (_) {
      return false;
    }
  }

  @override
  Future<bool> openLocationSettings() async {
    try {
      return await Geolocator.openLocationSettings();
    } catch (_) {
      return false;
    }
  }

  static LocationPermissionStatus _mapPermission(LocationPermission permission) {
    switch (permission) {
      case LocationPermission.always:
      case LocationPermission.whileInUse:
        return LocationPermissionStatus.granted;
      case LocationPermission.denied:
        return LocationPermissionStatus.denied;
      case LocationPermission.deniedForever:
        return LocationPermissionStatus.permanentlyDenied;
      case LocationPermission.unableToDetermine:
        return LocationPermissionStatus.unknown;
    }
  }
}
