import 'location_models.dart';
import 'permission_service.dart';

/// Privacy-first location service for NETRA.
/// Coordinates are coarse by default to prevent precise donor tracking.
abstract class LocationService {
  Future<ApproximateLocation?> getCurrentLocation(
      {bool approximateOnly = true});
  Future<List<ApproximateLocation>> searchLocations(String query);
}

/// Production implementation with privacy fuzzing and graceful fallbacks.
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
    final enabled = await _permissionService.isLocationServiceEnabled();
    if (!enabled) {
      return null;
    }

    final permission = await _permissionService.checkLocationPermission();
    if (permission != LocationPermissionStatus.granted) {
      final requested = await _permissionService.requestLocationPermission();
      if (requested != LocationPermissionStatus.granted) {
        return null;
      }
    }

    if (_deviceCoordinateProvider != null) {
      final coords = await _deviceCoordinateProvider!();
      if (coords == null) return null;

      final double rawLat = coords.latitude;
      final double rawLng = coords.longitude;
      final double lat =
          approximateOnly ? (rawLat * 100).round() / 100 : rawLat;
      final double lng =
          approximateOnly ? (rawLng * 100).round() / 100 : rawLng;

      return ApproximateLocation(
        coordinates: Coordinates(
          latitude: lat,
          longitude: lng,
          accuracy: coords.accuracy,
        ),
        isApproximate: approximateOnly,
      );
    }

    // When native device GPS hardware/bridge is not present, return null
    // rather than faking coordinates to prevent incorrect emergency or blood request proximity matching.
    return null;
  }

  @override
  Future<List<ApproximateLocation>> searchLocations(String query) async {
    if (query.trim().isEmpty) return [];

    // Localized sample locations matching query
    final sampleLocations = [
      const ApproximateLocation(
        city: "Mumbai",
        district: "Parel",
        state: "Maharashtra",
        postalCode: "400012",
        coordinates: Coordinates(latitude: 19.00, longitude: 72.83),
      ),
      const ApproximateLocation(
        city: "Mumbai",
        district: "Bandra",
        state: "Maharashtra",
        postalCode: "400050",
        coordinates: Coordinates(latitude: 19.05, longitude: 72.84),
      ),
      const ApproximateLocation(
        city: "Pune",
        district: "Shivajinagar",
        state: "Maharashtra",
        postalCode: "411005",
        coordinates: Coordinates(latitude: 18.53, longitude: 73.85),
      ),
    ];

    final q = query.toLowerCase();
    return sampleLocations
        .where((loc) =>
            (loc.city?.toLowerCase().contains(q) ?? false) ||
            (loc.district?.toLowerCase().contains(q) ?? false) ||
            (loc.postalCode?.contains(q) ?? false))
        .toList();
  }
}
