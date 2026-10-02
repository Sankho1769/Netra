import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../models/auth_models.dart';

class AuthResponseBundle {
  final User user;
  final AuthTokens? tokens;

  const AuthResponseBundle({required this.user, this.tokens});
}

class AuthApiService {
  final ApiClient _client;

  AuthApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(baseUrl: baseUrl ?? ApiConfig.endpoint('/auth'));

  Future<AuthResponseBundle> register(RegisterRequest request) async {
    try {
      final response = await _client.post(
        '/register',
        body: request.toJson(),
        requiresAuth: false,
      );
      final data = response as Map<String, dynamic>;
      final user = User.fromJson(data['user'] as Map<String, dynamic>);
      AuthTokens? tokens;
      if (data['accessToken'] != null &&
          (data['accessToken'] as String).isNotEmpty) {
        tokens = AuthTokens.fromJson(data);
      }
      return AuthResponseBundle(user: user, tokens: tokens);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(
          'Unable to complete registration. Please try again.');
    }
  }

  Future<AuthResponseBundle> verifyEmail({
    required String email,
    required String code,
  }) async {
    try {
      final response = await _client.post(
        '/verify-email',
        body: {'email': email.trim().toLowerCase(), 'code': code.trim()},
        requiresAuth: false,
      );
      final data = response as Map<String, dynamic>;
      final user = User.fromJson(data['user'] as Map<String, dynamic>);
      final tokens = AuthTokens.fromJson(data);
      return AuthResponseBundle(user: user, tokens: tokens);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException('Invalid or expired verification code.');
    }
  }

  Future<void> resendVerification({required String email}) async {
    try {
      await _client.post(
        '/resend-verification',
        body: {'email': email.trim().toLowerCase()},
        requiresAuth: false,
      );
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(
          'Could not resend verification code. Please wait.');
    }
  }

  Future<AuthResponseBundle> login(LoginRequest request) async {
    try {
      final response = await _client.post(
        '/login',
        body: request.toJson(),
        requiresAuth: false,
      );
      final data = response as Map<String, dynamic>;
      final user = User.fromJson(data['user'] as Map<String, dynamic>);
      final tokens = AuthTokens.fromJson(data);
      return AuthResponseBundle(user: user, tokens: tokens);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw ValidationException(
          'Authentication failed. Please check your credentials.');
    }
  }

  Future<AuthResponseBundle> refresh(String refreshToken) async {
    try {
      final response = await _client.post(
        '/refresh',
        body: {'refreshToken': refreshToken},
        requiresAuth: false,
      );
      final data = response as Map<String, dynamic>;
      final user = User.fromJson(data['user'] as Map<String, dynamic>);
      final tokens = AuthTokens.fromJson(data);
      return AuthResponseBundle(user: user, tokens: tokens);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const UnauthorizedException('Refresh session expired or invalid.');
    }
  }

  Future<void> logout({String? refreshToken, String? accessToken}) async {
    try {
      final headers = <String, String>{};
      if (accessToken != null && accessToken.isNotEmpty) {
        headers['Authorization'] = 'Bearer $accessToken';
      }
      await _client.post(
        '/logout',
        headers: headers,
        body: refreshToken != null ? {'refreshToken': refreshToken} : null,
      );
    } catch (_) {
      // Best-effort logout: clear local state even if network call fails
    }
  }

  Future<void> logoutAll(String accessToken) async {
    try {
      await _client.post(
        '/logout-all',
        headers: {'Authorization': 'Bearer $accessToken'},
      );
    } catch (_) {
      // Best-effort logout-all
    }
  }

  Future<User> getMe(String accessToken) async {
    final response = await _client.get(
      '/me',
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    return User.fromJson(response as Map<String, dynamic>);
  }
}
