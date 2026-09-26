import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/network_exception.dart';
import '../../notification/services/push_notification_service.dart';
import '../models/auth_models.dart';
import '../services/auth_api_service.dart';
import '../services/secure_token_storage.dart';

class AuthController extends ChangeNotifier {
  final AuthApiService _apiService;
  final SecureTokenStorage _tokenStorage;
  final PushNotificationService? _pushNotificationService;

  AuthStatus _status = AuthStatus.unknown;
  User? _currentUser;
  String? _errorMessage;

  AuthController({
    AuthApiService? apiService,
    SecureTokenStorage? tokenStorage,
    PushNotificationService? pushNotificationService,
    bool configureGlobalAuth = true,
  })  : _apiService = apiService ?? AuthApiService(),
        _tokenStorage = tokenStorage ?? PlatformSecureTokenStorage(),
        _pushNotificationService = pushNotificationService {
    if (configureGlobalAuth) {
      ApiClient.configureAuth(
        tokenProvider: () => _tokenStorage.getAccessToken(),
        onTokenRefresh: () async {
          final currentRefresh = await _tokenStorage.getRefreshToken();
          if (currentRefresh == null || currentRefresh.isEmpty) {
            await _tokenStorage.clearTokens();
            return false;
          }
          try {
            final bundle = await _apiService.refresh(currentRefresh);
            await _tokenStorage.saveTokens(
              accessToken: bundle.tokens.accessToken,
              refreshToken: bundle.tokens.refreshToken,
            );
            return true;
          } catch (_) {
            await _tokenStorage.clearTokens();
            return false;
          }
        },
      );
    }
  }

  AuthStatus get status => _status;
  User? get currentUser => _currentUser;
  String? get errorMessage => _errorMessage;
  bool get isAuthenticated =>
      _status == AuthStatus.authenticated && _currentUser != null;

  /// Restores existing session on app launch. Never hangs infinitely.
  Future<void> initialize() async {
    _status = AuthStatus.authenticating;
    _errorMessage = null;
    notifyListeners();

    try {
      final refreshToken = await _tokenStorage.getRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) {
        _status = AuthStatus.unauthenticated;
        notifyListeners();
        return;
      }

      // Refresh to verify session and retrieve latest active user state
      final bundle = await _apiService.refresh(refreshToken);
      await _tokenStorage.saveTokens(
        accessToken: bundle.tokens.accessToken,
        refreshToken: bundle.tokens.refreshToken,
      );

      _currentUser = bundle.user;
      _status = AuthStatus.authenticated;
      notifyListeners();
    } catch (e) {
      // Clear invalid credentials and fail gracefully to unauthenticated state
      await _tokenStorage.clearTokens();
      _currentUser = null;
      _status = AuthStatus.unauthenticated;
      notifyListeners();
    }
  }

  Future<bool> login(String email, String password) async {
    _status = AuthStatus.authenticating;
    _errorMessage = null;
    notifyListeners();

    final normalizedEmail = email.trim().toLowerCase();

    try {
      final bundle = await _apiService
          .login(LoginRequest(email: normalizedEmail, password: password));
      await _tokenStorage.saveTokens(
        accessToken: bundle.tokens.accessToken,
        refreshToken: bundle.tokens.refreshToken,
      );

      _currentUser = bundle.user;
      _status = AuthStatus.authenticated;
      _errorMessage = null;
      notifyListeners();
      return true;
    } catch (e) {
      if (e is NetworkException) {
        _errorMessage = e.message;
      } else {
        _errorMessage = 'Invalid email or password';
      }
      _status = AuthStatus.unauthenticated;
      notifyListeners();
      return false;
    }
  }

  Future<bool> register({
    required String fullName,
    required String email,
    required String phone,
    required String password,
  }) async {
    _status = AuthStatus.authenticating;
    _errorMessage = null;
    notifyListeners();

    final normalizedEmail = email.trim().toLowerCase();
    final normalizedPhone = phone.trim();

    try {
      final bundle = await _apiService.register(
        RegisterRequest(
          fullName: fullName.trim(),
          email: normalizedEmail,
          phone: normalizedPhone,
          password: password,
        ),
      );
      await _tokenStorage.saveTokens(
        accessToken: bundle.tokens.accessToken,
        refreshToken: bundle.tokens.refreshToken,
      );

      _currentUser = bundle.user;
      _status = AuthStatus.authenticated;
      _errorMessage = null;
      notifyListeners();
      return true;
    } catch (e) {
      if (e is NetworkException) {
        _errorMessage = e.message;
      } else {
        _errorMessage = 'Unable to complete registration. Please try again.';
      }
      _status = AuthStatus.unauthenticated;
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    try {
      final refreshToken = await _tokenStorage.getRefreshToken();
      final accessToken = await _tokenStorage.getAccessToken();

      // Revoke device push token if registered
      await _pushNotificationService?.revokeCurrentDeviceToken();

      // Call server to invalidate refresh session
      await _apiService.logout(
          refreshToken: refreshToken, accessToken: accessToken);
    } catch (_) {
      // Best-effort server notification; always proceed with local clearance
    } finally {
      // Clear local storage deterministically
      await _tokenStorage.clearTokens();
      _currentUser = null;
      _status = AuthStatus.unauthenticated;
      _errorMessage = null;
      notifyListeners();
    }
  }

  Future<void> logoutAll() async {
    final accessToken = await _tokenStorage.getAccessToken();
    await _pushNotificationService?.revokeCurrentDeviceToken();
    if (accessToken != null) {
      await _apiService.logoutAll(accessToken);
    }
    await _tokenStorage.clearTokens();
    _currentUser = null;
    _status = AuthStatus.unauthenticated;
    _errorMessage = null;
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}
