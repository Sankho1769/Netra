import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../models/blood_bank.dart';
import '../models/blood_inventory.dart';

class BloodBankApiService {
  final ApiClient _client;

  BloodBankApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(baseUrl: baseUrl ?? ApiConfig.endpoint('/bloodbanks'));

  Future<List<BloodBankSummary>> discoverBloodBanks({
    String? city,
    String? bloodGroup,
    String? operatingStatus,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
      };
      if (city != null && city.trim().isNotEmpty) {
        queryParams['city'] = city.trim();
      }
      if (bloodGroup != null && bloodGroup.trim().isNotEmpty) {
        queryParams['bloodGroup'] = bloodGroup.trim();
      }
      if (operatingStatus != null && operatingStatus.trim().isNotEmpty) {
        queryParams['operatingStatus'] = operatingStatus.trim();
      }

      final response = await _client.get('', queryParameters: queryParams);

      if (response is Map<String, dynamic> && response.containsKey('content')) {
        final list = response['content'] as List<dynamic>;
        return list
            .map((e) => BloodBankSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      } else if (response is List<dynamic>) {
        return response
            .map((e) => BloodBankSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to discover blood centres. Please try again.');
    }
  }

  Future<List<BloodBankSummary>> getNearbyBloodBanks({
    required double latitude,
    required double longitude,
    double radiusKm = 10.0,
    String? bloodGroup,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'latitude': latitude,
        'longitude': longitude,
        'radiusKm': radiusKm,
      };
      if (bloodGroup != null && bloodGroup.trim().isNotEmpty) {
        queryParams['bloodGroup'] = bloodGroup.trim();
      }

      final response =
          await _client.get('/nearby', queryParameters: queryParams);

      if (response is List<dynamic>) {
        return response
            .map((e) => BloodBankSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load nearby blood centres. Please try again.');
    }
  }

  Future<BloodBankDetail> getBloodBankDetails(
    String id, {
    double? userLat,
    double? userLon,
  }) async {
    try {
      final queryParams = <String, dynamic>{};
      if (userLat != null && userLon != null) {
        queryParams['userLat'] = userLat;
        queryParams['userLon'] = userLon;
      }

      final response = await _client.get('/$id',
          queryParameters: queryParams.isNotEmpty ? queryParams : null);
      return BloodBankDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load blood centre details. Please try again.');
    }
  }

  Future<List<BloodInventoryItem>> getBloodBankInventory(String id) async {
    try {
      final response = await _client.get('/$id/inventory');
      if (response is List<dynamic>) {
        return response
            .map((e) => BloodInventoryItem.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load blood inventory. Please try again.');
    }
  }
}
