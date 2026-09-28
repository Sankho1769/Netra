import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../models/verified_hospital_model.dart';

class HospitalApiService {
  final ApiClient _client;

  HospitalApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(baseUrl: baseUrl ?? ApiConfig.endpoint('/hospitals'));

  Future<List<VerifiedHospitalModel>> searchHospitals({
    required String query,
    String? city,
  }) async {
    if (query.trim().isEmpty) return [];
    try {
      final queryParams = <String, dynamic>{'query': query.trim()};
      if (city != null && city.trim().isNotEmpty) {
        queryParams['city'] = city.trim();
      }

      final response =
          await _client.get('/search', queryParameters: queryParams);
      if (response is List<dynamic>) {
        return response
            .map((item) =>
                VerifiedHospitalModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      return [];
    }
  }

  Future<VerifiedHospitalModel?> verifyHospital({
    required String hospitalName,
    String? hospitalAddress,
    String? city,
    String? state,
    String? placeId,
  }) async {
    try {
      final body = <String, dynamic>{
        'hospitalName': hospitalName.trim(),
        if (hospitalAddress != null) 'hospitalAddress': hospitalAddress.trim(),
        if (city != null) 'city': city.trim(),
        if (state != null) 'state': state.trim(),
        if (placeId != null) 'placeId': placeId.trim(),
      };
      final response = await _client.post('/verify', body: body);
      if (response is Map<String, dynamic>) {
        return VerifiedHospitalModel.fromJson(response);
      }
      return null;
    } catch (e) {
      if (e is NetworkException) rethrow;
      return null;
    }
  }
}
