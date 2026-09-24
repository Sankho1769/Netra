import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../../auth/services/secure_token_storage.dart';
import '../models/donor_match_response_model.dart';

/// Service managing API calls for donor response workflows and requester persistent match tracking.
class DonorResponseApiService {
  final ApiClient _client;
  final SecureTokenStorage _tokenStorage;

  DonorResponseApiService({
    ApiClient? client,
    SecureTokenStorage? tokenStorage,
    String? baseUrl,
  })  : _client = client ?? ApiClient(baseUrl: baseUrl ?? ApiConfig.baseUrl),
        _tokenStorage = tokenStorage ?? PlatformSecureTokenStorage();

  Future<Map<String, String>> _authHeaders() async {
    final token = await _tokenStorage.getAccessToken();
    final headers = <String, String>{'Content-Type': 'application/json'};
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  String _sanitizeErrorMessage(dynamic e) {
    if (e is NetworkException) {
      return e.message;
    }
    final str = e
        .toString()
        .replaceFirst('Exception: ', '')
        .replaceFirst('ValidationException: ', '');
    if (str.toLowerCase().contains('sql') ||
        str.toLowerCase().contains('database') ||
        str.toLowerCase().contains('hibernate') ||
        str.toLowerCase().contains('org.netra') ||
        str.toLowerCase().contains('internal')) {
      return 'A system error occurred. Please try again later.';
    }
    return str.isEmpty
        ? 'An unexpected error occurred. Please try again.'
        : str;
  }

  /// Fetches persistent matches assigned to the authenticated donor.
  Future<List<DonorMatchDetail>> getMyMatches() async {
    try {
      final headers = await _authHeaders();
      final response = await _client.get('/donor/matches', headers: headers);

      if (response is List<dynamic>) {
        return response
            .map((e) => DonorMatchDetail.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Fetches details for a specific match assigned to the authenticated donor.
  Future<DonorMatchDetail> getMatchDetail(String matchId) async {
    try {
      final headers = await _authHeaders();
      final response =
          await _client.get('/donor/matches/$matchId', headers: headers);

      if (response is Map<String, dynamic>) {
        return DonorMatchDetail.fromJson(response);
      }
      throw const ValidationException(
          'Unexpected response format from server.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Accepts a match assignment on behalf of the authenticated donor.
  Future<DonorMatchDetail> acceptMatch(String matchId) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/donor/matches/$matchId/accept',
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return DonorMatchDetail.fromJson(response);
      }
      throw const ValidationException(
          'Unexpected response format upon accepting match.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Declines a match assignment on behalf of the authenticated donor.
  Future<DonorMatchDetail> declineMatch(String matchId) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/donor/matches/$matchId/decline',
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return DonorMatchDetail.fromJson(response);
      }
      throw const ValidationException(
          'Unexpected response format upon declining match.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Creates a persistent donor match record for the intended candidate.
  /// Authorized for request owner or administrator.
  Future<RequesterDonorMatch> createMatch(
      String requestId, String candidateReference) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/blood-requests/$requestId/matches',
        headers: headers,
        body: {'candidateReference': candidateReference},
      );

      if (response is Map<String, dynamic>) {
        return RequesterDonorMatch.fromJson(response);
      }
      throw const ValidationException(
          'Unexpected response format upon creating match.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Retrieves persistent match records for a blood request.
  /// Authorized for request owner or administrator.
  Future<List<RequesterDonorMatch>> getMatchResponses(String requestId) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.get(
        '/blood-requests/$requestId/match-responses',
        headers: headers,
      );

      if (response is List<dynamic>) {
        return response
            .map((e) => RequesterDonorMatch.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }
}
