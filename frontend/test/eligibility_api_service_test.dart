import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:netra_app/core/network/api_client.dart';
import 'package:netra_app/core/network/network_exception.dart';
import 'package:netra_app/features/eligibility/models/eligibility_models.dart';
import 'package:netra_app/features/eligibility/services/eligibility_api_service.dart';

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

void main() {
  group('EligibilityApiService Network & Error Mapping Tests', () {
    test('fetchRuleVersion successfully parses active version on HTTP 200',
        () async {
      final mockClient = MockHttpClient((req) async {
        expect(req.url.path, '/eligibility/rules/version');
        return http.Response(
            jsonEncode({'activeVersion': 'INDIA-NBTC-2026-02'}), 200);
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient,
      );

      final version = await service.fetchRuleVersion();
      expect(version, equals('INDIA-NBTC-2026-02'));
    });

    test(
        'createSessionDetailed returns session info with capability token on HTTP 201',
        () async {
      final mockClient = MockHttpClient((req) async {
        expect(req.url.path, '/eligibility/sessions');
        return http.Response(
          jsonEncode({
            'sessionId': 'session-xyz-123',
            'ruleVersion': 'INDIA-NBTC-2026-01',
            'capabilityToken': 'cap-token-abc',
            'expiresAt': '2026-09-24T18:00:00Z',
          }),
          201,
        );
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient,
      );

      final session = await service.createSessionDetailed();
      expect(session.sessionId, equals('session-xyz-123'));
      expect(session.capabilityToken, equals('cap-token-abc'));
      expect(session.ruleVersion, equals('INDIA-NBTC-2026-01'));
      expect(service.currentCapabilityToken, equals('cap-token-abc'));
    });

    test(
        'submitAnswers includes X-Capability-Token header and submits answers list',
        () async {
      String? capturedHeader;
      String? capturedBody;

      final mockClient = MockHttpClient((req) async {
        expect(req.url.path, '/eligibility/sessions/session-1/answers');
        capturedHeader = req.headers['X-Capability-Token'];
        if (req is http.Request) {
          capturedBody = req.body;
        }
        return http.Response(jsonEncode({'status': 'SUCCESS'}), 200);
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient,
      );
      service.currentCapabilityToken = 'cap-token-existing';

      await service
          .submitAnswers('session-1', {'AGE': '28', 'WEIGHT_KG': '70'});

      expect(capturedHeader, equals('cap-token-existing'));
      expect(capturedBody, contains('AGE'));
      expect(capturedBody, contains('28'));
    });

    test('checkEligibility correctly parses EligibilityResult on HTTP 200',
        () async {
      final mockClient = MockHttpClient((req) async {
        expect(req.url.path, '/eligibility/sessions/session-1/check');
        return http.Response(
          jsonEncode({
            'sessionId': 'session-1',
            'ruleVersion': 'INDIA-NBTC-2026-01',
            'result': 'LIKELY_ELIGIBLE',
            'title': 'Eligible to Donate',
            'message': 'Donor meets NBTC criteria.',
            'disclaimer': 'Clinical verification required at donation site.',
          }),
          200,
        );
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient,
      );

      final result = await service.checkEligibility('session-1');
      expect(result.sessionId, equals('session-1'));
      expect(result.result, equals(ResultType.LIKELY_ELIGIBLE));
      expect(result.title, equals('Eligible to Donate'));
      expect(result.message, contains('NBTC criteria'));
    });

    test('Maps HTTP 401 and 403 to UnauthorizedException', () async {
      final mockClient401 = MockHttpClient((req) async {
        return http.Response(
            jsonEncode({'message': 'Invalid capability token'}), 401);
      });

      final service401 = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient401,
      );

      expect(
        () => service401.fetchRuleVersion(),
        throwsA(isA<UnauthorizedException>()),
      );

      final mockClient403 = MockHttpClient((req) async {
        return http.Response(jsonEncode({'message': 'Access forbidden'}), 403);
      });

      final service403 = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient403,
      );

      expect(
        () => service403.checkEligibility('session-1'),
        throwsA(isA<UnauthorizedException>()),
      );
    });

    test('Maps HTTP 409 to ConflictException', () async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(
            jsonEncode({'message': 'Session already completed'}), 409);
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient,
      );

      expect(
        () => service.submitAnswers('session-1', {'AGE': '25'}),
        throwsA(isA<ConflictException>()),
      );
    });

    test('Maps HTTP 429 to RateLimitException', () async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(
            jsonEncode({'message': 'Too many screening requests'}), 429);
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient,
      );

      expect(
        () => service.createSession(),
        throwsA(isA<RateLimitException>()),
      );
    });

    test('Maps HTTP 500+ to ServerException', () async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(
            jsonEncode({'message': 'Internal screening engine error'}), 500);
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: mockClient,
      );

      expect(
        () => service.getSessionResult('session-1'),
        throwsA(isA<ServerException>()),
      );
    });

    test('Maps timeout to ConnectionException', () async {
      final timeoutClient = ApiClient(
        baseUrl: 'http://localhost:8080/eligibility',
        timeout: const Duration(milliseconds: 50),
        client: MockHttpClient((req) async {
          await Future.delayed(const Duration(milliseconds: 150));
          return http.Response('{"ok":true}', 200);
        }),
      );

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        apiClient: timeoutClient,
      );

      expect(
        () => service.fetchRuleVersion(),
        throwsA(isA<ConnectionException>()),
      );
    });

    test('Maps SocketException network failure to ConnectionException',
        () async {
      final networkFailClient = MockHttpClient((req) async {
        throw const SocketException('Connection refused');
      });

      final service = EligibilityApiService(
        baseUrl: 'http://localhost:8080/eligibility',
        client: networkFailClient,
      );

      expect(
        () => service.fetchRuleVersion(),
        throwsA(isA<ConnectionException>()),
      );
    });
  });
}
