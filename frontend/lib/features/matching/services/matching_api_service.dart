import '../../../core/network/api_client.dart';
import '../../../core/network/network_exception.dart';
import '../../auth/services/secure_token_storage.dart';
import '../models/donor_match_model.dart';

/// Secure API service for fetching matched donor candidates for blood requests.
class MatchingApiService {
  final ApiClient _client;
  final SecureTokenStorage _tokenStorage;

  MatchingApiService({
    ApiClient? client,
    SecureTokenStorage? tokenStorage,
    String? baseUrl,
  })  : _client = client ??
            ApiClient(
                baseUrl:
                    baseUrl ?? 'http://localhost:8080/api/v1/blood-requests'),
        _tokenStorage = tokenStorage ?? PlatformSecureTokenStorage();

  /// Fetches ranked donor candidates matching the specified blood request.
  ///
  /// Requires authentication as request owner or administrator.
  Future<DonorMatchResponse> fetchMatches(
    String requestId, {
    double? radiusKm,
    int? limit,
  }) async {
    try {
      final token = await _tokenStorage.getAccessToken();
      final headers = <String, String>{};
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      }

      final queryParams = <String, dynamic>{};
      if (radiusKm != null) {
        queryParams['radiusKm'] = radiusKm;
      }
      if (limit != null) {
        queryParams['limit'] = limit;
      }

      final response = await _client.get(
        '/$requestId/matches',
        headers: headers,
        queryParameters: queryParams.isNotEmpty ? queryParams : null,
      );

      if (response is Map<String, dynamic>) {
        return DonorMatchResponse.fromJson(response);
      }
      throw const ValidationException(
          'Unexpected response format from matching engine.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(e.toString());
    }
  }
}
