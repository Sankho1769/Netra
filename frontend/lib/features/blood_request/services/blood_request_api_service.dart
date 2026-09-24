import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../models/blood_request.dart';

class BloodRequestApiService {
  final ApiClient _client;

  BloodRequestApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(
                baseUrl: baseUrl ?? ApiConfig.endpoint('/blood-requests'));

  Future<List<BloodRequestSummary>> discoverRequests({
    String? bloodGroup,
    String? city,
    String? urgency,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
      };
      if (bloodGroup != null && bloodGroup.trim().isNotEmpty) {
        queryParams['bloodGroup'] = bloodGroup.trim();
      }
      if (city != null && city.trim().isNotEmpty) {
        queryParams['city'] = city.trim();
      }
      if (urgency != null && urgency.trim().isNotEmpty) {
        queryParams['urgency'] = urgency.trim();
      }

      final response = await _client.get('', queryParameters: queryParams);

      if (response is Map<String, dynamic> && response.containsKey('content')) {
        final list = response['content'] as List<dynamic>;
        return list
            .map((e) => BloodRequestSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      } else if (response is List<dynamic>) {
        return response
            .map((e) => BloodRequestSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to discover blood requests. Please try again.');
    }
  }

  Future<List<BloodRequestSummary>> getMyRequests({
    int page = 0,
    int size = 20,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
      };

      final response = await _client.get('/me', queryParameters: queryParams);

      if (response is Map<String, dynamic> && response.containsKey('content')) {
        final list = response['content'] as List<dynamic>;
        return list
            .map((e) => BloodRequestSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      } else if (response is List<dynamic>) {
        return response
            .map((e) => BloodRequestSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to load your blood requests.');
    }
  }

  Future<List<BloodRequestSummary>> findNearbyRequests({
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
            .map((e) => BloodRequestSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to find nearby blood requests.');
    }
  }

  Future<BloodRequestDetail> getRequestDetails(String id) async {
    try {
      // NOTE: Accepts ONLY ID. Coordinates are never sent to this endpoint.
      final response = await _client.get('/$id');
      return BloodRequestDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to load blood request details.');
    }
  }

  Future<BloodRequestDetail> createRequest(Map<String, dynamic> payload) async {
    try {
      final response = await _client.post('', body: payload);
      return BloodRequestDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to create blood request.');
    }
  }

  Future<BloodRequestDetail> updateRequest(
      String id, Map<String, dynamic> payload) async {
    try {
      final response = await _client.patch('/$id', body: payload);
      return BloodRequestDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to update blood request.');
    }
  }

  Future<BloodRequestDetail> cancelRequest(String id, {String? reason}) async {
    try {
      final body = <String, dynamic>{};
      if (reason != null && reason.trim().isNotEmpty) {
        body['reason'] = reason.trim();
      }
      final response = await _client.post('/$id/cancel', body: body);
      return BloodRequestDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to cancel blood request.');
    }
  }
}
