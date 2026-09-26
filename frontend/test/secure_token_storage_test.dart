import 'dart:io';
import 'package:flutter_secure_storage/test/test_flutter_secure_storage_platform.dart';
import 'package:flutter_secure_storage_platform_interface/flutter_secure_storage_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/auth/models/auth_models.dart';
import 'package:netra_app/features/auth/services/auth_api_service.dart';
import 'package:netra_app/features/auth/services/secure_token_storage.dart';
import 'package:netra_app/features/auth/state/auth_controller.dart';

class FakeAuthApiService extends AuthApiService {
  AuthResponseBundle? refreshResponse;
  bool refreshShouldFail = false;
  bool logoutCalled = false;
  bool logoutAllCalled = false;
  String? lastLoggedOutRefresh;
  String? lastLoggedOutAccess;

  @override
  Future<AuthResponseBundle> refresh(String refreshToken) async {
    if (refreshShouldFail) {
      throw Exception('Refresh token expired');
    }
    return refreshResponse ??
        AuthResponseBundle(
          user: const User(
            id: 'user-123',
            fullName: 'Jolly Banerjee',
            email: 'jolly.banerjee@netra.org',
            roles: ['ROLE_DONOR'],
            status: 'ACTIVE',
          ),
          tokens: const AuthTokens(
            accessToken: 'new-access-token-456',
            refreshToken: 'new-refresh-token-789',
            expiresIn: 900,
          ),
        );
  }

  @override
  Future<void> logout({String? refreshToken, String? accessToken}) async {
    logoutCalled = true;
    lastLoggedOutRefresh = refreshToken;
    lastLoggedOutAccess = accessToken;
  }

  @override
  Future<void> logoutAll(String accessToken) async {
    logoutAllCalled = true;
  }
}

class FaultySecureStoragePlatform extends FlutterSecureStoragePlatform {
  bool failWrites = false;
  bool failReads = false;
  final Map<String, String> data = {};

  @override
  Future<bool> containsKey(
          {required String key, required Map<String, String> options}) async =>
      data.containsKey(key);

  @override
  Future<void> delete(
          {required String key, required Map<String, String> options}) async =>
      data.remove(key);

  @override
  Future<void> deleteAll({required Map<String, String> options}) async =>
      data.clear();

  @override
  Future<String?> read(
      {required String key, required Map<String, String> options}) async {
    if (failReads) throw Exception('Simulated KeyStore read error');
    return data[key];
  }

  @override
  Future<Map<String, String>> readAll(
          {required Map<String, String> options}) async =>
      data;

  @override
  Future<void> write({
    required String key,
    required String value,
    required Map<String, String> options,
  }) async {
    if (failWrites) throw Exception('Simulated KeyStore write error');
    data[key] = value;
  }
}

void main() {
  group('SecureTokenStorage & Mobile Token Security Tests', () {
    late Map<String, String> mockSecureData;
    late PlatformSecureTokenStorage tokenStorage;

    setUp(() {
      mockSecureData = {};
      FlutterSecureStoragePlatform.instance =
          TestFlutterSecureStoragePlatform(mockSecureData);
      tokenStorage = PlatformSecureTokenStorage();
    });

    test('Saves tokens into secure storage vault without plaintext persistence',
        () async {
      await tokenStorage.saveTokens(
        accessToken: 'access-sec-001',
        refreshToken: 'refresh-sec-001',
      );

      final accessToken = await tokenStorage.getAccessToken();
      final refreshToken = await tokenStorage.getRefreshToken();

      expect(accessToken, 'access-sec-001');
      expect(refreshToken, 'refresh-sec-001');
      expect(mockSecureData['netra_vault_access_token_v1'], 'access-sec-001');
      expect(mockSecureData['netra_vault_refresh_token_v1'], 'refresh-sec-001');

      final hasSession = await tokenStorage.hasValidSession();
      expect(hasSession, isTrue);
    });

    test('Clears tokens completely from memory and secure storage', () async {
      await tokenStorage.saveTokens(
        accessToken: 'access-sec-002',
        refreshToken: 'refresh-sec-002',
      );

      await tokenStorage.clearTokens();

      final accessToken = await tokenStorage.getAccessToken();
      final refreshToken = await tokenStorage.getRefreshToken();

      expect(accessToken, isNull);
      expect(refreshToken, isNull);
      expect(
          mockSecureData.containsKey('netra_vault_access_token_v1'), isFalse);
      expect(
          mockSecureData.containsKey('netra_vault_refresh_token_v1'), isFalse);

      final hasSession = await tokenStorage.hasValidSession();
      expect(hasSession, isFalse);
    });

    test(
        'Fails safely on secure storage write error without plaintext fallback',
        () async {
      final faultyPlatform = FaultySecureStoragePlatform();
      faultyPlatform.failWrites = true;
      FlutterSecureStoragePlatform.instance = faultyPlatform;

      final secureStorage = PlatformSecureTokenStorage();

      await expectLater(
        secureStorage.saveTokens(
          accessToken: 'unpersistable-acc',
          refreshToken: 'unpersistable-ref',
        ),
        throwsA(isA<SecureStorageException>()),
      );

      // Memory cache is cleared on failure to prevent using unsaved tokens
      faultyPlatform.failWrites = false;
      final retrievedAccess = await secureStorage.getAccessToken();
      expect(retrievedAccess, isNull);
    });

    test('Fails safely on secure storage read error and returns null',
        () async {
      final faultyPlatform = FaultySecureStoragePlatform();
      faultyPlatform.data['netra_vault_access_token_v1'] = 'corrupted-acc';
      faultyPlatform.failReads = true;
      FlutterSecureStoragePlatform.instance = faultyPlatform;

      final secureStorage = PlatformSecureTokenStorage();
      final token = await secureStorage.getAccessToken();
      expect(token, isNull);
    });
  });

  group('AuthController Secure Storage Flow & Session Restoration', () {
    late Map<String, String> mockSecureData;
    late PlatformSecureTokenStorage tokenStorage;
    late FakeAuthApiService fakeApiService;
    late AuthController authController;

    setUp(() {
      mockSecureData = {};
      FlutterSecureStoragePlatform.instance =
          TestFlutterSecureStoragePlatform(mockSecureData);
      tokenStorage = PlatformSecureTokenStorage();
      fakeApiService = FakeAuthApiService();
      authController = AuthController(
        apiService: fakeApiService,
        tokenStorage: tokenStorage,
      );
    });

    test('Missing credentials on app launch transitions to unauthenticated',
        () async {
      expect(authController.status, AuthStatus.unknown);

      await authController.initialize();

      expect(authController.status, AuthStatus.unauthenticated);
      expect(authController.currentUser, isNull);
      expect(authController.isAuthenticated, isFalse);
    });

    test(
        'App launch with stored refresh token restores session and updates secure storage',
        () async {
      await tokenStorage.saveTokens(
        accessToken: 'initial-acc',
        refreshToken: 'initial-ref',
      );

      await authController.initialize();

      expect(authController.status, AuthStatus.authenticated);
      expect(authController.currentUser, isNotNull);
      expect(authController.currentUser!.email, 'jolly.banerjee@netra.org');
      expect(authController.isAuthenticated, isTrue);

      // Verify refreshed tokens were persisted in secure storage
      final storedAccess = await tokenStorage.getAccessToken();
      final storedRefresh = await tokenStorage.getRefreshToken();
      expect(storedAccess, 'new-access-token-456');
      expect(storedRefresh, 'new-refresh-token-789');
    });

    test('Expired refresh token during app launch clears secure storage',
        () async {
      await tokenStorage.saveTokens(
        accessToken: 'stale-acc',
        refreshToken: 'stale-ref',
      );
      fakeApiService.refreshShouldFail = true;

      await authController.initialize();

      expect(authController.status, AuthStatus.unauthenticated);
      expect(authController.currentUser, isNull);

      final storedRefresh = await tokenStorage.getRefreshToken();
      expect(storedRefresh, isNull);
    });

    test('Logout clears stored credentials from secure storage', () async {
      await tokenStorage.saveTokens(
        accessToken: 'valid-acc',
        refreshToken: 'valid-ref',
      );

      await authController.logout();

      expect(authController.status, AuthStatus.unauthenticated);
      expect(authController.currentUser, isNull);
      expect(fakeApiService.logoutCalled, isTrue);
      expect(fakeApiService.lastLoggedOutRefresh, 'valid-ref');

      final storedAccess = await tokenStorage.getAccessToken();
      final storedRefresh = await tokenStorage.getRefreshToken();
      expect(storedAccess, isNull);
      expect(storedRefresh, isNull);
    });

    test('LogoutAll clears stored credentials from secure storage', () async {
      await tokenStorage.saveTokens(
        accessToken: 'valid-acc-all',
        refreshToken: 'valid-ref-all',
      );

      await authController.logoutAll();

      expect(authController.status, AuthStatus.unauthenticated);
      expect(fakeApiService.logoutAllCalled, isTrue);

      final storedAccess = await tokenStorage.getAccessToken();
      final storedRefresh = await tokenStorage.getRefreshToken();
      expect(storedAccess, isNull);
      expect(storedRefresh, isNull);
    });
  });

  group('Zero Plaintext Token Storage Hygiene Test', () {
    test(
        'Verifies SharedPreferences is not imported or used for token persistence in lib/',
        () {
      final libDir = Directory('lib');
      final files = libDir.listSync(recursive: true).whereType<File>();

      for (final file in files) {
        if (!file.path.endsWith('.dart')) continue;
        final content = file.readAsStringSync();
        expect(
          content.contains('shared_preferences'),
          isFalse,
          reason:
              'Found forbidden shared_preferences dependency in ${file.path}',
        );
        expect(
          content.contains('SharedPreferences'),
          isFalse,
          reason: 'Found forbidden SharedPreferences usage in ${file.path}',
        );
      }
    });
  });
}
