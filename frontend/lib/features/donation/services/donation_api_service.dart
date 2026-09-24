import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../../auth/services/secure_token_storage.dart';
import '../models/donation_model.dart';

class DonationApiService {
  final ApiClient _client;
  final SecureTokenStorage _tokenStorage;

  DonationApiService({
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

  /// Fetches paginated donation history for the authenticated donor.
  Future<List<DonationModel>> getMyDonations({
    int page = 0,
    int size = 20,
  }) async {
    try {
      final headers = await _authHeaders();
      final queryParams = 'page=$page&size=$size';
      final response =
          await _client.get('/donations/my?$queryParams', headers: headers);

      if (response is Map<String, dynamic>) {
        final content = response['content'] as List<dynamic>?;
        if (content != null) {
          return content
              .map((item) =>
                  DonationModel.fromJson(item as Map<String, dynamic>))
              .toList();
        }
      } else if (response is List<dynamic>) {
        return response
            .map((item) => DonationModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Fetches single donation detail by ID.
  Future<DonationModel> getDonationDetail(String donationId) async {
    try {
      final headers = await _authHeaders();
      final response =
          await _client.get('/donations/$donationId', headers: headers);

      if (response is Map<String, dynamic>) {
        return DonationModel.fromJson(response);
      }
      throw const ValidationException('Invalid response format from server.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Submits a donation claim for verification.
  Future<DonationModel> submitClaim(CreateDonationClaimDto dto) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/donations',
        headers: headers,
        body: dto.toJson(),
      );

      if (response is Map<String, dynamic>) {
        return DonationModel.fromJson(response);
      }
      throw const ValidationException('Invalid response format from server.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Cancels a pending donation claim by the donor.
  Future<DonationModel> cancelClaim(String donationId) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/donations/$donationId/cancel',
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return DonationModel.fromJson(response);
      }
      throw const ValidationException('Invalid response format from server.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Fetches pending donations queue for authorized staff.
  Future<List<DonationModel>> getPendingDonations({
    int page = 0,
    int size = 20,
  }) async {
    try {
      final headers = await _authHeaders();
      final queryParams = 'page=$page&size=$size';
      final response = await _client.get(
        '/donations/pending?$queryParams',
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        final content = response['content'] as List<dynamic>?;
        if (content != null) {
          return content
              .map((item) =>
                  DonationModel.fromJson(item as Map<String, dynamic>))
              .toList();
        }
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Staff verifies a pending donation claim.
  Future<DonationModel> verifyDonation(String donationId,
      {String? notes}) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/donations/$donationId/verify',
        headers: headers,
        body: {'notes': notes},
      );

      if (response is Map<String, dynamic>) {
        return DonationModel.fromJson(response);
      }
      throw const ValidationException('Invalid response format from server.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Staff rejects a pending donation claim with feedback.
  Future<DonationModel> rejectDonation(
      String donationId, String rejectionReason) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/donations/$donationId/reject',
        headers: headers,
        body: {'rejectionReason': rejectionReason},
      );

      if (response is Map<String, dynamic>) {
        return DonationModel.fromJson(response);
      }
      throw const ValidationException('Invalid response format from server.');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }
}
