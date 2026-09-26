import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:netra_app/core/location/location_models.dart';
import 'package:netra_app/core/location/location_service.dart';
import 'package:netra_app/core/location/permission_service.dart';
import 'package:netra_app/core/network/api_client.dart';
import 'package:netra_app/core/network/network_exception.dart';
import 'package:netra_app/features/auth/services/secure_token_storage.dart';
import 'package:netra_app/features/blood_request/screens/blood_request_list_screen.dart';
import 'package:netra_app/features/blood_request/state/blood_request_controller.dart';
import 'package:netra_app/features/blood_request/services/blood_request_api_service.dart';
import 'package:netra_app/features/notification/services/notification_api_service.dart';
import 'package:netra_app/features/notification/services/push_notification_service.dart';

class MockHttpClient extends http.BaseClient {
  final Future<http.Response> Function(http.BaseRequest request) handler;
  MockHttpClient(this.handler);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final response = await handler(request);
    return http.StreamedResponse(
      Stream.value(response.bodyBytes),
      response.statusCode,
      headers: response.headers,
      request: request,
    );
  }
}

class FixedPermissionService implements PermissionService {
  final LocationPermissionStatus status;
  final bool enabled;

  FixedPermissionService({
    this.status = LocationPermissionStatus.granted,
    this.enabled = true,
  });

  @override
  Future<LocationPermissionStatus> checkLocationPermission() async => status;

  @override
  Future<LocationPermissionStatus> requestLocationPermission() async => status;

  @override
  Future<bool> isLocationServiceEnabled() async => enabled;
}

class InMemoryTokenStorage implements SecureTokenStorage {
  String? accessToken;
  String? refreshToken;

  InMemoryTokenStorage({this.accessToken, this.refreshToken});

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @override
  Future<String?> getAccessToken() async => accessToken;

  @override
  Future<String?> getRefreshToken() async => refreshToken;

  @override
  Future<void> clearTokens() async {
    accessToken = null;
    refreshToken = null;
  }

  @override
  Future<bool> hasValidSession() async =>
      accessToken != null && accessToken!.isNotEmpty;
}

void main() {
  group('Location Service Privacy and Fallback Tests', () {
    test('Returns null when device GPS provider is not attached', () async {
      final service = DefaultLocationService(
        permissionService:
            FixedPermissionService(status: LocationPermissionStatus.granted),
      );

      final location = await service.getCurrentLocation();
      expect(location, isNull);
    });

    test('Returns null when location permission is denied', () async {
      final service = DefaultLocationService(
        permissionService:
            FixedPermissionService(status: LocationPermissionStatus.denied),
        deviceCoordinateProvider: () async =>
            const Coordinates(latitude: 28.6139, longitude: 77.2090),
      );

      final location = await service.getCurrentLocation();
      expect(location, isNull);
    });

    test('Returns null when location service is disabled', () async {
      final service = DefaultLocationService(
        permissionService: FixedPermissionService(
          status: LocationPermissionStatus.granted,
          enabled: false,
        ),
        deviceCoordinateProvider: () async =>
            const Coordinates(latitude: 28.6139, longitude: 77.2090),
      );

      final location = await service.getCurrentLocation();
      expect(location, isNull);
    });

    test(
        'Returns coarse approximate coordinates when provider supplies location',
        () async {
      final service = DefaultLocationService(
        permissionService:
            FixedPermissionService(status: LocationPermissionStatus.granted),
        deviceCoordinateProvider: () async =>
            const Coordinates(latitude: 28.6139, longitude: 77.2090),
      );

      final location = await service.getCurrentLocation(approximateOnly: true);
      expect(location, isNotNull);
      expect(location!.latitude, equals(28.61));
      expect(location.longitude, equals(77.21));
      expect(location.isApproximate, isTrue);
    });
  });

  group('ApiClient 401 Refresh & 403 Forbidden Tests', () {
    test('401 triggers token refresh and retries with new token', () async {
      int requestCount = 0;
      bool refreshCalled = false;
      String currentToken = 'initial-token';

      final mockClient = MockHttpClient((req) async {
        requestCount++;
        final authHeader = req.headers['Authorization'];
        if (authHeader == 'Bearer initial-token') {
          return http.Response(jsonEncode({'message': 'Token expired'}), 401);
        }
        if (authHeader == 'Bearer new-token') {
          return http.Response(jsonEncode({'status': 'ok'}), 200);
        }
        return http.Response(jsonEncode({'message': 'Unauthorized'}), 401);
      });

      final apiClient = ApiClient(
        baseUrl: 'http://api.netra.local',
        client: mockClient,
        tokenProvider: () async => currentToken,
        onTokenRefresh: () async {
          refreshCalled = true;
          currentToken = 'new-token';
          return true;
        },
      );

      final result = await apiClient.get('/secure-resource');
      expect(result, isA<Map>());
      expect(result['status'], equals('ok'));
      expect(refreshCalled, isTrue);
      expect(requestCount, equals(2));
    });

    test(
        '403 Forbidden does NOT trigger token refresh and throws ForbiddenException',
        () async {
      int requestCount = 0;
      bool refreshCalled = false;

      final mockClient = MockHttpClient((req) async {
        requestCount++;
        return http.Response(jsonEncode({'message': 'Access forbidden'}), 403);
      });

      final apiClient = ApiClient(
        baseUrl: 'http://api.netra.local',
        client: mockClient,
        tokenProvider: () async => 'valid-token',
        onTokenRefresh: () async {
          refreshCalled = true;
          return true;
        },
      );

      await expectLater(
        () => apiClient.get('/admin-only'),
        throwsA(isA<ForbiddenException>()),
      );
      expect(refreshCalled, isFalse);
      expect(requestCount, equals(1));
    });

    test('401 throws UnauthorizedException when refresh fails', () async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(jsonEncode({'message': 'Token expired'}), 401);
      });

      final apiClient = ApiClient(
        baseUrl: 'http://api.netra.local',
        client: mockClient,
        onTokenRefresh: () async => false,
      );

      await expectLater(
        () => apiClient.get('/secure-resource'),
        throwsA(isA<UnauthorizedException>()),
      );
    });
  });

  group('FCM Device Token Revocation Tests', () {
    test('NotificationApiService.revokeDeviceTokenByToken posts raw token',
        () async {
      String? postedBody;
      String? postedPath;

      final mockClient = MockHttpClient((req) async {
        postedPath = req.url.path;
        postedBody = (req as http.Request).body;
        return http.Response('', 204);
      });

      final storage = InMemoryTokenStorage(accessToken: 'user-token');
      final notificationApi = NotificationApiService(
        client:
            ApiClient(baseUrl: 'http://api.netra.local', client: mockClient),
        tokenStorage: storage,
      );

      await notificationApi.revokeDeviceTokenByToken('fcm-device-token-xyz');

      expect(postedPath, equals('/devices/tokens/revoke'));
      expect(
          jsonDecode(postedBody!), equals({'token': 'fcm-device-token-xyz'}));
    });

    test('PushNotificationService registers and revokes active token',
        () async {
      String? revokedToken;

      final mockClient = MockHttpClient((req) async {
        if (req.url.path == '/devices/tokens/revoke') {
          final data = jsonDecode((req as http.Request).body);
          revokedToken = data['token'];
          return http.Response('', 204);
        }
        if (req.url.path == '/devices/tokens') {
          return http.Response(
            jsonEncode({
              'id': 'token-id-1',
              'token': 'fcm-device-token-123',
              'platform': 'ANDROID',
              'provider': 'FCM',
              'isActive': true,
              'createdAt': '2026-09-25T12:00:00Z',
            }),
            201,
          );
        }
        return http.Response('Not Found', 404);
      });

      final storage = InMemoryTokenStorage(accessToken: 'user-token');
      final notificationApi = NotificationApiService(
        client:
            ApiClient(baseUrl: 'http://api.netra.local', client: mockClient),
        tokenStorage: storage,
      );
      final pushService = PushNotificationService(apiService: notificationApi);

      await pushService.registerDeviceToken('fcm-device-token-123');
      expect(pushService.currentDeviceToken, equals('fcm-device-token-123'));

      await pushService.revokeCurrentDeviceToken();
      expect(revokedToken, equals('fcm-device-token-123'));
      expect(pushService.currentDeviceToken, isNull);
    });
  });

  group('BloodRequestListScreen Location Handling Widget Tests', () {
    testWidgets(
        'Renders location unavailable UI when location is unavailable on nearby tab',
        (tester) async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(jsonEncode([]), 200);
      });

      final apiService = BloodRequestApiService(
        client:
            ApiClient(baseUrl: 'http://api.netra.local', client: mockClient),
      );
      final controller = BloodRequestController(apiService: apiService);

      final locationService = DefaultLocationService(
        permissionService:
            FixedPermissionService(status: LocationPermissionStatus.denied),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: BloodRequestListScreen(
            controller: controller,
            locationService: locationService,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Switch to Tab 2 (Nearby Camps / Hospitals)
      await tester.tap(find.text('Nearby Camps / Hospitals'));
      await tester.pumpAndSettle();

      expect(find.text('Device location unavailable or permission denied.'),
          findsOneWidget);
      expect(find.text('Retry Location Access'), findsOneWidget);
    });
  });
}
