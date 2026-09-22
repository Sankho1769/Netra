import 'package:flutter/material.dart';
import '../models/auth_models.dart';
import '../services/auth_api_service.dart';
import '../services/secure_token_storage.dart';

class AuthController extends ChangeNotifier {
  final AuthApiService _apiService;
  final SecureTokenStorage _tokenStorage;

  AuthStatus _status = AuthStatus.unknown;
  User? _currentUser;
  String? _errorMessage;

  AuthController({
    AuthApiService? apiService,
    SecureTokenStorage? tokenStorage,
  })  : _apiService = apiService ?? AuthApiService(),
        _tokenStorage = tokenStorage ?? PlatformSecureTokenStorage();

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

    try {
      final bundle = await _apiService
          .login(LoginRequest(email: email, password: password));
      await _tokenStorage.saveTokens(
        accessToken: bundle.tokens.accessToken,
        refreshToken: bundle.tokens.refreshToken,
      );

      _currentUser = bundle.user;
      _status = AuthStatus.authenticated;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _status = AuthStatus.unauthenticated;
      notifyListeners();
      return false;
    }
  }

  Future<bool> register({
    required String fullName,
    required String email,
    String? phone,
    required String password,
  }) async {
    _status = AuthStatus.authenticating;
    _errorMessage = null;
    notifyListeners();

    try {
      final bundle = await _apiService.register(
        RegisterRequest(
            fullName: fullName, email: email, phone: phone, password: password),
      );
      await _tokenStorage.saveTokens(
        accessToken: bundle.tokens.accessToken,
        refreshToken: bundle.tokens.refreshToken,
      );

      _currentUser = bundle.user;
      _status = AuthStatus.authenticated;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _status = AuthStatus.unauthenticated;
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    final refreshToken = await _tokenStorage.getRefreshToken();
    final accessToken = await _tokenStorage.getAccessToken();

    // Call server to invalidate refresh session
    await _apiService.logout(
        refreshToken: refreshToken, accessToken: accessToken);

    // Clear local storage
    await _tokenStorage.clearTokens();
    _currentUser = null;
    _status = AuthStatus.unauthenticated;
    _errorMessage = null;
    notifyListeners();
  }

  Future<void> logoutAll() async {
    final accessToken = await _tokenStorage.getAccessToken();
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
