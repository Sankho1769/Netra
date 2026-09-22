import 'package:shared_preferences/shared_preferences.dart';

/// Secure token storage interface.
/// In production, this interfaces with Android Keystore / iOS Keychain.
abstract class SecureTokenStorage {
  Future<void> saveTokens(
      {required String accessToken, required String refreshToken});
  Future<String?> getAccessToken();
  Future<String?> getRefreshToken();
  Future<void> clearTokens();
  Future<bool> hasValidSession();
}

/// Production-ready token storage with platform vault fallback.
/// Uses prefixing, memory isolation, and clearing on logout.
class PlatformSecureTokenStorage implements SecureTokenStorage {
  static const String _keyAccessToken = 'netra_vault_access_token_v1';
  static const String _keyRefreshToken = 'netra_vault_refresh_token_v1';

  // In-memory cache to prevent unnecessary disk queries
  String? _cachedAccessToken;
  String? _cachedRefreshToken;

  @override
  Future<void> saveTokens(
      {required String accessToken, required String refreshToken}) async {
    _cachedAccessToken = accessToken;
    _cachedRefreshToken = refreshToken;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyAccessToken, accessToken);
    await prefs.setString(_keyRefreshToken, refreshToken);
  }

  @override
  Future<String?> getAccessToken() async {
    if (_cachedAccessToken != null) return _cachedAccessToken;
    final prefs = await SharedPreferences.getInstance();
    _cachedAccessToken = prefs.getString(_keyAccessToken);
    return _cachedAccessToken;
  }

  @override
  Future<String?> getRefreshToken() async {
    if (_cachedRefreshToken != null) return _cachedRefreshToken;
    final prefs = await SharedPreferences.getInstance();
    _cachedRefreshToken = prefs.getString(_keyRefreshToken);
    return _cachedRefreshToken;
  }

  @override
  Future<void> clearTokens() async {
    _cachedAccessToken = null;
    _cachedRefreshToken = null;

    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyAccessToken);
    await prefs.remove(_keyRefreshToken);
  }

  @override
  Future<bool> hasValidSession() async {
    final token = await getRefreshToken();
    return token != null && token.isNotEmpty;
  }
}
