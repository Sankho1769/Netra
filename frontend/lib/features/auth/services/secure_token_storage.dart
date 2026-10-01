import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/auth_models.dart';

/// Custom exception thrown when secure storage operations fail.
/// Sanitized to never expose sensitive token values in error messages.
class SecureStorageException implements Exception {
  final String message;
  final dynamic cause;

  const SecureStorageException(this.message, [this.cause]);

  @override
  String toString() => 'SecureStorageException: $message';
}

/// Secure token storage interface.
/// In production, this interfaces with Android Keystore / iOS Keychain.
abstract class SecureTokenStorage {
  Future<void> saveTokens(
      {required String accessToken, required String refreshToken});
  Future<String?> getAccessToken();
  Future<String?> getRefreshToken();
  Future<void> clearTokens();
  Future<bool> hasValidSession();
  Future<void> saveUser(User user);
  Future<User?> getUser();
}

/// Production-ready token storage backed by hardware-backed Keystore / Keychain.
/// Zero plaintext persistence; strictly fails safely without insecure fallbacks.
class PlatformSecureTokenStorage implements SecureTokenStorage {
  static const String _keyAccessToken = 'netra_vault_access_token_v1';
  static const String _keyRefreshToken = 'netra_vault_refresh_token_v1';
  static const String _keyUser = 'netra_vault_user_profile_v1';

  final FlutterSecureStorage _storage;

  // In-memory cache to prevent unnecessary disk queries
  String? _cachedAccessToken;
  String? _cachedRefreshToken;
  User? _cachedUser;

  PlatformSecureTokenStorage({FlutterSecureStorage? storage})
      : _storage = storage ??
            const FlutterSecureStorage(
              aOptions: AndroidOptions(
                resetOnError: false,
                keyCipherAlgorithm:
                    KeyCipherAlgorithm.RSA_ECB_OAEPwithSHA_256andMGF1Padding,
                storageCipherAlgorithm:
                    StorageCipherAlgorithm.AES_GCM_NoPadding,
              ),
              iOptions: IOSOptions(
                accessibility: KeychainAccessibility.first_unlock_this_device,
              ),
            );

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    try {
      await _storage.write(key: _keyAccessToken, value: accessToken);
      await _storage.write(key: _keyRefreshToken, value: refreshToken);
      _cachedAccessToken = accessToken;
      _cachedRefreshToken = refreshToken;
    } catch (e) {
      _cachedAccessToken = null;
      _cachedRefreshToken = null;
      throw SecureStorageException(
          'Failed to securely persist authentication tokens: ${e.runtimeType}',
          e);
    }
  }

  @override
  Future<String?> getAccessToken() async {
    if (_cachedAccessToken != null) return _cachedAccessToken;
    try {
      _cachedAccessToken = await _storage.read(key: _keyAccessToken);
      return _cachedAccessToken;
    } catch (_) {
      _cachedAccessToken = null;
      return null;
    }
  }

  @override
  Future<String?> getRefreshToken() async {
    if (_cachedRefreshToken != null) return _cachedRefreshToken;
    try {
      _cachedRefreshToken = await _storage.read(key: _keyRefreshToken);
      return _cachedRefreshToken;
    } catch (_) {
      _cachedRefreshToken = null;
      return null;
    }
  }

  @override
  Future<void> saveUser(User user) async {
    try {
      final jsonStr = jsonEncode(user.toJson());
      await _storage.write(key: _keyUser, value: jsonStr);
      _cachedUser = user;
    } catch (_) {
      // Best-effort user cache
    }
  }

  @override
  Future<User?> getUser() async {
    if (_cachedUser != null) return _cachedUser;
    try {
      final jsonStr = await _storage.read(key: _keyUser);
      if (jsonStr != null && jsonStr.isNotEmpty) {
        final decoded = jsonDecode(jsonStr) as Map<String, dynamic>;
        _cachedUser = User.fromJson(decoded);
        return _cachedUser;
      }
    } catch (_) {
      _cachedUser = null;
    }
    return null;
  }

  @override
  Future<void> clearTokens() async {
    _cachedAccessToken = null;
    _cachedRefreshToken = null;
    _cachedUser = null;

    try {
      await _storage.delete(key: _keyAccessToken);
      await _storage.delete(key: _keyRefreshToken);
      await _storage.delete(key: _keyUser);
    } catch (_) {
      // Memory cache is already invalidated; swallow platform delete errors safely
    }
  }

  @override
  Future<bool> hasValidSession() async {
    final token = await getRefreshToken();
    return token != null && token.isNotEmpty;
  }
}

/// In-memory token storage for unit tests and isolated mocks.
class InMemorySecureTokenStorage implements SecureTokenStorage {
  String? _accessToken;
  String? _refreshToken;
  User? _user;

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    _accessToken = accessToken;
    _refreshToken = refreshToken;
  }

  @override
  Future<String?> getAccessToken() async => _accessToken;

  @override
  Future<String?> getRefreshToken() async => _refreshToken;

  @override
  Future<void> saveUser(User user) async {
    _user = user;
  }

  @override
  Future<User?> getUser() async => _user;

  @override
  Future<void> clearTokens() async {
    _accessToken = null;
    _refreshToken = null;
    _user = null;
  }

  @override
  Future<bool> hasValidSession() async =>
      _refreshToken != null && _refreshToken!.isNotEmpty;
}
