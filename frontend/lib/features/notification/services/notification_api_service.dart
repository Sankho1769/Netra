import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../../auth/services/secure_token_storage.dart';
import '../models/notification_model.dart';

class NotificationApiService {
  final ApiClient _client;
  final SecureTokenStorage _tokenStorage;

  NotificationApiService({
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

  /// Fetches paginated notifications for the authenticated user.
  Future<List<AppNotification>> getNotifications({
    int page = 0,
    int size = 20,
    bool unreadOnly = false,
  }) async {
    try {
      final headers = await _authHeaders();
      final queryParams = 'page=$page&size=$size&unreadOnly=$unreadOnly';
      final response =
          await _client.get('/notifications?$queryParams', headers: headers);

      if (response is Map<String, dynamic>) {
        final content = response['content'] as List<dynamic>?;
        if (content != null) {
          return content
              .map((e) => AppNotification.fromJson(e as Map<String, dynamic>))
              .toList();
        }
      } else if (response is List<dynamic>) {
        return response
            .map((e) => AppNotification.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Fetches unread notification count for the authenticated user.
  Future<int> getUnreadCount() async {
    try {
      final headers = await _authHeaders();
      final response =
          await _client.get('/notifications/unread-count', headers: headers);
      if (response is Map<String, dynamic>) {
        return (response['unreadCount'] as num?)?.toInt() ?? 0;
      }
      return 0;
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Marks a specific notification as read.
  Future<AppNotification> markAsRead(String notificationId) async {
    try {
      final headers = await _authHeaders();
      final response = await _client
          .patch('/notifications/$notificationId/read', headers: headers);
      if (response is Map<String, dynamic>) {
        return AppNotification.fromJson(response);
      }
      throw const FormatException(
          'Invalid server response when marking notification as read');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Marks all notifications for the authenticated user as read.
  Future<int> markAllAsRead() async {
    try {
      final headers = await _authHeaders();
      final response =
          await _client.patch('/notifications/read-all', headers: headers);
      if (response is Map<String, dynamic>) {
        return (response['updatedCount'] as num?)?.toInt() ?? 0;
      }
      return 0;
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Registers or refreshes a device push token for the authenticated user.
  Future<DeviceToken> registerDeviceToken({
    required String token,
    String platform = 'ANDROID',
    String provider = 'FCM',
  }) async {
    try {
      final headers = await _authHeaders();
      final body = DeviceTokenRegistration(
        token: token,
        platform: platform,
        provider: provider,
      ).toJson();

      final response =
          await _client.post('/devices/tokens', headers: headers, body: body);
      if (response is Map<String, dynamic>) {
        return DeviceToken.fromJson(response);
      }
      throw const FormatException(
          'Invalid server response when registering device token');
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Revokes a device token for the authenticated user by token ID.
  Future<void> revokeDeviceToken(String tokenId) async {
    try {
      final headers = await _authHeaders();
      await _client.delete('/devices/tokens/$tokenId', headers: headers);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }

  /// Revokes a device token by raw token string for the authenticated user.
  Future<void> revokeDeviceTokenByToken(String token) async {
    try {
      final headers = await _authHeaders();
      await _client.post(
        '/devices/tokens/revoke',
        headers: headers,
        body: {'token': token},
      );
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(_sanitizeErrorMessage(e));
    }
  }
}
