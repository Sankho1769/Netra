import '../../../core/network/api_client.dart';
import '../../../core/network/network_exception.dart';
import '../../auth/services/secure_token_storage.dart';
import '../models/fulfillment_model.dart';

class FulfillmentApiService {
  final ApiClient _client;
  final SecureTokenStorage _tokenStorage;

  FulfillmentApiService({
    ApiClient? client,
    SecureTokenStorage? tokenStorage,
    String? baseUrl,
  })  : _client = client ??
            ApiClient(baseUrl: baseUrl ?? 'http://localhost:8080/api/v1'),
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

  /// Creates a new operational fulfillment claim.
  Future<FulfillmentModel> createFulfillment(CreateFulfillmentDto dto) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/fulfillments',
        body: dto.toJson(),
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return FulfillmentModel.fromJson(response);
      }
      throw Exception('Failed to parse fulfillment response.');
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }

  /// Starts fulfillment (READY -> IN_PROGRESS).
  Future<FulfillmentModel> startFulfillment(String id) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/fulfillments/$id/start',
        body: {},
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return FulfillmentModel.fromJson(response);
      }
      throw Exception('Failed to parse start fulfillment response.');
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }

  /// Completes fulfillment (IN_PROGRESS -> FULFILLED).
  Future<FulfillmentModel> completeFulfillment(String id) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/fulfillments/$id/complete',
        body: {},
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return FulfillmentModel.fromJson(response);
      }
      throw Exception('Failed to parse complete fulfillment response.');
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }

  /// Fails fulfillment (IN_PROGRESS -> FAILED).
  Future<FulfillmentModel> failFulfillment(
      String id, FailFulfillmentDto dto) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/fulfillments/$id/fail',
        body: dto.toJson(),
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return FulfillmentModel.fromJson(response);
      }
      throw Exception('Failed to parse fail fulfillment response.');
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }

  /// Cancels fulfillment in READY status.
  Future<FulfillmentModel> cancelFulfillment(
      String id, CancelFulfillmentDto dto) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.post(
        '/fulfillments/$id/cancel',
        body: dto.toJson(),
        headers: headers,
      );

      if (response is Map<String, dynamic>) {
        return FulfillmentModel.fromJson(response);
      }
      throw Exception('Failed to parse cancel fulfillment response.');
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }

  /// Fetches paginated fulfillments for the current user (requester, donor, staff).
  Future<List<FulfillmentModel>> getMyFulfillments({
    int page = 0,
    int size = 20,
  }) async {
    try {
      final headers = await _authHeaders();
      final queryParams = 'page=$page&size=$size';
      final response =
          await _client.get('/fulfillments/my?$queryParams', headers: headers);

      if (response is Map<String, dynamic>) {
        final content = response['content'] as List<dynamic>?;
        if (content != null) {
          return content
              .map((e) => FulfillmentModel.fromJson(e as Map<String, dynamic>))
              .toList();
        }
      }
      return [];
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }

  /// Fetches pending/in-progress queue for blood bank staff / admins.
  Future<List<FulfillmentModel>> getPendingFulfillments({
    int page = 0,
    int size = 20,
  }) async {
    try {
      final headers = await _authHeaders();
      final queryParams = 'page=$page&size=$size';
      final response = await _client.get('/fulfillments/pending?$queryParams',
          headers: headers);

      if (response is Map<String, dynamic>) {
        final content = response['content'] as List<dynamic>?;
        if (content != null) {
          return content
              .map((e) => FulfillmentModel.fromJson(e as Map<String, dynamic>))
              .toList();
        }
      }
      return [];
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }

  /// Fetches fulfillment details by ID.
  Future<FulfillmentModel> getFulfillmentById(String id) async {
    try {
      final headers = await _authHeaders();
      final response = await _client.get('/fulfillments/$id', headers: headers);

      if (response is Map<String, dynamic>) {
        return FulfillmentModel.fromJson(response);
      }
      throw Exception('Failed to parse fulfillment detail response.');
    } catch (e) {
      throw Exception(_sanitizeErrorMessage(e));
    }
  }
}
